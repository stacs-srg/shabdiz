/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.util;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * Utility that provides input readers.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Input {

    public static final String DEFAULT_CHARACTER_ENCODING = "UTF8";

    /** Prevent instantiation of utility class. */
    private Input() {

    }

    /**
     * Returns the next line from the console.
     *
     * @param prompt the message to be prompted to the user
     * @return the next line from the console
     * @throws IOException if an I/O error occurs
     */
    public static String readLine(final String prompt) throws IOException {

        System.out.print(prompt);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, DEFAULT_CHARACTER_ENCODING));
        return reader.readLine();
    }

    /**
     * Prompts the given message and reads a password or passphrase.
     * If {@code System.console() != null}, the string is read via command-line with echoing disabled; otherwise using a GUI with masked input.
     *
     * @param prompt the message to be prompted to the user
     * @return A character array containing the password or passphrase, not including any line-termination characters, or {@code null} if an end of stream has been reached.
     * @see Console#readPassword()
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
            //Put the focus on the textfield.
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
        } finally {
            dialog.dispose();
        }
    }
}
