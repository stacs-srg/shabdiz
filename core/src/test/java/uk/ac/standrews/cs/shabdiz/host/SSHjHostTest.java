package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.net.InetAddress;
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
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.Input;
import uk.ac.standrews.cs.shabdiz.util.JarUtils;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.test.category.Ignore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@Category(Ignore.class)
public class SSHjHostTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSHjHostTest.class);
    private static SSHjHost host;

    public static void main(String[] args) throws InterruptedException {

        while (!Thread.currentThread().isInterrupted())
            Thread.sleep(1000);
    }

    @BeforeClass
    public static void setUp() throws Exception {

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
        host = new SSHjHost(InetAddress.getLocalHost().getHostName(), authentication);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        host.close();
    }

    @Test
    public void testUpload() throws Exception {

        final String content = "some_text";
        final File temp_file = File.createTempFile("text", ".text");
        final File destination = new File(temp_file.getAbsolutePath() + ".copy");
        temp_file.deleteOnExit();
        destination.deleteOnExit();

        FileUtils.writeStringToFile(temp_file, content);
        assertTrue(temp_file.exists());
        assertFalse(destination.exists());

        host.upload(temp_file, destination.getAbsolutePath());
        assertTrue(destination.exists());
        assertEquals(content, FileUtils.readFileToString(destination));
    }

    @Test
    public void testDownload() throws Exception {

        final String content = "some_text";
        final File temp_file = File.createTempFile("text", ".text");
        final File destination = new File(temp_file.getAbsolutePath() + ".copy");
        temp_file.deleteOnExit();
        destination.deleteOnExit();

        FileUtils.writeStringToFile(temp_file, content);
        assertTrue(temp_file.exists());
        assertFalse(destination.exists());

        host.download(temp_file.getAbsolutePath(), destination);
        assertTrue(destination.exists());
        assertEquals(content, FileUtils.readFileToString(destination));
    }

    @Test
    public void testExecute() throws Exception {

        final Process pwd = host.execute("pwd");
        final String pwd_output = ProcessUtil.awaitNormalTerminationAndGetOutput(pwd);
        assertEquals(System.getProperty("user.home"), pwd_output);
    }

    @Test
    public void testExecuteWithWorkingDirectory() throws Exception {

        final String temp_directory = FileUtils.getTempDirectory().getAbsolutePath();
        final Process pwd = host.execute(temp_directory, "pwd");
        final String pwd_output = ProcessUtil.awaitNormalTerminationAndGetOutput(pwd);
        assertEquals(temp_directory, pwd_output);
    }

    @Test
    public void testGetPlatform() throws Exception {

        //TODO improve testing of platform
        final Platform platform = host.getPlatform();
        assertNotNull(platform);
        assertEquals(File.separatorChar, platform.getSeparator());
        assertEquals(File.pathSeparatorChar, platform.getPathSeparator());
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
    public void testProcessDistruction() throws Exception {

        final File jar = File.createTempFile("test", ".jar");
        jar.deleteOnExit();
        JarUtils.currentClasspathToExecutableJar(jar, SSHjHostTest.class);
        final String command = "java -jar " + jar.getAbsolutePath();
        final Process process = host.execute(command);

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
        final Process check_process_exists = host.execute("ps -o pid,command -ax | grep '\\" + command + "\\E' | sed 's/^[a-z]*[ ]*\\([0-9]*\\).*/\\1/' | tr '\\n' ',' | sed 's/,$//g'", false);
        final String pids = ProcessUtil.awaitNormalTerminationAndGetOutput(check_process_exists);
        assertFalse(pids.contains(","));

    }
}
