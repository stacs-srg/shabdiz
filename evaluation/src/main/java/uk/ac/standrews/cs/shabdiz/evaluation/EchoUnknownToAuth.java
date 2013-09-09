package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import javax.inject.Provider;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.host.Host;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class EchoUnknownToAuth extends UnknownToAuthExperiment {

    public EchoUnknownToAuth(final int network_size, final Provider<Host> host_provider, final ApplicationManager manager) throws IOException {

        super(network_size, host_provider, manager);
    }

    @Override
    protected void addHostToNetwork(final Host host) {

    }
}
