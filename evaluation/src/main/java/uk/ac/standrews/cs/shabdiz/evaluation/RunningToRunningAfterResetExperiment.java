package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import javax.inject.Provider;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Unknwon -> Auth -> Running -> RESET_STATE -> Running
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class RunningToRunningAfterResetExperiment extends Experiment {

    private static final String TIME_TO_REACH_RUNNING_AFTER_RESET = "time_to_reach_running_after_reset";

    public RunningToRunningAfterResetExperiment(final int network_size, final Provider<Host> host_provider, final ApplicationManager manager) throws IOException {

        super(network_size, host_provider, manager);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        network.setStatusScannerEnabled(true);
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));

        network.setAutoDeployEnabled(true);
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));

        disableAllNetworkScanners();
        resetApplicationDescriptorsStatusTo(ApplicationState.UNKNOWN);
        network.setStatusScannerEnabled(true);
        final long time_to_reach_running_after_reset = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_RESET, String.valueOf(time_to_reach_running_after_reset));
    }

    private void resetApplicationDescriptorsStatusTo(final ApplicationState status) {

        for (ApplicationDescriptor descriptor : getNetwork()) {
            descriptor.setApplicationState(status);
        }
    }
}
