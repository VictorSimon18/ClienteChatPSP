package cliente;

import comun.Mensaje;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Hilo daemon que mantiene el socket TCP con el servidor y procesa los mensajes
 * recibidos en tiempo real (push del servidor al cliente).
 *
 * <p>Sustituye al antiguo mecanismo de long polling HTTP: en lugar de hacer
 * peticiones GET repetidas cada 30 segundos, este hilo permanece bloqueado
 * en {@code readLine()} y recibe los mensajes en cuanto el servidor los emite.
 *
 * <p>El formato de cada mensaje es el producido por {@link comun.Mensaje#toHttpString()},
 * enviado como una línea de texto terminada en {@code '\n'}.
 */
public class ReceptorMensajes extends Thread {

    private final Socket          socket;
    private final InterfazGrafica gui;
    private final String          usuario;
    private volatile boolean      activo = true;

    /**
     * @param socket  Socket TCP ya conectado al servidor (debe estar abierto).
     * @param gui     Interfaz gráfica donde se mostrarán los mensajes recibidos.
     * @param usuario Nombre del usuario autenticado (para comparar remitentes).
     */
    public ReceptorMensajes(Socket socket, InterfazGrafica gui, String usuario) {
        this.socket  = socket;
        this.gui     = gui;
        this.usuario = usuario;
        setDaemon(true);
        setName("ReceptorTCP-" + usuario);
    }

    @Override
    public void run() {
        try {
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Leer mensajes del servidor línea a línea hasta que se cierre la conexión
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                if (!linea.isEmpty()) {
                    Mensaje msg = Mensaje.fromHttpString(linea);
                    if (msg != null) procesarMensaje(msg);
                }
            }

        } catch (IOException e) {
            // Solo notificar si la desconexión no fue voluntaria (llamada a detener())
            if (activo) {
                gui.mostrarMensaje("[Sistema] Conexión perdida: " + e.getMessage());
                gui.volverALogin();
            }
        }
    }

    /** Delega en la interfaz gráfica la presentación del mensaje según su tipo. */
    private void procesarMensaje(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case ERROR         -> gui.mostrarError(mensaje.getContenido());
            case MESSAGE, HELP -> gui.mostrarMensaje(mensaje.toString());
            case PRIVATE       -> gui.mostrarMensaje("[Privado] " + mensaje);
            case USER_LIST     -> gui.actualizarListaUsuarios(mensaje.getContenido());
            default            -> gui.mostrarMensaje(mensaje.toString());
        }
    }

    /**
     * Detiene el receptor de forma controlada.
     * Pone {@code activo = false} antes de cerrar el socket para que la excepción
     * de E/S resultante no se interprete como una desconexión inesperada.
     */
    public void detener() {
        activo = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
