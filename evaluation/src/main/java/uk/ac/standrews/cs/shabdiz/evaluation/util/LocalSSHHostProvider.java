package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.io.IOException;
import java.util.function.Supplier;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class LocalSSHHostProvider implements Supplier<Host> {

    private final Host local_ssh_host;

    public LocalSSHHostProvider() throws IOException {

        local_ssh_host = new SSHHost("localhost", BlubHostProvider.SSHJ_AUTH);
    }

    @Override
    public Host get() {

        return local_ssh_host;
    }

    @Override
    public String toString() {

        return "local_ssh_host";
    }
}
