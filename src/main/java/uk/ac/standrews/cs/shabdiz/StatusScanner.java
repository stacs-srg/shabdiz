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

import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;

/**
 * Scanner that monitors machine status. Machines are probed for the presence of a particular application, and for their willingness to accept an SSH connection with specified credentials.
 * The results of these tests are recorded in the corresponding host descriptors.
 * This scanner publishes a new latch after every cycle through the host list. This enables other scanners to synchronize their own operation with this one.
 */
public class StatusScanner<T extends ApplicationDescriptor> extends AbstractConcurrentScanner<T> {

    /** The default timeout for attempted status checks. */
    public static final Duration DEFAULT_STATUS_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    private static final boolean ENABLED_BY_DEFAULT = true;

    protected StatusScanner(final Duration cycle_delay) {

        this(cycle_delay, DEFAULT_STATUS_CHECK_TIMEOUT, ENABLED_BY_DEFAULT);
    }

    protected StatusScanner(final Duration cycle_delay, final Duration status_check_timeout, final boolean enabled) {

        super(cycle_delay, status_check_timeout, enabled);
    }

    @Override
    protected void scan(final T application_descriptor) {

        if (isEnabled()) {
            application_descriptor.getApplicationManager().updateApplicationState(application_descriptor);
        }
    }
}
