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
package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.net.HttpURLConnection;
import java.net.URL;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

class UrlPingerManager implements ApplicationManager {

    @Override
    public ApplicationState probeApplicationState(final ApplicationDescriptor descriptor) {

        final URL target = ((UrlPingerDescriptor) descriptor).getTarget();
        System.out.println("probing state of " + target);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("GET");

            connection.connect();
            final int response_code = connection.getResponseCode();
            switch (response_code) {
                case HttpURLConnection.HTTP_OK:
                    return ApplicationState.RUNNING;
                default:
                    return ApplicationState.UNKNOWN;
            }
        }
        catch (final Exception e) {
            return ApplicationState.UNREACHABLE;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException();

    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException();

    }
}
