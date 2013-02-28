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
package uk.ac.standrews.cs.shabdiz.zold.util;

import java.io.Serializable;

import uk.ac.standrews.cs.shabdiz.zold.api.JobRemote;

/**
 * Similar to the class {@link Void}, with the difference that this class is {@link Serializable}.
 * Since {@link Void} is not {@link Serializable}, this class may be used as the return type of a <code>void</code> {@link JobRemote}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class SerializableVoid implements Serializable {

    private static final long serialVersionUID = -6017357297629660338L;

    private SerializableVoid() {

    }
}
