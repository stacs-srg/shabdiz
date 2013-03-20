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
package uk.ac.standrews.cs.shabdiz;

import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scans a {@link ApplicationNetwork network} for an application-specific change. Scanners are executed periodically.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Scanner {

    /**
     * Scans the {@link ApplicationNetwork network} for a change.
     * 
     * @param network the network
     */
    void scan(ApplicationNetwork network);

    /**
     * Gets the delay between the termination of one scan and the commencement of the next.
     * 
     * @return the delay between the termination of one scan and the commencement of the next.
     */
    Duration getCycleDelay();

    /**
     * Gets the timeout of a scan cycle.
     * 
     * @return the timeout of a scan cycle
     * @see Duration
     */
    Duration getScanTimeout();

    /**
     * Sets the policy on whether the future scans should be performed.
     * This method has no effect on an executing scan cycle.
     * 
     * @param enabled whether to perform scans.
     */
    void setEnabled(boolean enabled);

    /**
     * Checks if this scanner is enabled.
     * 
     * @return {@code true} if this scanner is enabled
     */
    boolean isEnabled();

}
