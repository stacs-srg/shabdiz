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
package uk.ac.standrews.cs.shabdiz;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;

/**
 * Scanner that checks for machines that will accept an SSH connection but are not currently running the given application, i.e. that
 * are in state AUTH. For such machines an attempt is made to launch the application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoDeployScanner<T extends ApplicationDescriptor> extends AbstractConcurrentScanner<T> {

    private static final Duration DEPLOY_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    protected AutoDeployScanner(final Duration cycle_delay) {

        super(cycle_delay, DEPLOY_CHECK_TIMEOUT, false);
    }

    protected boolean isDeployable(final ApplicationDescriptor application_descriptor) {

        return ApplicationState.AUTH.equals(application_descriptor.getApplicationState());
    }

    @Override
    protected void scan(final T application_descriptor) {

        if (isEnabled() && isDeployable(application_descriptor)) {
            try {
                getApplicationNetwork().deploy(application_descriptor);
            }
            catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (final TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
