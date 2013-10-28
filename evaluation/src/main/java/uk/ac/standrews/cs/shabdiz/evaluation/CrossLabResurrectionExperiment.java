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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHCredentials;
import uk.ac.standrews.cs.shabdiz.host.SSHjHost;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Input;

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
@RunWith(Parameterized.class)
public class CrossLabResurrectionExperiment extends ResurrectionExperiment {

    static final String KILL_PORTION = "kill_portion";
    static final Integer[] NETWORK_SIZES = new Integer[]{4};
    static final Float[] KILL_PORTIONS = {0.5F};
    static final ExperimentManager[] APPLICATION_MANAGERS = {EchoManager.MAVEN_BASED_COLD};
    static final Provider<Host>[] CROSS_LAB_HOST_PROVIDER = new Provider[]{new CrossLabHostProvider()};

    public CrossLabResurrectionExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, final float kill_portion) {

        super(network_size, host_provider, manager, kill_portion);
    }

    @Parameterized.Parameters(name = "network_size_{0}__on_{1}__{2}__kill_portion_{3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, CROSS_LAB_HOST_PROVIDER, APPLICATION_MANAGERS, KILL_PORTIONS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    static class CrossLabHostProvider implements Provider<Host> {

        static final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        private static final AuthMethod authentication = new AuthPublickey(key_provider);

        static {
            final char[] passphrase = Input.readPassword("Please enter local private key password: ");
            key_provider.init(new File(SSHCredentials.DEFAULT_SSH_HOME, "id_rsa"), new PasswordFinder() {

                @Override
                public char[] reqPassword(final Resource<?> resource) {

                    return passphrase;
                }

                @Override
                public boolean shouldRetry(final Resource<?> resource) {

                    return false;
                }
            });
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
                throw new RuntimeException(e);
            }
        }

        @Override
        public Host get() {

            return hosts.get(index++);
        }
    }
}
