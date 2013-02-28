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
package uk.ac.standrews.cs.shabdiz.new_api;

import uk.ac.standrews.cs.shabdiz.api.Host;

/**
 * Maintains a set of {@link ApplicationDescriptor}s.
 * This class provides methods for managing a set of managed application instances across multiple {@link Host}s.
 * 
 * @param <T> the type of {@link ApplicationDescriptor}s that are maintained by this network
 * @see Network
 */
public interface ApplicationNetwork<T extends ApplicationDescriptor> extends Network<T> {

    String getApplicationName();

    void awaitUniformState(State... states) throws InterruptedException;

    boolean addScanner(Scanner<T> scanner);

    boolean removeScanner(Scanner<T> scanner);

    void setScanEnabled(boolean enabled);

    //FIXME add auto deploy and auto kill
}
