package cliente;

import comun.Mensaje;
import comun.TipoMensaje;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class InterfazGrafica extends JFrame {

    // ── Paleta de colores ─────────────────────────────────────
    private static final Color C_BG      = new Color(0xF8F9FA);
    private static final Color C_SURFACE = Color.WHITE;
    private static final Color C_PRIMARY = new Color(0x6366F1);
    private static final Color C_PRI_HVR = new Color(0x4F46E5);
    private static final Color C_TEXT    = new Color(0x1F2937);
    private static final Color C_MUTED   = new Color(0x94FF02);
    private static final Color C_BORDER  = new Color(0xE2E2E6);
    private static final Color C_SIDEBAR = new Color(0xF3F4F6);
    private static final Color C_ERROR   = new Color(0xEF4444);
    private static final Color C_WARN    = new Color(0xF59E0B);

    // Colores de burbujas (como strings hex para HTML)
    private static final String H_OWN_BG  = "#6366F1"; // fondo burbuja propia (mensajes enviados por el usuario)
    private static final String H_OWN_FG  = "#FFFFFF"; // texto burbuja propia
    private static final String H_OTH_BG  = "#E5E7EB"; // fondo burbuja ajena (mensajes de otros usuarios)
    private static final String H_OTH_FG  = "#1F2937"; // texto burbuja ajena
    private static final String H_SYS     = "#454547FF"; // mensajes de sistema genéricos (gris)
    private static final String H_JOIN    = "#8ccc7a"; // notificación de usuario que se une al chat (verde)
    private static final String H_LEAVE   = "#DC2626"; // notificación de usuario que abandona el chat (rojo)
    private static final String H_PRIV_BG = "#FEF3C7"; // fondo mensaje privado (amarillo claro)
    private static final String H_PRIV_FG = "#92400E"; // texto mensaje privado (marrón)

    // ── Fuentes ───────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font F_SUB    = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font F_LABEL  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_INPUT  = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font F_BTN    = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_SIDE_H = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font F_SIDE   = new Font("Segoe UI", Font.PLAIN, 13);

    private final ClienteChat cliente;
    private boolean estaRegistrando = false;

    // ── Componentes Login ─────────────────────────────────────
    private JPanel       panelLogin;
    private JTextField   txtUsuario;
    private JPasswordField txtPassword;
    private JButton      btnLogin;
    private JButton      btnRegistro;
    private JLabel       lblEstado;

    // ── Componentes Chat ──────────────────────────────────────
    private JPanel       panelChat;
    private JTextPane    areaMensajes;
    private JScrollPane  scrollMensajes;
    private StringBuilder mensajesHtml = new StringBuilder();
    private JTextField   txtMensaje;
    private JButton      btnEnviar;
    private DefaultListModel<String> modeloUsuarios;
    private JList<String>            listaUsuarios;
    private JButton      btnDesconectar;
    private JLabel       lblHeaderNombre;

    private CardLayout cardLayout;
    private JPanel     panelPrincipal;

    public InterfazGrafica(ClienteChat cliente) {
        this.cliente = cliente;
        inicializarUI();
    }

    // =========================================================
    // INICIALIZACIÓN
    // =========================================================

    private void inicializarUI() {
        setTitle("Chat PSP");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(860, 600);
        setMinimumSize(new Dimension(620, 460));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cliente.desconectar();
                dispose();
                System.exit(0);
            }
        });

        cardLayout    = new CardLayout();
        panelPrincipal = new JPanel(cardLayout);

        crearPanelLogin();
        crearPanelChat();

        panelPrincipal.add(panelLogin, "LOGIN");
        panelPrincipal.add(panelChat,  "CHAT");

        add(panelPrincipal);
        cardLayout.show(panelPrincipal, "LOGIN");
        setVisible(true);
    }

    // =========================================================
    // PANEL LOGIN
    // =========================================================

    private void crearPanelLogin() {
        // Fondo exterior
        panelLogin = new JPanel(new GridBagLayout());
        panelLogin.setBackground(C_BG);

        // Tarjeta blanca centrada
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1),
            new EmptyBorder(44, 52, 40, 52)
        ));

        // Círculo de logo
        JPanel logo = crearLogo();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logo);
        card.add(Box.createVerticalStrut(20));

        // Título
        JLabel lblTitulo = new JLabel("Chat PSP", SwingConstants.CENTER);
        lblTitulo.setFont(F_TITLE);
        lblTitulo.setForeground(C_PRIMARY);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblTitulo);
        card.add(Box.createVerticalStrut(4));

        // Subtítulo
        JLabel lblSub = new JLabel("Inicia sesión para continuar", SwingConstants.CENTER);
        lblSub.setFont(F_SUB);
        lblSub.setForeground(C_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSub);
        card.add(Box.createVerticalStrut(32));

        // Campo usuario
        card.add(crearEtiquetaCampo("Usuario"));
        card.add(Box.createVerticalStrut(6));
        txtUsuario = new JTextField();
        estilizarCampo(txtUsuario);
        card.add(txtUsuario);
        card.add(Box.createVerticalStrut(18));

        // Campo contraseña
        card.add(crearEtiquetaCampo("Contraseña"));
        card.add(Box.createVerticalStrut(6));
        txtPassword = new JPasswordField();
        estilizarCampo(txtPassword);
        card.add(txtPassword);
        card.add(Box.createVerticalStrut(28));

        // Botón iniciar sesión
        btnLogin = crearBotonPrimario("Iniciar Sesión");
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(12));

        // Botón registrarse
        btnRegistro = crearBotonSecundario("Registrarse");
        card.add(btnRegistro);
        card.add(Box.createVerticalStrut(18));

        // Etiqueta de estado
        lblEstado = new JLabel(" ", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setForeground(C_ERROR);
        lblEstado.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblEstado);

        panelLogin.add(card);

        // ── Acciones ──────────────────────────────────────────
        ActionListener accionLogin = e -> {
            String usuario  = txtUsuario.getText().trim();
            String password = new String(txtPassword.getPassword());
            if (!usuario.isEmpty() && !password.isEmpty()) {
                estaRegistrando = false;
                lblEstado.setText("Conectando...");
                lblEstado.setForeground(C_PRIMARY);
                btnLogin.setEnabled(false);
                btnRegistro.setEnabled(false);
                cliente.reconectar();
                cliente.enviarMensaje(new Mensaje(TipoMensaje.LOGIN, password, usuario));
            }
        };

        btnLogin.addActionListener(accionLogin);
        txtPassword.addActionListener(accionLogin);

        btnRegistro.addActionListener(e -> {
            String usuario  = txtUsuario.getText().trim();
            String password = new String(txtPassword.getPassword());
            if (!usuario.isEmpty() && !password.isEmpty()) {
                estaRegistrando = true;
                lblEstado.setText("Registrando...");
                lblEstado.setForeground(C_PRIMARY);
                btnLogin.setEnabled(false);
                btnRegistro.setEnabled(false);
                cliente.reconectar();
                cliente.enviarMensaje(new Mensaje(TipoMensaje.REGISTER, password, usuario));
            }
        });
    }

    // =========================================================
    // PANEL CHAT
    // =========================================================

    private void crearPanelChat() {
        panelChat = new JPanel(new BorderLayout());
        panelChat.setBackground(C_BG);

        // ── Barra superior ────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_PRIMARY);
        header.setBorder(new EmptyBorder(0, 20, 0, 16));
        header.setPreferredSize(new Dimension(0, 58));

        JLabel lblApp = new JLabel("Chat PSP");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblApp.setForeground(Color.WHITE);
        header.add(lblApp, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        lblHeaderNombre = new JLabel("");
        lblHeaderNombre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblHeaderNombre.setForeground(new Color(0xC7D2FE));
        btnDesconectar = crearBotonHeader("Salir");
        headerRight.add(lblHeaderNombre);
        headerRight.add(btnDesconectar);
        header.add(headerRight, BorderLayout.EAST);
        panelChat.add(header, BorderLayout.NORTH);

        // ── Área de mensajes (HTML) ───────────────────────────
        areaMensajes = new JTextPane();
        areaMensajes.setEditable(false);
        areaMensajes.setContentType("text/html");
        areaMensajes.setBackground(C_BG);
        areaMensajes.setBorder(new EmptyBorder(8, 8, 8, 8));
        areaMensajes.setText(htmlBase(""));

        scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.setBorder(BorderFactory.createEmptyBorder());
        scrollMensajes.getVerticalScrollBar().setUnitIncrement(16);

        // ── Panel lateral de usuarios ─────────────────────────
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios  = new JList<>(modeloUsuarios);
        listaUsuarios.setFont(F_SIDE);
        listaUsuarios.setBackground(C_SIDEBAR);
        listaUsuarios.setForeground(C_TEXT);
        listaUsuarios.setFixedCellHeight(38);
        listaUsuarios.setCellRenderer(new UsuarioCellRenderer());

        JScrollPane scrollUsuarios = new JScrollPane(listaUsuarios);
        scrollUsuarios.setBorder(BorderFactory.createEmptyBorder());

        JPanel cabSide = new JPanel(new BorderLayout());
        cabSide.setBackground(C_SIDEBAR);
        cabSide.setBorder(new EmptyBorder(16, 16, 10, 16));
        JLabel lblSideTitle = new JLabel("USUARIOS");
        lblSideTitle.setFont(F_SIDE_H);
        lblSideTitle.setForeground(C_MUTED);
        cabSide.add(lblSideTitle);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(C_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 1, 0, 0, C_BORDER));
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.add(cabSide, BorderLayout.NORTH);
        sidebar.add(scrollUsuarios, BorderLayout.CENTER);

        // ── Split pane ────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollMensajes, sidebar);
        split.setResizeWeight(1.0);
        split.setBorder(null);
        split.setDividerSize(1);
        panelChat.add(split, BorderLayout.CENTER);

        // ── Barra de entrada ──────────────────────────────────
        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setBackground(C_SURFACE);
        inputBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, C_BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));

        txtMensaje = new JTextField();
        txtMensaje.setFont(F_INPUT);
        txtMensaje.setForeground(C_TEXT);
        txtMensaje.setBackground(C_BG);
        txtMensaje.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1),
            new EmptyBorder(9, 14, 9, 14)
        ));

        btnEnviar = crearBotonPrimario("Enviar");
        btnEnviar.setPreferredSize(new Dimension(96, 42));
        btnEnviar.setMaximumSize(new Dimension(96, 42));

        inputBar.add(txtMensaje, BorderLayout.CENTER);
        inputBar.add(btnEnviar,  BorderLayout.EAST);
        panelChat.add(inputBar, BorderLayout.SOUTH);

        // ── Acciones ──────────────────────────────────────────
        ActionListener accionEnviar = e -> {
            String texto = txtMensaje.getText().trim();
            if (!texto.isEmpty()) {
                cliente.enviarMensaje(new Mensaje(TipoMensaje.MESSAGE, texto, cliente.getNombreUsuario()));
                txtMensaje.setText("");
            }
            txtMensaje.requestFocus();
        };

        btnEnviar.addActionListener(accionEnviar);
        txtMensaje.addActionListener(accionEnviar);

        btnDesconectar.addActionListener(e -> {
            cliente.desconectar();
            volverALogin();
        });
    }

    // =========================================================
    // RENDERIZADO DE BURBUJAS
    // =========================================================

    private void agregarBurbuja(String htmlBurbuja) {
        // Llamar siempre desde EDT
        mensajesHtml.append(htmlBurbuja);
        areaMensajes.setText(htmlBase(mensajesHtml.toString()));
        // Doble invokeLater para asegurar scroll después del repintado
        SwingUtilities.invokeLater(() ->
            scrollMensajes.getVerticalScrollBar().setValue(
                scrollMensajes.getVerticalScrollBar().getMaximum()
            )
        );
    }

    private String htmlBase(String body) {
        return "<html><body style='background-color:#F8F9FA;"
             + "font-family:Segoe UI,Arial,sans-serif;margin:6px;padding:0;'>"
             + body + "</body></html>";
    }

    /** Burbuja propia (derecha, índigo) */
    private String htmlBurbujaPropia(String texto, String hora) {
        return "<table width='100%' border='0' cellpadding='3' cellspacing='0'>"
             + "<tr><td width='22%'>&nbsp;</td>"
             + "<td bgcolor='" + H_OWN_BG + "' style='padding:9px 14px;'>"
             + "<font color='" + H_OWN_FG + "' face='Segoe UI,Arial' size='4'>"
             + esc(texto) + "</font>"
             + "<br><font color='#C7D2FE' size='2'>" + esc(hora) + "</font>"
             + "</td></tr></table>";
    }

    /** Burbuja ajena (izquierda, gris) */
    private String htmlBurbujaAjena(String remitente, String texto, String hora) {
        return "<table width='100%' border='0' cellpadding='3' cellspacing='0'>"
             + "<tr><td width='78%' bgcolor='" + H_OTH_BG + "' style='padding:9px 14px;'>"
             + "<font color='" + H_SYS + "' size='3'><b>" + esc(remitente) + "</b></font><br>"
             + "<font color='" + H_OTH_FG + "' face='Segoe UI,Arial' size='4'>"
             + esc(texto) + "</font>"
             + "<br><font color='" + H_SYS + "' size='2'>" + esc(hora) + "</font>"
             + "</td><td width='22%'>&nbsp;</td></tr></table>";
    }

    /** Mensaje de sistema (centrado, gris cursiva) */
    private String htmlSistema(String texto) {
        return "<table width='100%' border='0' cellpadding='6' cellspacing='0'>"
             + "<tr><td align='center'>"
             + "<font color='" + H_SYS + "' size='3'><i>" + esc(texto) + "</i></font>"
             + "</td></tr></table>";
    }

    /** Mensaje de unión al chat (centrado, verde esmeralda cursiva) */
    private String htmlUnion(String texto) {
        return "<table width='100%' border='0' cellpadding='6' cellspacing='0'>"
             + "<tr><td align='center'>"
             + "<font color='" + H_JOIN + "' size='3'><i>" + esc(texto) + "</i></font>"
             + "</td></tr></table>";
    }

    /** Mensaje de salida del chat (centrado, rojo cursiva) */
    private String htmlSalida(String texto) {
        return "<table width='100%' border='0' cellpadding='6' cellspacing='0'>"
             + "<tr><td align='center'>"
             + "<font color='" + H_LEAVE + "' size='3'><i>" + esc(texto) + "</i></font>"
             + "</td></tr></table>";
    }

    /** Mensaje privado (izquierda, amarillo) */
    private String htmlPrivado(String remitente, String texto, String hora) {
        return "<table width='100%' border='0' cellpadding='3' cellspacing='0'>"
             + "<tr><td width='78%' bgcolor='" + H_PRIV_BG + "' style='padding:9px 14px;'>"
             + "<font color='" + H_PRIV_FG + "' size='3'><b>[Privado] " + esc(remitente) + "</b></font><br>"
             + "<font color='" + H_PRIV_FG + "' face='Segoe UI,Arial' size='4'>"
             + esc(texto) + "</font>"
             + "<br><font color='" + H_SYS + "' size='2'>" + esc(hora) + "</font>"
             + "</td><td width='22%'>&nbsp;</td></tr></table>";
    }

    private static boolean esEventoSistema(String content) {
        String low = content.toLowerCase();
        return low.contains("se uni") || low.contains("ha salido") || low.contains("se desconect") || low.contains("abandonó");
    }

    private String htmlSegunEvento(String content) {
        String low = content.toLowerCase();
        if (low.contains("se uni"))                                          return htmlUnion(content);
        if (low.contains("ha salido") || low.contains("se desconect") || low.contains("abandonó")) return htmlSalida(content);
        return htmlSistema(content);
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =========================================================
    // PARSEO DE MENSAJES
    // =========================================================

    /** Parsea "[HH:mm:ss] remitente: contenido" */
    private MensajeParseado parsear(String texto) {
        if (texto == null || !texto.startsWith("[")) return null;
        int cierre = texto.indexOf(']');
        if (cierre == -1) return null;
        String hora  = texto.substring(1, cierre);
        String resto = texto.substring(cierre + 1).trim();
        int colon    = resto.indexOf(": ");
        if (colon == -1) return null;
        return new MensajeParseado(hora, resto.substring(0, colon).trim(), resto.substring(colon + 2));
    }

    private static class MensajeParseado {
        final String hora, remitente, contenido;
        MensajeParseado(String hora, String remitente, String contenido) {
            this.hora = hora; this.remitente = remitente; this.contenido = contenido;
        }
    }

    // =========================================================
    // API PÚBLICA (llamada desde ReceptorMensajes)
    // =========================================================

    public void mostrarMensaje(String texto) {
        SwingUtilities.invokeLater(() -> {
            if (texto == null || texto.isBlank()) return;

            if (texto.startsWith("[Sistema]")) {
                String content = texto.substring("[Sistema]".length()).trim();
                agregarBurbuja(htmlSegunEvento(content));
                return;
            }

            if (texto.startsWith("[Privado]")) {
                MensajeParseado mp = parsear(texto.substring("[Privado]".length()).trim());
                if (mp != null) agregarBurbuja(htmlPrivado(mp.remitente, mp.contenido, mp.hora));
                else             agregarBurbuja(htmlSistema(texto));
                return;
            }

            MensajeParseado mp = parsear(texto);
            if (mp != null) {
                if (esEventoSistema(mp.contenido)) {
                    agregarBurbuja(htmlSegunEvento(mp.contenido));
                } else {
                    String yo = cliente.getNombreUsuario();
                    if (yo != null && yo.equals(mp.remitente))
                        agregarBurbuja(htmlBurbujaPropia(mp.contenido, mp.hora));
                    else
                        agregarBurbuja(htmlBurbujaAjena(mp.remitente, mp.contenido, mp.hora));
                }
            } else {
                agregarBurbuja(htmlSistema(texto));
            }
        });
    }

    public void mostrarError(String error) {
        SwingUtilities.invokeLater(() -> {
            // Si estamos en el panel del chat, mostrar el error como burbuja de sistema
            // para que sea visible (lblEstado solo es visible en el panel de login).
            Component panelVisible = null;
            for (Component c : panelPrincipal.getComponents()) {
                if (c.isVisible()) { panelVisible = c; break; }
            }
            if (panelVisible == panelChat) {
                agregarBurbuja(htmlSistema("Error: " + error));
            } else {
                lblEstado.setText(error);
                lblEstado.setForeground(C_ERROR);
                btnLogin.setEnabled(true);
                btnRegistro.setEnabled(true);
            }
        });
    }

    public void loginExitoso(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (estaRegistrando) {
                estaRegistrando = false;
                btnLogin.setEnabled(true);
                btnRegistro.setEnabled(true);
                lblEstado.setText(" ");
                mostrarPopupRegistro();
            } else {
                mensajesHtml = new StringBuilder();
                areaMensajes.setText(htmlBase(""));
                agregarBurbuja(htmlSistema(mensaje));
                setTitle("Chat PSP — " + cliente.getNombreUsuario());
                lblHeaderNombre.setText(cliente.getNombreUsuario());
                cardLayout.show(panelPrincipal, "CHAT");
                txtMensaje.requestFocus();
            }
        });
    }

    private void mostrarPopupRegistro() {
        JDialog dialog = new JDialog(this, "Registro completado", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(340, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(28, 36, 24, 36));

        JLabel icono = new JLabel("✓", SwingConstants.CENTER);
        icono.setFont(new Font("Segoe UI", Font.BOLD, 40));
        icono.setForeground(new Color(0x059669));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(icono);
        content.add(Box.createVerticalStrut(10));

        JLabel titulo = new JLabel("¡Registro completado!", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(C_TEXT);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titulo);
        content.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Ya puedes iniciar sesión.", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(C_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(20));

        JButton btnVolver = crearBotonPrimario("Volver al inicio de sesión");
        content.add(btnVolver);

        dialog.add(content);
        btnVolver.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    public void actualizarListaUsuarios(String listaCSV) {
        SwingUtilities.invokeLater(() -> {
            modeloUsuarios.clear();
            if (listaCSV != null && !listaCSV.isEmpty()) {
                for (String u : listaCSV.split(","))
                    modeloUsuarios.addElement(u.trim());
            }
        });
    }

    public void volverALogin() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(panelPrincipal, "LOGIN");
            lblEstado.setText("Desconectado.");
            lblEstado.setForeground(C_WARN);
            btnLogin.setEnabled(true);
            btnRegistro.setEnabled(true);
            txtPassword.setText("");
            modeloUsuarios.clear();
            setTitle("Chat PSP");
        });
    }

    // =========================================================
    // COMPONENTES PERSONALIZADOS
    // =========================================================

    private JPanel crearLogo() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String t = "PSP";
                g2.drawString(t,
                    (getWidth()  - fm.stringWidth(t)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                );
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(90, 42); }
            @Override public Dimension getMaximumSize()   { return new Dimension(90, 42); }
        };
    }

    private JLabel crearEtiquetaCampo(String texto) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(F_LABEL);
        lbl.setForeground(C_TEXT);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return lbl;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(F_INPUT);
        campo.setForeground(C_TEXT);
        campo.setBackground(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORDER, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        campo.setPreferredSize(new Dimension(290, 44));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        campo.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    /** Botón principal (relleno índigo, texto blanco, esquinas redondeadas) */
    private JButton crearBotonPrimario(String texto) {
        JButton btn = new JButton(texto) {
            boolean over = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { over = false; repaint(); }
            }); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? (over ? C_PRI_HVR : C_PRIMARY) : C_BORDER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isEnabled() ? Color.WHITE : C_MUTED);
                g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                );
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(290, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Botón secundario (borde índigo, fondo blanco) */
    private JButton crearBotonSecundario(String texto) {
        JButton btn = new JButton(texto) {
            boolean over = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { over = false; repaint(); }
            }); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(over ? new Color(0xEEF2FF) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(C_PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                );
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(C_PRIMARY);
        btn.setPreferredSize(new Dimension(290, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Botón pequeño para la barra de header */
    private JButton crearBotonHeader(String texto) {
        JButton btn = new JButton(texto) {
            boolean over = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { over = false; repaint(); }
            }); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(over ? new Color(0xEF4444) : new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2
                );
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(72, 34));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Renderer de celdas de la lista de usuarios */
    private class UsuarioCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            lbl.setFont(F_SIDE);
            lbl.setBorder(new EmptyBorder(6, 16, 6, 16));
            lbl.setText("\u25CF  " + value);
            lbl.setForeground(isSelected ? C_PRIMARY : C_TEXT);
            lbl.setBackground(isSelected ? new Color(0xEEF2FF) : C_SIDEBAR);
            return lbl;
        }
    }
}
