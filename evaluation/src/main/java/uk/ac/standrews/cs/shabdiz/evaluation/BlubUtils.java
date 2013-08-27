package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public final class BlubUtils {

    private static final int MAX_BLUB_NODES = 48;
    private static final SSHPublicKeyCredentials INTERNAL_PUBLIC_KEY_CREDENTIALS = SSHPublicKeyCredentials.getDefaultRSACredentials(new char[0]);

    public static Set<Host> getHosts(int max) throws IOException {

        vaildateHostCount(max);
        final Set<Host> hosts = new HashSet<Host>();
        for (int i = 0; i < max; i++) {

            SSHHost host = new SSHHost("compute-0-" + i, INTERNAL_PUBLIC_KEY_CREDENTIALS);
            hosts.add(host);
        }
        return hosts;
    }

    private static void vaildateHostCount(final int max) {

        if (max > MAX_BLUB_NODES || max < 1) { throw new IllegalArgumentException("host count must be between 1 to " + MAX_BLUB_NODES + " (inclusive)"); }
    }
}
