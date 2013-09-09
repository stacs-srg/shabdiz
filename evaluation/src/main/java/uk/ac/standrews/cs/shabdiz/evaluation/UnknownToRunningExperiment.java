package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import javax.inject.Provider;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class UnknownToRunningExperiment extends UnknownToAuthExperiment {

    public UnknownToRunningExperiment(final int network_size, final Provider<Host> host_provider, final ApplicationManager manager) throws IOException {

        super(network_size, host_provider, manager);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().setAutoDeployEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time.stop();
    }
}
