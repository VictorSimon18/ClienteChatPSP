package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

/**
 * Punto de entrada del cliente. Arquitectura híbrida HTTPS + TCP/TLS:
 *
 * <ul>
 *   <li><b>HTTPS</b>: login, registro, envío de mensajes, desconexión.</li>
 *   <li><b>TCP/TLS</b>: canal persistente cifrado para recibir mensajes en tiempo real
 *       (push del servidor).</li>
 * </ul>
 *
 * <p>El cliente valida el certificado del servidor mediante el truststore almacenado
 * en {@code certs/truststore.jks}. Genera los certificados con {@code gen_certs.sh}
 * en el servidor y copia el truststore a la carpeta {@code certs/} de este proyecto.
 *
 * <p>Flujo tras un login exitoso:
 * <ol>
 *   <li>Se recibe {@code OK|puertoTCP|mensaje} del servidor HTTPS.</li>
 *   <li>Se abre un {@link SSLSocket} TCP/TLS al puerto indicado.</li>
 *   <li>Se envía el nombre de usuario por TCP para identificarse.</li>
 *   <li>{@link ReceptorMensajes} queda bloqueado leyendo mensajes push cifrados del servidor.</li>
 * </ol>
 */
public class ClienteChat {

    private static final String HOST_DEFAULT    = "localhost";
    private static final int    PUERTO_DEFAULT  = 12345;

    /** Ruta al truststore JKS que contiene el certificado público del servidor. */
    private static final String TRUSTSTORE_PATH = "certs/truststore.jks";

    private String     baseUrl;
    private String     host;
    /** Contexto SSL/TLS compartido por HTTPS y TCP para verificar el certificado del servidor. */
    private SSLContext sslContext;
    private ReceptorMensajes receptor;
    private InterfazGrafica  gui;
    private volatile String  nombreUsuario;
    private volatile Socket  socketTCP;

    public static void main(String[] args) {
        String host   = HOST_DEFAULT;
        int    puerto = PUERTO_DEFAULT;

        if (args.length >= 1) {
            host = args[0];
        } else {
            String input = (String) JOptionPane.showInputDialog(
                null, "Dirección IP del servidor:", "Conectar al servidor",
                JOptionPane.PLAIN_MESSAGE, null, null, HOST_DEFAULT);
            if (input == null) System.exit(0);
            if (!input.trim().isEmpty()) host = input.trim();
        }

        if (args.length >= 2) {
            try { puerto = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { System.out.println("Puerto inválido. Usando " + PUERTO_DEFAULT); }
        }

        final String finalHost   = host;
        final int    finalPuerto = puerto;

        SwingUtilities.invokeLater(() -> {
            ClienteChat cliente = new ClienteChat();
            cliente.iniciar(finalHost, finalPuerto);
        });
    }

    private void iniciar(String host, int puerto) {
        this.host    = host;
        this.baseUrl = "https://" + host + ":" + puerto;
        try {
            this.sslContext = crearSSLContext();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    "No se encontró el truststore en '" + TRUSTSTORE_PATH + "'.\n"
                    + "Ejecuta gen_certs.sh en el servidor y copia el truststore a certs/.",
                    "Error SSL", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al cargar el certificado SSL: " + e.getMessage(),
                    "Error SSL", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        gui = new InterfazGrafica(this);
    }

    // ── SSL/TLS ───────────────────────────────────────────────────────────────

    /**
     * Crea el {@link SSLContext} del cliente a partir del truststore JKS.
     *
     * <p>El truststore contiene el certificado público del servidor, generado por
     * {@code gen_certs.sh}. Esto permite al cliente verificar la identidad del servidor
     * sin necesidad de aceptar certificados arbitrarios ni deshabilitar la validación TLS.
     *
     * <p>La contraseña puede sobreescribirse con la propiedad de sistema
     * {@code ssl.truststore.password} (valor por defecto: {@code changeit}).
     *
     * @return {@link SSLContext} configurado con el truststore del servidor.
     * @throws FileNotFoundException si el truststore no existe en la ruta esperada.
     * @throws Exception             si el truststore está dañado o la contraseña es incorrecta.
     */
    private SSLContext crearSSLContext() throws Exception {
        char[] password = System.getProperty("ssl.truststore.password", "changeit").toCharArray();
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE_PATH)) {
            trustStore.load(fis, password);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje.getTipo() == TipoMensaje.LOGIN || mensaje.getTipo() == TipoMensaje.REGISTER) {
            nombreUsuario = mensaje.getRemitente();
        }
        new Thread(() -> {
            try {
                switch (mensaje.getTipo()) {
                    case LOGIN, REGISTER -> doAutenticar(mensaje);
                    case MESSAGE         -> doMensaje(mensaje);
                    case DISCONNECT      -> doDesconectar();
                }
            } catch (IOException e) {
                gui.mostrarError("Error de conexión: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[ClienteHttp] Error inesperado: " + e);
                gui.mostrarError("Error inesperado: " + e.getMessage());
            }
        }, "ClienteHttp").start();
    }

