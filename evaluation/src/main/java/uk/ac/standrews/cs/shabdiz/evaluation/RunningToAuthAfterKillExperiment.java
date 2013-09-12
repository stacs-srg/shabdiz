package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import javax.inject.Provider;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Unknwon -> Auth -> Running -> Auth
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningToAuthAfterKillExperiment extends Experiment {

    private static final String TIME_TO_REACH_AUTH_FROM_RUNNING = "time_to_reach_auth_from_running";

    public RunningToAuthAfterKillExperiment(int network_size, final Provider<Host> host_provider, final ApplicationManager manager) throws IOException {

        super(network_size, host_provider, manager);
    }

    @Override
    public void doExperiment() throws Exception {

        network.setStatusScannerEnabled(true);
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));

        network.setAutoDeployEnabled(true);
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));

        network.setAutoDeployEnabled(false);
        network.killAll();
        final long time_to_reach_auth_from_running = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH_FROM_RUNNING, String.valueOf(time_to_reach_auth_from_running));
    }
}
