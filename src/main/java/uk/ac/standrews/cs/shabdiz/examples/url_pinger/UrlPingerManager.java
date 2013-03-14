package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.net.HttpURLConnection;
import java.net.URL;

import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;

class UrlPingerManager implements ApplicationManager {

    @Override
    public void updateApplicationState(final ApplicationDescriptor descriptor) {
        final ApplicationState state = getState(descriptor);
        descriptor.setCachedApplicationState(state);

    }

    private ApplicationState getState(final ApplicationDescriptor descriptor) {

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
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException();

    }
}
