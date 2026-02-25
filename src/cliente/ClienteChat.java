package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ClienteChat {

    private static final String HOST_DEFAULT = "localhost";
    private static final int PUERTO_DEFAULT = 12345;

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private ReceptorMensajes receptor;
    private InterfazGrafica gui;
    private String nombreUsuario;
    private volatile boolean conectado = false;
    private String host;
    private int puerto;

    public static void main(String[] args) {
        String host = HOST_DEFAULT;
        int puerto = PUERTO_DEFAULT;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                puerto = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido. Usando " + PUERTO_DEFAULT);
            }
        }

        final String finalHost = host;
        final int finalPuerto = puerto;

        SwingUtilities.invokeLater(() -> {
            ClienteChat cliente = new ClienteChat();
            cliente.iniciar(finalHost, finalPuerto);
        });
    }

    private void iniciar(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
        gui = new InterfazGrafica(this);
        conectar();
    }

    private void conectar() {
        try {
            socket = new Socket(host, puerto);
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());
            conectado = true;

            receptor = new ReceptorMensajes(entrada, gui);
            receptor.start();

            System.out.println("Conectado al servidor " + host + ":" + puerto);
        } catch (IOException e) {
            gui.mostrarError("No se pudo conectar al servidor: " + e.getMessage());
        }
    }

    public void reconectar() {
        if (conectado) return;
        conectar();
    }

    public void enviarMensaje(Mensaje mensaje) {
        if (mensaje.getTipo() == TipoMensaje.LOGIN || mensaje.getTipo() == TipoMensaje.REGISTER) {
            nombreUsuario = mensaje.getRemitente();
        }

        try {
            if (salida != null && conectado) {
                synchronized (salida) {
                    salida.writeObject(mensaje);
                    salida.flush();
                    salida.reset();
                }
            }
        } catch (IOException e) {
            gui.mostrarError("Error al enviar mensaje: " + e.getMessage());
        }
    }

    public void desconectar() {
        if (!conectado) return;
        conectado = false;

        try {
            enviarMensaje(new Mensaje(TipoMensaje.DISCONNECT, "", nombreUsuario != null ? nombreUsuario : ""));
        } catch (Exception ignored) {}

        try {
            if (receptor != null) receptor.detener();
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        nombreUsuario = null;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
}
