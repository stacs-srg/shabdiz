package uk.ac.standrews.cs.shabdiz.impl;

import com.ariabod.barreleye.SSHSession;
import com.ariabod.barreleye.SSHSessionFactory;
import com.ariabod.barreleye.exception.SSHException;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.io.Console;
import java.io.File;
import java.io.IOException;

public abstract class Credentials {

    static final File SSH_HOME = new File(System.getProperty("user.home"), ".ssh");
    static final File SSH_KNOWN_HOSTS = new File(PublicKeyCredentials.SSH_HOME, "known_hosts");
    private final String username;

    public Credentials() {

        this(System.getProperty("user.name"));
    }

    public Credentials(final String username) {

        this.username = username;
    }

    /**
     * Gets a masked string, from the console if possible, otherwise using a Swing dialog.
     *
     * @param prompt the user prompt
     * @return the string entered
     */
    public static char[] readPassword(final String prompt) {

        final Console console = System.console();
        return console != null ? console.readPassword(prompt) : readPasswordViaGUI(prompt);
    }

    private static char[] readPasswordViaGUI(final String prompt) {

        final JPanel panel = new JPanel(new BorderLayout());
        final JLabel prompt_label = new JLabel(prompt);
        final JPasswordField password_field = new JPasswordField(10);
        panel.add(prompt_label, BorderLayout.NORTH);
        panel.add(password_field, BorderLayout.CENTER);
        final JOptionPane option_pane = new JOptionPane(panel, JOptionPane.DEFAULT_OPTION);
        final JDialog dialog = option_pane.createDialog("Password");
        try {
            password_field.addAncestorListener(new AncestorListener() {

                @Override
                public void ancestorAdded(final AncestorEvent e) {

                    final JComponent component = e.getComponent();
                    component.requestFocusInWindow();
                }

                @Override
                public void ancestorMoved(final AncestorEvent e) {

                }

                @Override
                public void ancestorRemoved(final AncestorEvent e) {

                }
            });
            dialog.setVisible(true);
            return !option_pane.getValue().equals(JOptionPane.OK_OPTION) ? null : password_field.getPassword();
        } finally {
            dialog.dispose();
        }
    }

    static void setSSHKnownHosts(final SSHSessionFactory sshSessionFactory) throws SSHException {

        sshSessionFactory.setKnownHosts(SSH_KNOWN_HOSTS.getAbsolutePath());
    }

    protected static byte[] toBytes(final char[] chars) {

        final byte[] bytes;
        if (chars == null) {
            bytes = null;
        } else {
            bytes = new byte[chars.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) chars[i];
            }
        }
        return bytes;
    }

    abstract void authenticate(final SSHSession session) throws IOException;

    public String getUsername() {

        return username;
    }
}
