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

import uk.ac.standrews.cs.nds.util.Duration;

/**
 * Scans a {@link ApplicationNetwork network} for an application-specific change.
 * 
 * @param <T> the type of {@link ApplicationDescriptor applications} to scan
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface Scanner<T extends ApplicationDescriptor> {

    /**
     * Scans the {@link ApplicationNetwork network} for a change.
     */
    void scan();

    /**
     * Gets the application network to be scanned by this scanner.
     * 
     * @return the application network to be scanned by this scanner
     */
    ApplicationNetwork<T> getApplicationNetwork();

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
