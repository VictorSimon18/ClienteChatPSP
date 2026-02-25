package cliente;

import comun.Mensaje;

import java.io.IOException;

public class ReceptorMensajes extends Thread {

    private final ClienteChat cliente;
    private final InterfazGrafica gui;
    private final String usuario;
    private volatile boolean activo = true;

    public ReceptorMensajes(ClienteChat cliente, InterfazGrafica gui, String usuario) {
        this.cliente = cliente;
        this.gui     = gui;
        this.usuario = usuario;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (activo) {
            try {
                String respuesta = cliente.longPoll(usuario);
                if (respuesta != null && !respuesta.isEmpty()) {
                    Mensaje msg = Mensaje.fromHttpString(respuesta);
                    if (msg != null) procesarMensaje(msg);
                }
            } catch (IOException e) {
                if (activo) {
                    gui.mostrarMensaje("[Sistema] Conexión perdida: " + e.getMessage());
                    gui.volverALogin();
                    activo = false;
                }
            }
        }
    }

    private void procesarMensaje(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case ERROR         -> gui.mostrarError(mensaje.getContenido());
            case MESSAGE, HELP -> gui.mostrarMensaje(mensaje.toString());
            case PRIVATE       -> gui.mostrarMensaje("[Privado] " + mensaje);
            case USER_LIST     -> gui.actualizarListaUsuarios(mensaje.getContenido());
            default            -> gui.mostrarMensaje(mensaje.toString());
        }
    }

    public void detener() {
        activo = false;
    }
}
