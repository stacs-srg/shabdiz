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
package uk.ac.standrews.cs.shabdiz.job.util;

import java.io.Serializable;

import uk.ac.standrews.cs.shabdiz.job.JobRemote;

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
