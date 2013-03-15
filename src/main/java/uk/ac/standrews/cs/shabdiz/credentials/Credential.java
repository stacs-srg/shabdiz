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
package uk.ac.standrews.cs.shabdiz.credentials;

import java.io.IOException;


/**
 * Stores the credentials and authenticates a given object.
 * 
 * @param <Authenticatee> the type of the object that can by authenticated by {@link #authenticate(Object)}
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see Credentials
 */
public interface Credential<Authenticatee> {

    /**
     * Authenticates a given object.
     * 
     * @param authenticatee the object to authenticate
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void authenticate(Authenticatee authenticatee) throws IOException;

    /**
     * Gets the username.
     * 
     * @return the username
     */
    String getUsername();

}
