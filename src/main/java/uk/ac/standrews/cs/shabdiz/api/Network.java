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
package uk.ac.standrews.cs.shabdiz.api;

import java.util.Set;


/**
 * Maintains a collection of objects on multiple {@link Host hosts}.
 * 
 * @param <Member> the type of objects that are maintained by this network
 * @see Set
 */
public interface Network<Member> extends Set<Member> {

    //TODO discuss whether we need these here?
    //    void add(Host host);
    //    void killAny(Host host);
    //    void deployAll();
    //    void killAll();

    /**
     * Deploys a member on the given {@code host}, and adds it to this network.
     * 
     * @param host the host on which to deploy a member
     * @return the deployed member
     * @see add(Object)
     */
    Member deploy(Host host);

    /**
     * Terminates and removes the the given {@code member} from this network.
     * 
     * @param member the member to terminate and remove
     * @see #remove(Object)
     */
    void kill(Member member);

    /** Shuts down this network and closes any open resources. */
    void shutdown();
}
