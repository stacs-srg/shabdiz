package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.util.Input;
import uk.ac.standrews.cs.test.category.Ignore;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@Category(Ignore.class)
@org.junit.Ignore
public class SupervisedExperimentTest {

    private static SSHHost host;

    @BeforeClass
    public static void setUp() throws Exception {

        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        key_provider.init(new File(System.getProperty("user.home") + File.separator + ".ssh", "id_rsa"), new PasswordFinder() {

            @Override
            public char[] reqPassword(final Resource<?> resource) {

                return Input.readPassword("local private key password: ");
            }

            @Override
            public boolean shouldRetry(final Resource<?> resource) {

                return false;
            }
        });

        final AuthMethod authentication = new AuthPublickey(key_provider);
        host = new SSHHost("masih.host.cs.st-andrews.ac.uk", authentication);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        host.close();
    }

    @Test(timeout = 30000)
    public void testJavaProcessBuilding() throws Exception {

        final ChordManager.MavenBasedWarm manager = ChordManager.MAVEN_BASED_WARM;
        final ApplicationNetwork network = new ApplicationNetwork("test");
        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);
        network.add(descriptor);
        network.setScanEnabled(false);
        manager.configure(network);
        manager.deploy(descriptor);
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        manager.kill(descriptor);
    }

}
