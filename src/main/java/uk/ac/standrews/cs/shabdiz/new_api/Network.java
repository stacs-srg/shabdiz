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

import java.util.Set;

import uk.ac.standrews.cs.shabdiz.api.Host;

/**
 * Presents a network of members and provides methods to manage each member on multiple hosts.
 * 
 * @param <Member> the type of members
 * @see Set
 */
public interface Network<Member> extends Set<Member> {

    Member deploy(Host host);

    void kill(Member member);

    void shutdown();

    //TODO this about whether we really need these here?
    //    void add(Host host);
    //    void killAny(Host host);
    //    void deployAll();
    //    void killAll();
}
