package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada del cliente. Arquitectura híbrida HTTP + TCP:
 *
 * <ul>
 *   <li><b>HTTP</b>: login, registro, envío de mensajes, desconexión.</li>
 *   <li><b>TCP</b>:  canal persistente para recibir mensajes en tiempo real (push del servidor).</li>
 * </ul>
 *
 * <p>Flujo tras un login exitoso:
 * <ol>
 *   <li>Se recibe {@code OK|puertoTCP|mensaje} del servidor HTTP.</li>
 *   <li>Se abre un {@link Socket} TCP al puerto indicado.</li>
 *   <li>Se envía el nombre de usuario por TCP para identificarse.</li>
 *   <li>{@link ReceptorMensajes} queda bloqueado leyendo mensajes push del servidor.</li>
 * </ol>
 */
public class ClienteChat {

    private static final String HOST_DEFAULT   = "localhost";
    private static final int    PUERTO_DEFAULT = 12345;

    private String  baseUrl;
    private String  host;                   // Host del servidor (para el socket TCP)
    private ReceptorMensajes receptor;
    private InterfazGrafica  gui;
    private volatile String  nombreUsuario;
    private volatile Socket  socketTCP;     // Socket TCP persistente para recibir mensajes

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
        this.baseUrl = "http://" + host + ":" + puerto;
        gui = new InterfazGrafica(this);
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
                // Captura errores inesperados (p.ej. NumberFormatException si la respuesta
                // del servidor no tiene el formato esperado) para que no maten el hilo
                // silenciosamente dejando la GUI congelada con los botones deshabilitados.
                System.err.println("[ClienteHttp] Error inesperado: " + e);
                gui.mostrarError("Error inesperado: " + e.getMessage());
            }
        }, "ClienteHttp").start();
    }

    public void reconectar() { /* HTTP es sin estado, no es necesario */ }

    public void desconectar() { doDesconectar(); }

    public String getNombreUsuario() { return nombreUsuario; }

    // ── Operaciones internas ─────────────────────────────────────────────────

    /**
     * Gestiona login y registro contra el servidor HTTP.
     *
     * <ul>
     *   <li><b>Login</b>:    recibe {@code OK|puertoTCP|mensaje}, abre el socket TCP
     *       y arranca el hilo {@link ReceptorMensajes}.</li>
     *   <li><b>Registro</b>: recibe {@code OK|mensaje}, muestra confirmación sin abrir TCP
     *       (el usuario debe hacer login a continuación).</li>
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
                // ── Login exitoso: conectar por TCP ──────────────────────────
                int puertoTcp = Integer.parseInt(partes[1].trim());

                // Detener receptor anterior si existía (re-login)
                if (receptor != null) receptor.detener();

                // Abrir socket TCP persistente con el servidor
                socketTCP = new Socket(host, puertoTcp);

                // Identificarse enviando el nombre de usuario como primera línea
                OutputStream salidaTCP = socketTCP.getOutputStream();
                salidaTCP.write((nombreUsuario + "\n").getBytes(StandardCharsets.UTF_8));
                salidaTCP.flush();

                // Arrancar hilo receptor que leerá mensajes push del servidor
                receptor = new ReceptorMensajes(socketTCP, gui, nombreUsuario);
                receptor.setDaemon(true);
                receptor.start();

                gui.loginExitoso(partes.length > 2 ? partes[2] : "");

            } else {
                // ── Registro exitoso: sin apertura de TCP ────────────────────
                // El usuario debe iniciar sesión por separado
                nombreUsuario = null;
                gui.loginExitoso(partes.length > 1 ? partes[1] : "Registro completado.");
            }

        } else {
            nombreUsuario = null;
            gui.mostrarError(partes.length > 1 ? partes[1] : "Error desconocido");
        }
    }

    /** Envía un mensaje de texto al servidor vía HTTP POST. */
    private void doMensaje(Mensaje mensaje) throws IOException {
        post("/mensaje", "usuario=" + enc(nombreUsuario)
                       + "&contenido=" + enc(mensaje.getContenido()));
    }

    /**
     * Cierra la sesión de forma controlada:
     * <ol>
     *   <li>Detiene el hilo receptor (cierra el socket TCP).</li>
     *   <li>Notifica al servidor vía HTTP POST /desconectar.</li>
     *   <li>Cierra el socket TCP si aún está abierto.</li>
     * </ol>
     */
    private void doDesconectar() {
        if (nombreUsuario == null) return; // Ya desconectado

        // Detener el hilo receptor: sets activo=false y cierra el socket
        if (receptor != null) { receptor.detener(); receptor = null; }

        // Notificar al servidor para que limpie recursos y difunda "ha salido"
        try { post("/desconectar", "usuario=" + enc(nombreUsuario)); }
        catch (IOException ignored) {}

        // Cerrar el socket TCP (puede que ya esté cerrado por receptor.detener())
        if (socketTCP != null) {
            try { socketTCP.close(); } catch (IOException ignored) {}
            socketTCP = null;
        }

        nombreUsuario = null;
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private String post(String path, String body) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }
        int status = conn.getResponseCode();
        InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
        return is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s != null ? s : "", "UTF-8"); }
        catch (Exception e) { return ""; }
    }
}
