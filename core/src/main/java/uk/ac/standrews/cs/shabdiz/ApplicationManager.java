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
package uk.ac.standrews.cs.shabdiz;

/**
 * Provides a mean of deployment, termination and status probe for application instances.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ApplicationManager {

    /**
     * Attempts to investigate the {@link ApplicationState state} of the application instance that is described by the given {@code descriptor}.
     * 
     * @param descriptor the descriptor of an application instance
     * @return the state that the application instance is believed to be in
     * @see ApplicationState
     */
    ApplicationState probeState(ApplicationDescriptor descriptor);

    /**
     * Attempts to deploy a new instance of the application that is described by the given {@code descriptor}, and returns a reference to the deployed instance.
     * 
     * @param descriptor the descriptor of an application instance
     * @return a reference to the deployed application
     * @throws Exception if the attempted application deployment fails
     */
    Object deploy(ApplicationDescriptor descriptor) throws Exception;

    /**
     * Attempts to kill the application instance that is described by the given {@code descriptor}.
     * 
     * @param descriptor the descriptor of an application instance
     * @throws Exception if the attempt to kill the application fails
     */
    void kill(ApplicationDescriptor descriptor) throws Exception;
}
