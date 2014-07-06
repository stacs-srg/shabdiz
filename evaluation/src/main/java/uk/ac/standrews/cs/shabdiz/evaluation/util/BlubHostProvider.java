package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.platform.UnixPlatform;
import uk.ac.standrews.cs.shabdiz.util.ArrayUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BlubHostProvider implements Supplier<Host> {

    public static final UnixPlatform LINUX_PLATFORM = new UnixPlatform("Linux");
    public static final AuthPublickey SSHJ_AUTH;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlubHostProvider.class);
    private static final int MAX_INDEX = 47;
    private static final int MAX_BLUB_NODES_COUNT = MAX_INDEX + 1;
    private static final String BLUB_NODE_HOST_NAME_PREFIX = "compute-0-";
    private static final Integer[] EXCLUDED_INDICES = {};
    static {
        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        key_provider.init(new File(System.getProperty("user.home") + File.separator + ".ssh", "id_rsa"));
        SSHJ_AUTH = new AuthPublickey(key_provider);
    }
    private final AtomicInteger next_host_index;

    public BlubHostProvider() {

        next_host_index = new AtomicInteger();
    }

    /**
     * Gets the next blub node in the list of available blub nodes by this provider.
     * This provider constructs maximum of {@value #MAX_BLUB_NODES_COUNT} hosts.
     *
     * @return a blub node
     *         throws NoSuchElementException if there are no more hosts left to construct
     *         throws RuntimeException if construction of a host fails typically due to an IO error
     */
    @Override
    public Host get() {

        final int host_index = getNextHostIndex();
        try {
            return getHostByIndex(host_index);
        }
        catch (IOException e) {
            LOGGER.error("failed to construct host with index: " + host_index, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {

        return "Blub";
    }

    private int getNextHostIndex() {

        int host_index;
        do {
            host_index = next_host_index.getAndIncrement();
        }
        while (isHostIndexExcluded(host_index));
        return host_index;
    }

    private boolean isHostIndexExcluded(final int host_index) {

        return ArrayUtil.contains(host_index, EXCLUDED_INDICES);
    }

    private static Host getHostByIndex(final int host_index) throws IOException {

        if (!isOutOfRange(host_index)) {
            final String host_name = BLUB_NODE_HOST_NAME_PREFIX + host_index;
            return new SSHHost(host_name, SSHClient.DEFAULT_PORT, SSHJ_AUTH, LINUX_PLATFORM);
            //            return new SSHHost(host_name, INTERNAL_PUBLIC_KEY_CREDENTIALS);
        }
        throw new NoSuchElementException("cannot instantiate any more hosts; maximum blub hosts available is " + MAX_BLUB_NODES_COUNT);
    }

    private static boolean isOutOfRange(final int index) {

        return index < 0 || index > MAX_INDEX;
    }
}
