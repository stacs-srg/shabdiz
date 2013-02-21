/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active.interfaces;

import java.util.SortedSet;

import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;

/**
 * Interface implemented by application-specific global scanners checking all known hosts.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface GlobalHostScanner extends HostScanner {

    /**
     * Performs some application-specific global check of the specified hosts.
     * 
     * @param host_state_list the hosts to be checked
     */
    void check(SortedSet<HostDescriptor> host_state_list);
}
