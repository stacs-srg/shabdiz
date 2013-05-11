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
package uk.ac.standrews.cs.shabdiz.host.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

/**
 * A utility class containing common {@link CommandBuilder command builders}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Commands {

    private static final String SPACE = " ";
    private static final Logger LOGGER = LoggerFactory.getLogger(Commands.class);

    private Commands() {

    }

    /** Gets the current working directory. */
    public static final CommandBuilder ECHO = new CommandBuilder() {

        private static final String ECHO = "echo";

        @Override
        public String get(final Platform platform, final String... params) {

            return ECHO + concatinateWithSpace(params);
        }
    };

    /** Gets the current working directory. */
    public static final CommandBuilder CURRENT_WORKING_DIRECTORY = new CommandBuilder() {

        private static final String PWD = "pwd";
        private static final String ECHO_CD = "echo %cd%";

        @Override
        public String get(final Platform platform, final String... params) {

            return Platforms.isUnixBased(platform) ? PWD : ECHO_CD;
        }
    };

    /** Gets user home directory. */
    public static final CommandBuilder USER_HOME = new CommandBuilder() {

        private static final String ECHO_HOME = "echo $HOME";
        private static final String ECHO_USERPROFILE = "echo %USERPROFILE%";

        @Override
        public String get(final Platform platform, final String... params) {

            return Platforms.isUnixBased(platform) ? ECHO_HOME : ECHO_USERPROFILE;
        }
    };

    /** Gets user name. */
    public static final CommandBuilder USER_NAME = new CommandBuilder() {

        private static final String WHOAMI = "whoami";
        private static final String ECHO_USERNAME = "echo %USERNAME%";

        @Override
        public String get(final Platform platform, final String... params) {

            return Platforms.isUnixBased(platform) ? WHOAMI : ECHO_USERNAME;
        }
    };

    /** Gets operating system name. */
    public static final CommandBuilder OS_NAME = new CommandBuilder() {

        public static final String UNAME = "uname";
        public static final String VER = "ver";

        @Override
        public String get(final Platform platform, final String... params) {

            return Platforms.isUnixBased(platform) ? UNAME : VER;
        }
    };

    /** The CD command builder. */
    public static final CommandBuilder CHANGE_DIRECTORY = new CommandBuilder() {

        private static final String CD = "cd";

        @Override
        public String get(final Platform platform, final String... params) {

            return CD + concatinateWithSpace(params);
        }
    };

    /** The CD command builder. */
    public static final CommandBuilder APPENDER = new CommandBuilder() {

        private static final String AND_AND = " && ";
        private static final String SEMICOLON = "; ";

        @Override
        public String get(final Platform platform, final String... params) {

            final String appender = Platforms.isUnixBased(platform) ? SEMICOLON : AND_AND;
            final StringBuilder builder = new StringBuilder();
            if (params != null) {

                final int params_length = params.length;
                for (int i = 0; i < params_length; i++) {
                    builder.append(params[i]);
                    if (i < params_length) {
                        builder.append(appender);
                    }
                }
            }
            return builder.toString();
        }
    };

    /** The kill by PID command builder. */
    public static final CommandBuilder KILL_BY_PROCESS_ID = new CommandBuilder() {

        private static final String TASKKILL_PID = "taskkill /PID ";
        private static final String KILL_9 = "kill -9 ";

        /** Given a PID, which is expected as the first element in {@code parameters}, constructs a platform-dependent process termination command. */
        @Override
        public String get(final Platform platform, final String... parameters) {

            if (parameters.length != 1) { throw new IllegalArgumentException("one argument, the pid, is expected as the first parameter"); }
            final Integer pid = Integer.parseInt(parameters[0]);
            LOGGER.debug("generating kill command for pid: {}", pid);
            return concatinateWithSpace(Platforms.isUnixBased(platform) ? KILL_9 : TASKKILL_PID, String.valueOf(pid));
        }
    };

    static String concatinateWithSpace(final String... params) {

        final StringBuilder string_builder = new StringBuilder();
        if (params != null) {

            for (final String param : params) {
                string_builder.append(SPACE);
                string_builder.append(param);
            }
        }
        return string_builder.toString();
    }
}
