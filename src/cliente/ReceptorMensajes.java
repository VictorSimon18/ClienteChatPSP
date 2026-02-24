package cliente;

import comun.Mensaje;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ReceptorMensajes extends Thread {

    private final ObjectInputStream entrada;
    private final InterfazGrafica gui;
    private volatile boolean activo = true;

    public ReceptorMensajes(ObjectInputStream entrada, InterfazGrafica gui) {
        this.entrada = entrada;
        this.gui = gui;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (activo) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensaje(mensaje);
            }
        } catch (EOFException | java.net.SocketException e) {
            if (activo) {
                gui.mostrarMensaje("[Sistema] Conexión con el servidor perdida.");
                gui.volverALogin();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (activo) {
                gui.mostrarMensaje("[Sistema] Error de conexión: " + e.getMessage());
                gui.volverALogin();
            }
        }
    }

    private void procesarMensaje(Mensaje mensaje) {
        switch (mensaje.getTipo()) {
            case OK -> gui.loginExitoso(mensaje.getContenido());
            case ERROR -> gui.mostrarError(mensaje.getContenido());
            case MESSAGE, HELP -> gui.mostrarMensaje(mensaje.toString());
            case PRIVATE -> gui.mostrarMensaje("[Privado] " + mensaje);
            case USER_LIST -> gui.actualizarListaUsuarios(mensaje.getContenido());
            default -> gui.mostrarMensaje(mensaje.toString());
        }
    }

    public void detener() {
        activo = false;
    }
}
