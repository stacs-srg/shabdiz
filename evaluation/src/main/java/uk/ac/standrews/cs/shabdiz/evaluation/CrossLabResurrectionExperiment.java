package uk.ac.standrews.cs.shabdiz.evaluation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.inject.Provider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHCredentials;
import uk.ac.standrews.cs.shabdiz.host.SSHjHost;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.Input;

import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ECHO_FILE_WARM_MANAGERS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_50;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_1_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_5_MINUTE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCHEDULER_THREAD_POOL_SIZE_10;

/**
 * Investigates how long it takes for a network to reach {@link ApplicationState#RUNNING} state after a portion of application instances are killed.
 * For a given network size, a host provider, a manager and a kill portion:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - disables auto-deploy
 * - kills a portion of network
 * - re-enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CrossLabResurrectionExperiment extends ResurrectionExperiment {

    public static final CrossLabHostProvider CROSS_LAB_HOST_PROVIDER = new CrossLabHostProvider();
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossLabResurrectionExperiment.class);

    public CrossLabResurrectionExperiment(final int network_size, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_pool_size, int concurrent_scanner_pool_size) {

        super(network_size, CROSS_LAB_HOST_PROVIDER, manager, kill_portion, scanner_interval, scanner_timeout, scheduler_pool_size, concurrent_scanner_pool_size);
    }

    @Parameterized.Parameters(name = "network_{0}_{1}_{2}_kill_{3}_interval_{4}_timeout_{5}_sch_pool_{6}_conc_pool_{7}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{new Integer[]{CROSS_LAB_HOST_PROVIDER.hosts.size()}, ECHO_FILE_WARM_MANAGERS, KILL_PORTION_50, SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10,
                        CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});
        for (int i = 0; i < 1; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Override
    public void tearDown() {

        try {
            super.tearDown();
        }
        finally {
            for (Host h : CROSS_LAB_HOST_PROVIDER.hosts) {
                try {
                    System.out.println("killing all java on " + h);
                    final Process execute = h.execute("killall java");
                    execute.waitFor();
                    execute.destroy();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class CrossLabHostProvider implements Provider<Host> {

        private static final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        private static final AuthMethod authentication = BlubHostProvider.SSHJ_AUTH;//new AuthPublickey(key_provider);
        //        private static final AuthMethod authentication = new AuthPassword(new CachedPasswordFinder());
        private static final String RSA_PRIVATE_KEY_FILE_NAME = "id_rsa";
        private static final File RSA_PRIVATE_KEY_FILE = new File(SSHCredentials.DEFAULT_SSH_HOME, RSA_PRIVATE_KEY_FILE_NAME);
        static {
            key_provider.init(RSA_PRIVATE_KEY_FILE, new CachedPasswordFinder());
        }
        private static final List<Host> hosts;
        static {
            hosts = new ArrayList<Host>();
            final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
            try {
                BlubHostProvider blub = new BlubHostProvider();
                for (int i = 0; i < 39; i++) {
                    hosts.add(blub.get());
                }
                List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>();

                for (int i = 1; i <= 80; i++) {
                    final Integer ii = i;
                    final ListenableFuture<Void> submit = service.submit(new Callable<Void>() {

                        @Override
                        public Void call() throws Exception {

                            final String host_name = "mac1-0" + String.format("%02d", ii) + "-m.cs.st-andrews.ac.uk";
                            boolean reachable;
                            try {
                                reachable = InetAddress.getByName(host_name).isReachable(5000);
                            }
                            catch (IOException e) {
                                reachable = false;
                            }

                            System.out.println(host_name + " rachable?  " + reachable);
                            if (reachable) {
                                final SSHjHost hjHost = new SSHjHost(host_name, authentication);
                                synchronized (hosts) {
                                    hosts.add(hjHost);
                                }
                            }
                            return null;
                        }
                    });
                    futures.add(submit);
                }

                Futures.allAsList(futures).get();

                hosts.add(new SSHjHost("pc2-036-l.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-039-l.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-041-l.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-042-l.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-043-l.cs.st-andrews.ac.uk", authentication));
            }
            catch (Exception e) {
                LOGGER.error("failed to add predefined hosts", e);
                throw new RuntimeException(e);
            }
            finally {
                service.shutdownNow();
            }
        }
        private int index;

        @Override
        public Host get() {

            return hosts.get(index++);
        }

        private static class CachedPasswordFinder implements PasswordFinder {

            private char[] passphrase;

            public CachedPasswordFinder() {

            }

            @Override
            public synchronized char[] reqPassword(final Resource<?> resource) {

                if (passphrase == null) {
                    passphrase = Input.readPassword("Please enter local private key password: ");
                }

                return passphrase;
            }

            @Override
            public boolean shouldRetry(final Resource<?> resource) {

                return false;
            }
        }
    }
}
