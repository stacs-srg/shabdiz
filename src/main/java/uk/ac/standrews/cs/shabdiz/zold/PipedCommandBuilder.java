/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.zold;

/**
 * Constructs a shell command pipeline.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class PipedCommandBuilder {

    private static final String PIPE = "|";
    private final StringBuilder builder;

    PipedCommandBuilder(final StringBuilder builder) {

        this.builder = builder;
    }

    /**
     * Appends a given command.
     * @param command the command
     */
    public void append(final String command) {

        if (builder.length() > 0) {
            builder.append(PIPE);
        }
        builder.append(command);
    }
}
