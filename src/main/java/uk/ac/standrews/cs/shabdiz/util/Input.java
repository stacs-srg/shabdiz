/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
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

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/**
 * Utility that provides input readers.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Input {

    private static final BufferedReader READER;

    static {
        READER = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Prevent instantiation of utility class.
     */
    private Input() {

    }

    /**
     * Returns the next line from the console.
     * 
     * @return the next line from the console
     */
    public static String readLine(final String prompt) {

        System.out.print(prompt);

        try {
            // Use reader rather than system console as in getMaskedLine(), because it does work within Eclipse.
            return READER.readLine();
        }
        catch (final IOException e) {
            return "";
        }
    }

    public static int readInt(final String prompt) {

        return Integer.parseInt(readLine(prompt));
    }

    /**
     * Gets a masked string, from the console if possible, otherwise using a Swing dialog.
     *
     * @param prompt the user prompt
     * @return the string entered
     */
    public static String readMaskedLine(final String prompt) {

        // Try to read from console. If executed from Eclipse IDE the console will be null.
        final Console console = System.console();
        if (console != null) { return new String(console.readPassword(prompt)); }

        final JPasswordField password_field = new JPasswordField();

        // Setting the focus to the password field.
        // Because of the bug (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574) in JOptionPane, the following workaround is used to set the focus to the password field:
        // Begin workaround
        password_field.addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(final HierarchyEvent e) {

                final Component c = e.getComponent();
                if (c.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {

                            c.requestFocus();
                        }
                    });
                }
            }
        });
        // End workaround

        final JOptionPane option_pane = new JOptionPane(password_field, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        final JDialog dialog = option_pane.createDialog(prompt);

        dialog.setVisible(true);
        final int result = (Integer) option_pane.getValue();
        dialog.dispose();

        if (result == JOptionPane.OK_OPTION) { return String.valueOf(password_field.getPassword()); }
        return "";
    }
}
