/*
 * Copyright 2013 University of St Andrews School of Computer Science
 * 
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.host;

import java.util.regex.Pattern;

public final class KillCommand {

    private KillCommand() {

        // TODO Auto-generated constructor stub
    }

    public static String get(final String process_command, final String username) {

        //ps -o pid,command -ax -u <USER> | grep '\Q<COMMAND>\E' | sed 's/^[a-z]*[ ]*\([0-9]*\).*/\1/' | tr '\n' ',' | sed 's/,$//g' | xargs pkill -TERM -P $1
        //TODO figure out what's wrong with pkill -f
        final StringBuilder kill_command = new StringBuilder();
        appendListProcessesByUser(kill_command, username);
        appendPipe(kill_command);
        appendSearchByCommand(kill_command, process_command);
        appendPipe(kill_command);
        appendListMatchingProcessIDsSeparatedByComma(kill_command);
        appendPipe(kill_command);
        appendKillProcessAndSubprocesses(kill_command);
        return kill_command.toString();
    }

    private static void appendKillProcessAndSubprocesses(final StringBuilder kill_command) {

        kill_command.append("xargs pkill -TERM -P $1;");
    }

    private static void appendPipe(final StringBuilder kill_command) {

        kill_command.append(" | ");
    }

    private static void appendListMatchingProcessIDsSeparatedByComma(final StringBuilder kill_command) {

        kill_command.append("sed 's/^[a-z]*[ ]*\\([0-9]*\\).*/\\1/'"); // List process IDs, one per line
        appendPipe(kill_command);
        kill_command.append("tr '\\n' ','"); // Replace '\n' with comma
        appendPipe(kill_command);
        kill_command.append("sed 's/,$//g'"); // Remove the tailing comma

    }

    private static void appendSearchByCommand(final StringBuilder kill_command, final String command) {

        kill_command.append("grep '");
        kill_command.append(Pattern.quote(command));
        kill_command.append("'");
    }

    private static void appendListProcessesByUser(final StringBuilder kill_command, final String username) {

        kill_command.append("ps -o pid,command -ax -u ");
        kill_command.append(username);
    }
}
