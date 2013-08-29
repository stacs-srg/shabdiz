package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class RunningToRunningExperiment extends UnknownToAuthExperiment {

    public RunningToRunningExperiment(final int network_size) throws IOException {

        super(network_size);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time_to_reach_running = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().setAutoDeployEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time_to_reach_running.stop();

        disableAllNetworkScanners();
        resetApplicationDescriptorsStatusTo(ApplicationState.UNKNOWN);
        final Timer.Time time_to_reach_auth = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time_to_reach_auth.stop();
    }

    private void resetApplicationDescriptorsStatusTo(final ApplicationState status) {

        for (ApplicationDescriptor descriptor : getNetwork()) {
            descriptor.setApplicationState(status);
        }
    }
}
