package uk.ac.standrews.cs.shabdiz.impl;

import java.awt.BorderLayout;
import java.io.Console;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import uk.ac.standrews.cs.barreleye.SSHClient;
import uk.ac.standrews.cs.barreleye.SSHClientFactory;
import uk.ac.standrews.cs.barreleye.exception.SSHException;

public abstract class Credentials {

    static final File SSH_HOME = new File(System.getProperty("user.home"), ".ssh");
    static final File SSH_KNOWN_HOSTS = new File(PublicKeyCredentials.SSH_HOME, "known_hosts");

    private final String username;

    protected Credentials() {

        this(getCurrentUser());
    }

    protected Credentials(final String username) {

        this.username = username;
    }

    /**
     * Gets the specified username.
     * 
     * @return the username
     */
    public String getUsername() {

        return username;
    }

    /**
     * Authenticates a given {@link SSHClient}.
     * 
     * @param client the SSH client to authenticate
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract void authenticate(final SSHClient client) throws IOException;

    /**
     * Prompts the given message and reads a password or passphrase.
     * If a console is available, the string is read via command-line with echoing disabled; otherwise using a GUI with masked input.
     * 
     * @param prompt the message to be prompted to the user
     * @return A character array containing the password or passphrase, not including any line-termination characters, or {@code null} if an end of stream has been reached.
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

                    //ignore;
                }

                @Override
                public void ancestorRemoved(final AncestorEvent e) {

                    //ignore;
                }
            });

            dialog.setVisible(true);
            return !option_pane.getValue().equals(JOptionPane.OK_OPTION) ? null : password_field.getPassword();
        }
        finally {
            dialog.dispose();
        }
    }

    static void setSSHKnownHosts(final SSHClientFactory sshSessionFactory) throws SSHException {

        sshSessionFactory.setKnownHosts(SSH_KNOWN_HOSTS.getAbsolutePath());
    }

    protected static String getCurrentUser() {

        return System.getProperty("user.name");
    }

    protected static byte[] toBytes(final char[] chars) {

        final byte[] bytes;
        if (chars == null) {
            bytes = null;
        }
        else {
            bytes = new byte[chars.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) chars[i];
            }
        }
        return bytes;
    }
}