    public void reconectar() { /* HTTPS es sin estado, no es necesario */ }

    public void desconectar() { doDesconectar(); }

    public String getNombreUsuario() { return nombreUsuario; }

    // ── Operaciones internas ─────────────────────────────────────────────────

    /**
     * Gestiona login y registro contra el servidor HTTPS.
     *
     * <ul>
     *   <li><b>Login</b>:    recibe {@code OK|puertoTCP|mensaje}, abre el {@link SSLSocket}
     *       TCP/TLS y arranca el hilo {@link ReceptorMensajes}.</li>
     *   <li><b>Registro</b>: recibe {@code OK|mensaje}, muestra confirmación sin abrir TCP.</li>
     * </ul>
     */
    private void doAutenticar(Mensaje mensaje) throws IOException {
        String path = mensaje.getTipo() == TipoMensaje.LOGIN ? "/login" : "/register";
        String body = "usuario=" + enc(mensaje.getRemitente())
                    + "&password=" + enc(mensaje.getContenido());
        String response = post(path, body);

        // Formato: "OK|puertoTCP|texto" para login  |  "OK|texto" para registro  |  "ERROR|detalle"
        String[] partes = response.split("\\|", 3);

        if ("OK".equals(partes[0])) {

            if (mensaje.getTipo() == TipoMensaje.LOGIN) {
                int puertoTcp = Integer.parseInt(partes[1].trim());

                if (receptor != null) receptor.detener();

                // Abrir SSLSocket TCP/TLS usando el mismo SSLContext con el truststore
                socketTCP = sslContext.getSocketFactory().createSocket(host, puertoTcp);

                OutputStream salidaTCP = socketTCP.getOutputStream();
                salidaTCP.write((nombreUsuario + "\n").getBytes(StandardCharsets.UTF_8));
                salidaTCP.flush();

                receptor = new ReceptorMensajes(socketTCP, gui, nombreUsuario);
                receptor.setDaemon(true);
                receptor.start();

                gui.loginExitoso(partes.length > 2 ? partes[2] : "");

            } else {
                nombreUsuario = null;
                gui.loginExitoso(partes.length > 1 ? partes[1] : "Registro completado.");
            }

        } else {
            nombreUsuario = null;
            gui.mostrarError(partes.length > 1 ? partes[1] : "Error desconocido");
        }
    }

    /** Envía un mensaje de texto al servidor vía HTTPS POST. */
    private void doMensaje(Mensaje mensaje) throws IOException {
        post("/mensaje", "usuario=" + enc(nombreUsuario)
                       + "&contenido=" + enc(mensaje.getContenido()));
    }

    /**
     * Cierra la sesión de forma controlada:
     * <ol>
     *   <li>Detiene el hilo receptor (cierra el socket TCP/TLS).</li>
     *   <li>Notifica al servidor vía HTTPS POST /desconectar.</li>
     *   <li>Cierra el socket TCP/TLS si aún está abierto.</li>
     * </ol>
     */
    private void doDesconectar() {
        if (nombreUsuario == null) return;

        if (receptor != null) { receptor.detener(); receptor = null; }

        try { post("/desconectar", "usuario=" + enc(nombreUsuario)); }
        catch (IOException ignored) {}

        if (socketTCP != null) {
            try { socketTCP.close(); } catch (IOException ignored) {}
            socketTCP = null;
        }

        nombreUsuario = null;
    }

    // ── HTTPS ─────────────────────────────────────────────────────────────────

    /**
     * Realiza un HTTP POST sobre TLS al servidor.
     *
     * <p>Usa el {@link SSLSocketFactory} del {@link SSLContext} cargado con el truststore
     * para que {@link HttpsURLConnection} verifique el certificado del servidor en cada
     * petición, sin aceptar certificados no reconocidos.
     *
     * @param path Ruta del endpoint (p.ej. {@code /login}).
     * @param body Cuerpo codificado en {@code application/x-www-form-urlencoded}.
     * @return Respuesta del servidor como cadena UTF-8.
     * @throws IOException si la conexión falla o el servidor no es accesible.
     */
    private String post(String path, String body) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        // Sin setFixedLengthStreamingMode: HttpsURLConnection almacena el body en
        // buffer interno, calcula Content-Length y envía request + body de forma
        // atómica al llamar a getResponseCode(), evitando problemas de vaciado de
        // registros TLS con streaming parcial.
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }
        int status = conn.getResponseCode();
        InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        } catch (java.net.SocketTimeoutException ignored) {
            // El servidor no cerró la conexión; devolvemos lo ya recibido
        } finally {
            conn.disconnect();
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s != null ? s : "", "UTF-8"); }
        catch (Exception e) { return ""; }
    }
}
