package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.platform.Platforms;
import uk.ac.standrews.cs.shabdiz.platform.SimplePlatform;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.Input;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.test.category.Ignore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@Category(Ignore.class)
public class SSHjHostTest extends Bootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHjHostTest.class);
    private static SSHjHost host;
    private static String host_name;

    @BeforeClass
    public static void setUp() throws Exception {

        assumeTrue(Platforms.getCurrentUser().equals("masih"));
        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        key_provider.init(new File(SSHCredentials.DEFAULT_SSH_HOME, "id_rsa"), new PasswordFinder() {

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

        //        final AuthMethod authentication = new AuthPassword(new PasswordFinder() {
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

        //        host = new SSHjHost(InetAddress.getLocalHost().getHostName(), authentication);
        host_name = "masih.host.cs.st-andrews.ac.uk";
        host = new SSHjHost(host_name, authentication);
        //                host = new LocalHost();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        host.close();
    }

    @Test
    public void testJavaProcessBuilding() throws Exception {

        AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
        builder.addCurrentJVMClasspath();
        builder.setMainClass(SSHjHostTest.class);
        final Process start = builder.start(host);
        final Properties properties = Bootstrap.readProperties(SSHjHostTest.class, start, new Duration(50, TimeUnit.SECONDS));
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
        }
    }

    @Test
    public void testFileUploadDownload() throws Exception {

        final String content = "some_text";
        final File temp_file = File.createTempFile("text", ".text");
        final File destination = new File(temp_file.getAbsolutePath() + ".copy");
        temp_file.deleteOnExit();
        destination.deleteOnExit();

        FileUtils.writeStringToFile(temp_file, content);
        assertTrue(temp_file.exists());
        assertFalse(destination.exists());

        final Platform platform = host.getPlatform();
        final String host_tmp = platform.getTempDirectory();

        host.upload(temp_file, host_tmp);
        final String tmp_file_path_on_host = host_tmp + platform.getSeparator() + temp_file.getName();
        final String exists_command = Commands.EXISTS.get(platform, tmp_file_path_on_host);
        final Process exists_process = host.execute(exists_command);
        final String exists_process_output = ProcessUtil.awaitNormalTerminationAndGetOutput(exists_process);
        assertTrue(Boolean.valueOf(exists_process_output));

        host.download(tmp_file_path_on_host, destination);
        assertTrue(destination.exists());
        assertEquals(content, FileUtils.readFileToString(destination));
    }

    @Test
    public void testExecute() throws Exception {

        final Process uname = host.execute("uname");
        final String uname_output = ProcessUtil.awaitNormalTerminationAndGetOutput(uname);
        assertEquals(host.getPlatform().getOperatingSystemName().toLowerCase(), uname_output.toLowerCase());
    }

    @Test
    public void testExecuteWithWorkingDirectory() throws Exception {

        final String temp_directory = host.getPlatform().getTempDirectory();
        final Process pwd = host.execute(temp_directory, "pwd");
        final String pwd_output = ProcessUtil.awaitNormalTerminationAndGetOutput(pwd);
        assertEquals(temp_directory, SimplePlatform.addTailingSeparator(host.getPlatform().getSeparator(), pwd_output));
    }

    @Test
    public void testGetPlatform() throws Exception {

        //TODO improve testing of platform
        final Platform platform = host.getPlatform();
        assertNotNull(platform);
    }

    @Test(expected = TimeoutException.class)
    public void testNeverDieingProcess() throws Exception {

        final Process forever_waiting_cat = host.execute("cat");
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Integer> future_wait_for = service.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {

                return forever_waiting_cat.waitFor();
            }
        });

        try {
            future_wait_for.get(5, TimeUnit.SECONDS);
        }
        finally {
            forever_waiting_cat.destroy();
            //TODO assert that cat died as a result of destroy
        }
    }

    @Test
    public void testProcessDestruction() throws Exception {

        AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
        builder.setMainClass(NeverEndingProgram.class);
        builder.addCurrentJVMClasspath();
        builder.addMavenDependency("uk.ac.standrews.cs:stachord:2.0-SNAPSHOT");

        final Process process = builder.start(host);

        try {
            TimeoutExecutorService.awaitCompletion(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {

                    return process.waitFor();
                }
            }, 5, TimeUnit.SECONDS);

            fail("expected timeout");
        }
        catch (TimeoutException e) {
            LOGGER.debug("waitFor timed out as expected", e);
        }
        process.destroy();
        //http://quicksilver.hg.cs.st-andrews.ac.uk/shabdiz/file/f17823cbf606/src/main/java/uk/ac/standrews/cs/shabdiz/impl/RemoteSSHHost.java
        final String check_process_command = "ps -o pid,command -a | grep " + NeverEndingProgram.class.getSimpleName() + " | sed 's/^[a-z]*[ ]*\\([0-9]*\\).*/\\1/' | tr '\\n' ',' | sed 's/,$//g'";
        System.out.println(check_process_command);
        final Process check_process_exists = host.execute(check_process_command, false);
        final String pids = ProcessUtil.awaitNormalTerminationAndGetOutput(check_process_exists);
        System.out.println(pids);
        assertFalse(pids.contains(","));

    }

    @Override
    protected void deploy(final String... args) throws Exception {

        setProperty("A", "B");
    }

    public static class NeverEndingProgram {

        public static void main(String[] args) throws InterruptedException {

            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
            }

        }
    }

}
