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
package uk.ac.standrews.cs.nds.madface.interfaces;

/**
 * Interface implemented by objects processing output stream from remote process.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface IStreamProcessor {

    /**
     * Processes a byte output by the process.
     *
     * @param byte_value the byte to be processed
     * @return true if the byte should then be passed on for further processing
     */
    boolean processByte(int byte_value);
}