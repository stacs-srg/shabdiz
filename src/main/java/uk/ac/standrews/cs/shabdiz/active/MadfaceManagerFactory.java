/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
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

package uk.ac.standrews.cs.shabdiz.active;

import uk.ac.standrews.cs.shabdiz.active.interfaces.IMadfaceManager;

/**
 * Madface manager factory, hiding implementation class from caller.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class MadfaceManagerFactory {

    private MadfaceManagerFactory() {

    }

    /**
     * Returns a Madface manager.
     * @return a Madface manager.
     */
    public static IMadfaceManager makeMadfaceManager() {

        return new MadfaceManager();
    }

    /**
     * Returns a Madface manager.
     * @return a Madface manager.
     */
    public static IMadfaceManager makeMadfaceManager(final Configuration configuration) {

        return new MadfaceManager(configuration);
    }
}
