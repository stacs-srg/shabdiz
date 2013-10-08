package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.io.IOException;
import javax.inject.Provider;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPasswordCredentials;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class LocalSSHHostProvider implements Provider<Host> {

    private final SSHHost local_ssh_host;

    public LocalSSHHostProvider() throws IOException {

        local_ssh_host = new SSHHost("localhost", new SSHPasswordCredentials(Input.readPassword("password: ")));
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
