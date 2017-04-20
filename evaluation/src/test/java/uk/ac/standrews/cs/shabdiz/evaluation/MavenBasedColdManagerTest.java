package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.evaluation.util.LocalHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.test.category.Ignore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder.createTempDirPathByPlatform;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(Parameterized.class)
@Category(Ignore.class)
@org.junit.Ignore
public class MavenBasedColdManagerTest {

    private final Integer network_size;
    private final Supplier<Host> host_provider;
    private final ExperimentManager manager;
    private ApplicationNetwork network;
    private File test_file;

    public MavenBasedColdManagerTest(Integer network_size, Supplier<Host> host_provider, final ExperimentManager manager) {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
    }

    @Parameterized.Parameters(name = "{index} network_size: {0}, host_provider: {1}, manager: {2}")
    public static Collection<Object[]> getParameters() {

        return Combinations.generateArgumentCombinations(new Object[][]{{10}, {new LocalHostProvider()}, {ChordManager.MAVEN_BASED_COLD, EchoManager.MAVEN_BASED_COLD}});
    }

    @Before
    public void setUp() throws Exception {

        network = new ApplicationNetwork("test_network");
        populateNetwork();
        initLocalTestFile();
    }

    @Test
    public void testConfigure() throws Exception {

        for (ApplicationDescriptor descriptor : network) {
            final Host host = descriptor.getHost();
            final Platform platform = host.getPlatform();
            final String tmp_on_host = createTempDirPathByPlatform(platform);
            final String exists_command = Commands.EXISTS.get(platform, tmp_on_host);

            host.upload(test_file, tmp_on_host);
            assertTrue(Boolean.parseBoolean(ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(exists_command))));
            manager.configure(network);
            assertFalse(Boolean.parseBoolean(ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(exists_command))));
        }
    }

    @After
    public void tearDown() throws Exception {

        network.shutdown();
        FileUtils.deleteQuietly(test_file);
    }

    private void initLocalTestFile() throws IOException {

        test_file = File.createTempFile("test", ".txt");
        FileUtils.writeStringToFile(test_file, "Some text");
    }

    private void populateNetwork() {

        for (int i = 0; i < network_size; i++) {
            final Host host = host_provider.get();
            network.add(new ApplicationDescriptor(host, manager));
        }
    }
}
