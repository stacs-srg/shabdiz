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

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.platform.CygwinPlatform;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;

/**
 * A utility class containing common {@link CommandBuilder command builders}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Commands {

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
    /** checks whether a given path exists. */
    public static final CommandBuilder EXISTS = new CommandBuilder() {

        private static final String ECHO_HOME = "[ -e \"%s\" ] && echo true || echo false";
        private static final String ECHO_USERPROFILE = "if exist \"%s\" (echo true) else (echo false)";

        @Override
        public String get(final Platform platform, final String... params) {

            return String.format(Platforms.isUnixBased(platform) || platform instanceof CygwinPlatform ? ECHO_HOME : ECHO_USERPROFILE, params[0]);
        }
    };
    public static final CommandBuilder GET_TEMP_DIR = new CommandBuilder() {

        private static final String ECHO_TEMP = "echo %TEMP%";
        private static final String ECHO_TMPDIR = "echo $TMPDIR";

        @Override
        public String get(final Platform platform, final String... params) {

            if (params.length != 0) {
                LOGGER.warn("ignoring passed parameters {}", Arrays.toString(params));
            }
            return Platforms.isUnixBased(platform) || platform instanceof CygwinPlatform ? ECHO_TMPDIR : ECHO_TEMP;
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

        private static final String CD = "cd ";

        @Override
        public String get(final Platform platform, final String... params) {

            return CD + platform.quote(params[0]);
        }
    };
    /** Appender. */
    public static final CommandBuilder APPENDER = new CommandBuilder() {

        private static final String AND_AND = " && ";
        private static final String SEMICOLON = "; ";

        @Override
        public String get(final Platform platform, final String... params) {

            final String appender = Platforms.isUnixBased(platform) || platform instanceof CygwinPlatform ? SEMICOLON : AND_AND;
            final StringBuilder builder = new StringBuilder();
            if (params != null) {

                final int params_length = params.length;
                for (int i = 0; i < params_length; i++) {
                    builder.append(params[i]);
                    if (i < params_length - 1) {
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
        private static final String KILL = "kill ";

        /** Given a PID, which is expected as the first element in {@code parameters}, constructs a platform-dependent process termination command. */
        @Override
        public String get(final Platform platform, final String... parameters) {

            if (parameters.length != 1) { throw new IllegalArgumentException("one argument, the pid, is expected as the first parameter"); }
            final Integer pid = Integer.parseInt(parameters[0]);
            LOGGER.debug("generating kill command for pid: {}", pid);
            return concatinateWithSpace(Platforms.isUnixBased(platform) ? KILL : TASKKILL_PID, String.valueOf(pid));
        }
    };
    /** Force kill by PID command builder. */
    public static final CommandBuilder FORCE_KILL_BY_PROCESS_ID = new CommandBuilder() {

        private static final String TASKKILL_PID = "taskkill /F /PID ";
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
    /** Recursively deletes a given file or directory. */
    public static final CommandBuilder DELETE_RECURSIVELY = new CommandBuilder() {

        /*
          Windows Command Prompt does not have a single command like linux 'rm -f' that removes files *and* directories.
         Hence, one has to execute 'rd' and 'del', ignore any errors, and hope that who ever designed Command Prompt gets cancer.

         See: http://stackoverflow.com/questions/338895/what-ever-happened-to-deltree-and-whats-its-replacement
         */

        private static final String RD_AND_DEL_IGNORE_ERRORS = "set del_path=%s && rd /s /q %%del_path%% 2> nul && del /f /q %%del_path%% 2> nul";
        private static final String RM = "rm -rf %s";

        /** Given a path to delete, which is expected as the first element in {@code parameters}, constructs a platform-dependent recursive path removal command. */
        @Override
        public String get(final Platform platform, final String... parameters) {

            if (parameters.length < 1) { throw new IllegalArgumentException("at least one path must be specified"); }
            final String quoted_params = quoteAndConcatinateWithSpace(platform, parameters);
            LOGGER.debug("path(s) to delete: {}", quoted_params);
            return String.format(Platforms.isUnixBased(platform) || platform instanceof CygwinPlatform ? RM : RD_AND_DEL_IGNORE_ERRORS, quoted_params);
        }
    };
    public static final CommandBuilder MAKE_DIRECTORIES = new CommandBuilder() {

        /*
            Ignore errors of mkdir in windows because there is no flag to tell the damn thing to be quiet if directory already exits (i.e like -p on bash). Get cancer and die Command Prompt.
         */
        private static final String MKDIR = "mkdir %s 2> nul";
        private static final String MKDIR_P = "mkdir -p %s";

        @Override
        public String get(final Platform platform, final String... parameters) {

            if (parameters.length == 0) { throw new IllegalArgumentException("at least one directory must be specified"); }
            final String directories = quoteAndConcatinateWithSpace(platform, parameters);
            return String.format(Platforms.isUnixBased(platform) || platform instanceof CygwinPlatform ? MKDIR_P : MKDIR, directories);
        }
    };
    private static final String SPACE = " ";
    private static final Logger LOGGER = LoggerFactory.getLogger(Commands.class);

    private Commands() {

    }

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

    static String quoteAndConcatinateWithSpace(Platform platform, final String... params) {

        final StringBuilder string_builder = new StringBuilder();
        if (params != null) {

            for (final String param : params) {
                string_builder.append(SPACE);
                string_builder.append(platform.quote(param));
            }
        }
        return string_builder.toString();
    }
}
