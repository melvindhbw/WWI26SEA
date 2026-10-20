package Interface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import ProgrammierKurs.Main;

public class UI extends JFrame {

    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private PipedOutputStream zuMain;

    public UI() {
        setTitle("DHBW Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Farben
        Color bgDark = new Color(24, 26, 32);
        Color panelDark = new Color(33, 36, 44);
        Color accentColor = new Color(79, 70, 229);
        Color textColor = new Color(235, 237, 240);
        Color botColor = new Color(148, 163, 184);

        getContentPane().setBackground(bgDark);

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setBackground(panelDark);
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Nachrichten");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(textColor);

        JLabel subtitleLabel = new JLabel(befehleText());
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(botColor);

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Chat Output Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(bgDark);
        chatArea.setForeground(textColor);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(14, 16, 14, 16));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        add(scrollPane, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(panelDark);
        inputPanel.setBorder(new EmptyBorder(12, 16, 14, 16));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(45, 49, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 80), 1),
                new EmptyBorder(8, 12, 8, 12)));

        sendButton = new JButton("Senden");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(accentColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Main.java im Hintergrund starten und Ein-/Ausgabe ins Fenster umleiten
        mainStarten();

        // Action Listener für Eingabe (Enter oder Button-Klick)
        sendButton.addActionListener(this::handleInput);
        inputField.addActionListener(this::handleInput);
    }

    // Liest die Befehle direkt aus Main.class: jeder String, der mit
    // equals/equalsIgnoreCase verglichen wird (z.B. die switch-cases), ist ein
    // Befehl
    private static String befehleText() {
        Set<String> befehle = new LinkedHashSet<>();
        try (InputStream in = Main.class.getResourceAsStream("Main.class")) {
            ClassModel klasse = ClassFile.of().parse(in.readAllBytes());
            for (MethodModel methode : klasse.methods()) {
                methode.code().ifPresent(code -> {
                    String letzterString = null;
                    for (CodeElement element : code) {
                        if (element instanceof ConstantInstruction konstante
                                && konstante.constantValue() instanceof String text) {
                            letzterString = text;
                        } else if (element instanceof InvokeInstruction aufruf) {
                            String name = aufruf.name().stringValue();
                            if (letzterString != null
                                    && aufruf.owner().asInternalName().equals("java/lang/String")
                                    && (name.equals("equals") || name.equals("equalsIgnoreCase"))) {
                                befehle.add(letzterString);
                            }
                            letzterString = null;
                        } else if (element instanceof Instruction) {
                            letzterString = null;
                        }
                    }
                });
            }
        } catch (Exception ex) {
            return "Befehle konnten nicht gelesen werden";
        }

        if (befehle.isEmpty()) {
            return "Keine Befehle gefunden";
        }
        return "Befehle: '" + String.join("', '", befehle) + "'";
    }

    private void mainStarten() {
        try {
            // Eingabe: alles was hier gesendet wird, liest Main über System.in
            PipedInputStream mainEingabe = new PipedInputStream();
            zuMain = new PipedOutputStream(mainEingabe);
            System.setIn(mainEingabe);
        } catch (IOException ex) {
            appendSystem("Fehler beim Starten: " + ex.getMessage());
            return;
        }

        // Ausgabe: alles was Main mit System.out ausgibt, erscheint im Chat
        System.setOut(new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream zeile = new ByteArrayOutputStream();

            @Override
            public synchronized void write(int b) {
                if (b == '\n') {
                    String text = zeile.toString(StandardCharsets.UTF_8).replace("\r", "");
                    zeile.reset();
                    SwingUtilities.invokeLater(() -> appendChat(text));
                } else {
                    zeile.write(b);
                }
            }
        }, true, StandardCharsets.UTF_8));

        Thread mainThread = new Thread(() -> {
            Main.main(new String[0]);
            // Main ist fertig (z.B. nach 'ex') -> Fenster schließen
            SwingUtilities.invokeLater(() -> {
                appendSystem("Programm beendet.");
                Timer timer = new Timer(500, evt -> {
                    dispose();
                    System.exit(0);
                });
                timer.setRepeats(false);
                timer.start();
            });
        });
        mainThread.setDaemon(true);
        mainThread.start();
    }

    private void handleInput(ActionEvent e) {
        String nachricht = inputField.getText().trim();
        if (nachricht.isEmpty() || zuMain == null) {
            return;
        }

        // Benutzer-Eingabe anzeigen
        appendChat("Du: " + nachricht);
        inputField.setText("");

        // Eingabe an Main weitergeben (wie in der Konsole getippt)
        try {
            zuMain.write((nachricht + "\n").getBytes(StandardCharsets.UTF_8));
            zuMain.flush();
        } catch (IOException ex) {
            appendSystem("Main läuft nicht mehr.");
        }
    }

    private void appendChat(String text) {
        chatArea.append(text + "\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void appendSystem(String text) {
        chatArea.append("[System] " + text + "\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        // Modernes Look-and-Feel initialisieren & UI starten
        SwingUtilities.invokeLater(() -> {
            UI ui = new UI();
            ui.setVisible(true);
        });
    }
}
