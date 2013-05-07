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

    private static final Logger LOGGER = LoggerFactory.getLogger(Commands.class);

    private Commands() {

    }

    /** The CD command builder. */
    public static final CommandBuilder CHANGE_DIRECTORY = new CommandBuilder() {

        @Override
        public String get(final Platform platform, final String... params) {

            return "cd" + concatinate(params);
        }
    };

    /** The kill by PID command builder. */
    public static final CommandBuilder KILL_BY_PROCESS_ID = new CommandBuilder() {

        /**
         * Given a PID, which is expected as the first element in {@code parameters}, constructs a platform-dependent process termination command.
         */
        @Override
        public String get(final Platform platform, final String... parameters) {

            if (parameters.length != 1) { throw new IllegalArgumentException("Only the pid is expected as the first parameter"); }
            final Integer pid = Integer.parseInt(parameters[0]);
            LOGGER.debug("generating kill command for pid: {}", pid);
            return concatinate(Platforms.isUnixBased(platform) ? "kill -9 " : "taskkill /PID ", String.valueOf(pid));
        }
    };

    static String concatinate(final String... params) {

        final StringBuilder string_builder = new StringBuilder();
        if (params != null) {
            for (final String param : params) {
                string_builder.append(" ");
                string_builder.append(param);
            }
        }
        return string_builder.toString();
    }
}
