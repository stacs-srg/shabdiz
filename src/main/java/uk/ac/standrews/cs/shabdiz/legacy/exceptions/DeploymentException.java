/*
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
package uk.ac.standrews.cs.shabdiz.legacy.exceptions;

/**
 * Indicates an unknown platform.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class DeploymentException extends Exception {

    private static final long serialVersionUID = -3375605576142248427L;

    /**
     * Creates an exception with a given message.
     * 
     * @param message the message
     */
    public DeploymentException(final String message) {

        super(message);
    }

    /**
     * Creates an exception with a given message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public DeploymentException(final String message, final Throwable cause) {

        super(message, cause);
    }
}
