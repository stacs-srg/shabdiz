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
package uk.ac.standrews.cs.shabdiz.api;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Presents a computation to be performed on a {@link Worker worker}.
 * 
 * @param <Result> the type of result returned by this job
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface JobRemote<Result extends Serializable> extends Callable<Result>, Serializable {

}
