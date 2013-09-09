package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import javax.inject.Provider;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class UnknownToAuthExperiment extends Experiment {

    protected final Timer state_timer;

    public UnknownToAuthExperiment(int network_size, final Provider<Host> host_provider, final ApplicationManager manager) throws IOException {

        super(network_size, host_provider, manager);
        state_timer = new Timer();
    }

    @Override
    public void setUp() throws Exception {

        registerMetric("state_timer", state_timer);
        disableAllNetworkScanners();
        populateNetwork();
        super.setUp();
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.AUTH);
        time.stop();
    }
}
