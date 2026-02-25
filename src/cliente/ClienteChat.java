package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ClienteChat {

    private static final String HOST_DEFAULT = "localhost";
    private static final int    PUERTO_DEFAULT = 12345;

    private String baseUrl;
    private ReceptorMensajes receptor;
    private InterfazGrafica gui;
    private volatile String nombreUsuario;

    public static void main(String[] args) {
        String host  = HOST_DEFAULT;
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
        this.baseUrl = "http://" + host + ":" + puerto;
        gui = new InterfazGrafica(this);
    }

    // ── API pública ─────────────────────────────────────────────────────────

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
            }
        }, "ClienteHttp").start();
    }

    public void reconectar() { /* HTTP es sin estado, no es necesario */ }

    public void desconectar() { doDesconectar(); }

    public String getNombreUsuario() { return nombreUsuario; }

    // ── Long polling (llamado desde ReceptorMensajes) ───────────────────────

    public String longPoll(String usuario) throws IOException {
        URL url = new URL(baseUrl + "/mensajes?usuario=" + enc(usuario));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(35_000); // servidor espera 30 s
        int status = conn.getResponseCode();
        if (status == 204) return null; // timeout sin mensaje
        if (status != 200) throw new IOException("HTTP " + status);
        return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // ── Operaciones internas ────────────────────────────────────────────────

    private void doAutenticar(Mensaje mensaje) throws IOException {
        String path = mensaje.getTipo() == TipoMensaje.LOGIN ? "/login" : "/register";
        String body = "usuario=" + enc(mensaje.getRemitente())
                    + "&password=" + enc(mensaje.getContenido());
        String response = post(path, body);
        String[] partes = response.split("\\|", 2);

        if ("OK".equals(partes[0])) {
            if (receptor != null) receptor.detener();
            receptor = new ReceptorMensajes(this, gui, nombreUsuario);
            receptor.setDaemon(true);
            receptor.start();
            gui.loginExitoso(partes.length > 1 ? partes[1] : "");
        } else {
            nombreUsuario = null;
            gui.mostrarError(partes.length > 1 ? partes[1] : "Error desconocido");
        }
    }

    private void doMensaje(Mensaje mensaje) throws IOException {
        post("/mensaje", "usuario=" + enc(nombreUsuario)
                       + "&contenido=" + enc(mensaje.getContenido()));
    }

    private void doDesconectar() {
        if (receptor != null) { receptor.detener(); receptor = null; }
        try { post("/desconectar", "usuario=" + enc(nombreUsuario != null ? nombreUsuario : "")); }
        catch (IOException ignored) {}
        nombreUsuario = null;
    }

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
