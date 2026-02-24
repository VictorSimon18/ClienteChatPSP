package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class InterfazGrafica extends JFrame {

    private final ClienteChat cliente;

    // Panel Login
    private JPanel panelLogin;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegistro;
    private JLabel lblEstado;

    // Panel Chat
    private JPanel panelChat;
    private JTextArea areaMensajes;
    private JTextField txtMensaje;
    private JButton btnEnviar;
    private DefaultListModel<String> modeloUsuarios;
    private JList<String> listaUsuarios;
    private JButton btnDesconectar;

    private CardLayout cardLayout;
    private JPanel panelPrincipal;

    public InterfazGrafica(ClienteChat cliente) {
        this.cliente = cliente;
        inicializarUI();
    }

    private void inicializarUI() {
        setTitle("Chat PSP");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(700, 500);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cliente.desconectar();
                dispose();
                System.exit(0);
            }
        });

        cardLayout = new CardLayout();
        panelPrincipal = new JPanel(cardLayout);

        crearPanelLogin();
        crearPanelChat();

        panelPrincipal.add(panelLogin, "LOGIN");
        panelPrincipal.add(panelChat, "CHAT");

        add(panelPrincipal);
        cardLayout.show(panelPrincipal, "LOGIN");

        setVisible(true);
    }

    private void crearPanelLogin() {
        panelLogin = new JPanel(new GridBagLayout());
        panelLogin.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelLogin.setBackground(new Color(240, 240, 245));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Chat PSP", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panelLogin.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panelLogin.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        txtUsuario = new JTextField(15);
        panelLogin.add(txtUsuario, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panelLogin.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        panelLogin.add(txtPassword, gbc);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelBotones.setOpaque(false);
        btnLogin = new JButton("Iniciar Sesión");
        btnRegistro = new JButton("Registrarse");
        panelBotones.add(btnLogin);
        panelBotones.add(btnRegistro);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panelLogin.add(panelBotones, gbc);

        lblEstado = new JLabel(" ", SwingConstants.CENTER);
        lblEstado.setForeground(Color.RED);
        gbc.gridy = 4;
        panelLogin.add(lblEstado, gbc);

        // Acciones
        ActionListener loginAction = e -> {
            String usuario = txtUsuario.getText().trim();
            String password = new String(txtPassword.getPassword());
            if (!usuario.isEmpty() && !password.isEmpty()) {
                lblEstado.setText("Conectando...");
                lblEstado.setForeground(Color.BLUE);
                btnLogin.setEnabled(false);
                btnRegistro.setEnabled(false);
                cliente.enviarMensaje(new Mensaje(TipoMensaje.LOGIN, password, usuario));
            }
        };

        btnLogin.addActionListener(loginAction);
        txtPassword.addActionListener(loginAction);

        btnRegistro.addActionListener(e -> {
            String usuario = txtUsuario.getText().trim();
            String password = new String(txtPassword.getPassword());
            if (!usuario.isEmpty() && !password.isEmpty()) {
                lblEstado.setText("Registrando...");
                lblEstado.setForeground(Color.BLUE);
                cliente.enviarMensaje(new Mensaje(TipoMensaje.REGISTER, password, usuario));
            }
        });
    }

    private void crearPanelChat() {
        panelChat = new JPanel(new BorderLayout(5, 5));
        panelChat.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Área de mensajes
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setLineWrap(true);
        areaMensajes.setWrapStyleWord(true);
        areaMensajes.setFont(new Font("SansSerif", Font.PLAIN, 13));
        areaMensajes.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Lista usuarios
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane scrollUsuarios = new JScrollPane(listaUsuarios);
        scrollUsuarios.setPreferredSize(new Dimension(150, 0));
        JPanel panelUsuarios = new JPanel(new BorderLayout());
        panelUsuarios.add(new JLabel(" Usuarios conectados", SwingConstants.CENTER), BorderLayout.NORTH);
        panelUsuarios.add(scrollUsuarios, BorderLayout.CENTER);

        // Panel central
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollMensajes, panelUsuarios);
        splitPane.setResizeWeight(0.8);
        panelChat.add(splitPane, BorderLayout.CENTER);

        // Panel inferior (escribir mensaje)
        JPanel panelInferior = new JPanel(new BorderLayout(5, 0));
        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnEnviar = new JButton("Enviar");
        btnDesconectar = new JButton("Salir");

        JPanel panelBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panelBtns.add(btnEnviar);
        panelBtns.add(btnDesconectar);

        panelInferior.add(txtMensaje, BorderLayout.CENTER);
        panelInferior.add(panelBtns, BorderLayout.EAST);
        panelChat.add(panelInferior, BorderLayout.SOUTH);

        // Acciones
        ActionListener enviarAction = e -> {
            String texto = txtMensaje.getText().trim();
            if (!texto.isEmpty()) {
                cliente.enviarMensaje(new Mensaje(TipoMensaje.MESSAGE, texto, cliente.getNombreUsuario()));
                txtMensaje.setText("");
            }
            txtMensaje.requestFocus();
        };

        btnEnviar.addActionListener(enviarAction);
        txtMensaje.addActionListener(enviarAction);

        btnDesconectar.addActionListener(e -> {
            cliente.desconectar();
            volverALogin();
        });
    }

    // --- Métodos llamados desde ReceptorMensajes ---

    public void mostrarMensaje(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaMensajes.append(texto + "\n");
            areaMensajes.setCaretPosition(areaMensajes.getDocument().getLength());
        });
    }

    public void mostrarError(String error) {
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText(error);
            lblEstado.setForeground(Color.RED);
            btnLogin.setEnabled(true);
            btnRegistro.setEnabled(true);
        });
    }

    public void loginExitoso(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            areaMensajes.setText("");
            mostrarMensaje("[Sistema] " + mensaje);
            setTitle("Chat PSP - " + cliente.getNombreUsuario());
            cardLayout.show(panelPrincipal, "CHAT");
            txtMensaje.requestFocus();
        });
    }

    public void actualizarListaUsuarios(String listaCSV) {
        SwingUtilities.invokeLater(() -> {
            modeloUsuarios.clear();
            if (listaCSV != null && !listaCSV.isEmpty()) {
                for (String usuario : listaCSV.split(",")) {
                    modeloUsuarios.addElement(usuario.trim());
                }
            }
        });
    }

    public void volverALogin() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(panelPrincipal, "LOGIN");
            lblEstado.setText("Desconectado.");
            lblEstado.setForeground(Color.ORANGE);
            btnLogin.setEnabled(true);
            btnRegistro.setEnabled(true);
            txtPassword.setText("");
            modeloUsuarios.clear();
            setTitle("Chat PSP");
        });
    }
}
