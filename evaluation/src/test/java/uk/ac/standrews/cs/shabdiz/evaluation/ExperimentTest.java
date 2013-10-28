package uk.ac.standrews.cs.shabdiz.evaluation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(ExperiementRunner.class)
public class ExperimentTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTest.class);

    //    private static SSHjHost host;
    //
    //    @BeforeClass
    //    public static void setUp() throws Exception {
    //
    //        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
    //        key_provider.init(new File(SSHCredentials.DEFAULT_SSH_HOME, "id_rsa"), new PasswordFinder() {
    //
    //            @Override
    //            public char[] reqPassword(final Resource<?> resource) {
    //
    //                return Input.readPassword("local private key password: ");
    //            }
    //
    //            @Override
    //            public boolean shouldRetry(final Resource<?> resource) {
    //
    //                return false;
    //            }
    //        });
    //
    //        final AuthMethod authentication = new AuthPublickey(key_provider);
    //        //        host = new SSHjHost(InetAddress.getLocalHost().getHostName(), authentication);
    ////        host = new SSHjHost("blub.cs.st-andrews.ac.uk", authentication);
    //        host = new SSHjHost("masih.host.cs.st-andrews.ac.uk", authentication);
    //    }
    //
    //    @AfterClass
    //    public static void tearDown() throws Exception {
    //        host.close();
    //    }
    //
    //    @Test
    //    public void testJavaProcessBuilding() throws Exception {
    //
    //        final ChordManager.MavenBasedWarm manager = ChordManager.MAVEN_BASED_WARM;
    //        final ApplicationNetwork network = new ApplicationNetwork("test");
    //        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);
    //        network.add(descriptor);
    //        network.setScanEnabled(false);
    //        manager.configure(network);
    //        manager.deploy(descriptor);
    //        manager.kill(descriptor);
    //    }

    @Test
    public void testName() throws Exception {

        fail();

    }
}
