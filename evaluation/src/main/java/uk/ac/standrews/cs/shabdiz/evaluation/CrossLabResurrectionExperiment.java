package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Provider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHCredentials;
import uk.ac.standrews.cs.shabdiz.host.SSHjHost;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.Input;

import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_KILL_PORTIONS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_MANAGERS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_NETWORK_SIZES;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPETITIONS;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossLabResurrectionExperiment.class);

    public CrossLabResurrectionExperiment(final int network_size, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_pool_size, int concurrent_scanner_pool_size) {

        super(network_size, new CrossLabHostProvider(), manager, kill_portion, scanner_interval, scanner_timeout, scheduler_pool_size, concurrent_scanner_pool_size);
    }

    @Parameterized.Parameters(name = "network_{0}_{1}_{2}_kill_{3}_interval_{4}_timeout_{5}_sch_pool_{6}_conc_pool_{7}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{ALL_NETWORK_SIZES, ALL_MANAGERS, ALL_KILL_PORTIONS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    static class CrossLabHostProvider implements Provider<Host> {

        private static final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        private static final AuthMethod authentication = new AuthPublickey(key_provider);
        private static final String RSA_PRIVATE_KEY_FILE_NAME = "id_rsa";
        private static final File RSA_PRIVATE_KEY_FILE = new File(SSHCredentials.DEFAULT_SSH_HOME, RSA_PRIVATE_KEY_FILE_NAME);
        static {
            key_provider.init(RSA_PRIVATE_KEY_FILE, new CachedPasswordFinder());
        }
        private final List<Host> hosts;
        private int index;

        CrossLabHostProvider() {

            hosts = new ArrayList<Host>();
            try {
                hosts.add(new SSHjHost("mac1-001-m.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("mac1-002-m.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-037-l.cs.st-andrews.ac.uk", authentication));
                hosts.add(new SSHjHost("pc2-042-l.cs.st-andrews.ac.uk", authentication));
            }
            catch (IOException e) {
                LOGGER.error("failed to add predefined hosts", e);
                throw new RuntimeException(e);
            }
        }

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
