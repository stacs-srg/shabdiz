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
package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Scanner that checks for machines that will accept an SSH connection but are not currently running the given application, i.e. that
 * are in state AUTH. For such machines an attempt is made to launch the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoDeployScanner extends AbstractConcurrentScanner {

    private static final Duration DEPLOY_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoDeployScanner.class);

    protected AutoDeployScanner(final Duration cycle_delay) {

        super(cycle_delay, DEPLOY_CHECK_TIMEOUT, false);
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        if (isDeployable(descriptor)) {
            try {
                final Object application_reference = descriptor.getApplicationManager().deploy(descriptor);
                descriptor.setApplicationReference(application_reference);
            } catch (final Exception e) {
                LOGGER.debug("auto deployment failed", e);
            }
        }
    }

    protected boolean isDeployable(final ApplicationDescriptor application_descriptor) {

        return ApplicationState.AUTH.equals(application_descriptor.getApplicationState());
    }

}
