package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.mashti.jetson.util.CloseableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.BOOTSTRAP_HOME_NAME;
import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.BOOTSTRAP_JAR_NAME;
import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.SHABDIZ_HOME_NAME;
import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.TEMP_HOME_NAME;
import static uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap.getBootstrapJar;

/**
 * Builds Java process on hosts and resolves any dependencies using Maven.
 * The process is started by executing a bootstrap jar that constructs a maven repository of any needed dependency.
 * By default the Maven central repository and the Maven repository at the school of computer science University of St Andrews are loaded.
 * Any additional repository may be added using {@link #addMavenRepository(URL)}.
 * Dependencies are added using {@link #addMavenDependency(String, String, String)}. Please note that any child dependency of an added dependency will be downloaded automatically.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AgentBasedJavaProcessBuilder extends JavaProcessBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentBasedJavaProcessBuilder.class);
    private static final String COLON = ":";
    private static final String BOOTSTRAP_CONFIG_FILE_NAME = "bootstrap.config";
    private static final String EQUALS = "=";
    private static final String JVM_PARAM_JAVAAGENT = "-javaagent:";
    private static final String JVM_PARAM_JAR = "-jar";
    private static final boolean FORCE_LOCAL_BOOTSTRAP_JAR_RECONSTRUCTION = true;
    private final Bootstrap.BootstrapConfiguration configuration;
    private final Set<File> uploads;

    /** Initialises a new Maven managed Java process builder. */
    public AgentBasedJavaProcessBuilder() {

        configuration = new Bootstrap.BootstrapConfiguration();
        uploads = new HashSet<File>();
    }

    @Override
    public Process start(final Host host, final String... parameters) throws IOException {

        final Platform platform = host.getPlatform();
        final String bootstrap_jar = uploadBootstrapJar(host);
        final String working_directory = getWorkingDirectoryByPlatform(platform);
        uploadLocalClasspathFiles(host, working_directory);
        uploadBootstrapConfigurationFile(host, working_directory);

        final String command = assembleCommand(platform, bootstrap_jar, parameters);
        LOGGER.info("cd {}; {}", working_directory, command);
        return host.execute(working_directory, command);
    }

    private void uploadLocalClasspathFiles(final Host host, final String working_directory) throws IOException {

        host.upload(uploads, working_directory);
    }

    private String getWorkingDirectoryByPlatform(final Platform platform) {

        String working_directory = getWorkingDirectory();
        if (working_directory == null) {
            working_directory = createTempDirByPlatform(platform);
        }
        return working_directory;
    }

    private static String createTempDirByPlatform(final Platform platform) {

        final char separator = platform.getSeparator();
        return getShabdizHomeByPlatform(platform) + TEMP_HOME_NAME + separator + System.currentTimeMillis();
    }

    private void uploadBootstrapConfigurationFile(final Host host, final String working_directory) throws IOException {

        final File config_as_file = getConfigurationAsFile();
        host.upload(config_as_file, working_directory);
    }

    private File getConfigurationAsFile() throws IOException {

        final File config_file = new File(createTempDirOnLocal(), BOOTSTRAP_CONFIG_FILE_NAME);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(config_file));
            configuration.write(out);
        }
        finally {
            CloseableUtil.closeQuietly(out);
        }
        return config_file;
    }

    private static File createTempDirOnLocal() throws IOException {

        final File temp_dir = new File(Bootstrap.LOCAL_SHABDIZ_TMP_HOME, String.valueOf(System.currentTimeMillis()));
        FileUtils.forceMkdir(temp_dir);
        return temp_dir;
    }

    private static String uploadBootstrapJar(final Host host) throws IOException {

        final Platform platform = host.getPlatform();
        final String bootstrap_home = getBootstrapHomeByPlatform(platform);
        final String bootstrap_jar = getBootstrapJarByPlatform(platform);
        final boolean already_exists = existsOnHost(host, bootstrap_jar);
        if (!already_exists) {
            host.upload(getBootstrapJar(FORCE_LOCAL_BOOTSTRAP_JAR_RECONSTRUCTION), bootstrap_home);
            LOGGER.debug("uploading bootstrap.jar to {} on host {}", bootstrap_jar, host);
        }
        else {
            LOGGER.debug("skipped bootstrap.jar upload to {} on host {}; bootstrap already exists", bootstrap_jar, host);
        }
        return bootstrap_jar;
    }

    private static String getBootstrapHomeByPlatform(final Platform platform) {

        return getShabdizHomeByPlatform(platform) + BOOTSTRAP_HOME_NAME + platform.getSeparator();
    }

    private static String getShabdizHomeByPlatform(final Platform platform) {

        return platform.getTempDirectory() + SHABDIZ_HOME_NAME + platform.getSeparator();
    }

    private static String getBootstrapJarByPlatform(final Platform platform) {

        return getBootstrapHomeByPlatform(platform) + BOOTSTRAP_JAR_NAME;
    }

    private static boolean existsOnHost(final Host host, final String file) throws IOException {

        final String exists_command = Commands.EXISTS.get(host.getPlatform(), file);
        final Process exists_process = host.execute(exists_command);
        boolean already_exists;
        try {
            final String result = ProcessUtil.awaitNormalTerminationAndGetOutput(exists_process);
            already_exists = Boolean.valueOf(result);
        }
        catch (InterruptedException e) {
            LOGGER.debug("interrupted while waiting to determine whether bootstrap.jar exists on remote host", e);
            already_exists = false;
        }
        finally {
            exists_process.destroy();
        }
        return already_exists;
    }

    private String assembleCommand(final Platform platform, final String bootstrap_jar, final String[] parameters) {

        final StringBuilder command = new StringBuilder();
        appendJavaBinPath(command, platform);
        appendBootstrpAgent(command, bootstrap_jar);
        appendJVMArguments(command);
        appendBootstrapJar(command, bootstrap_jar);
        appendCommandLineArguments(command, platform, parameters);
        return command.toString();
    }

    private static void appendBootstrpAgent(final StringBuilder command, final String bootstrap_jar) {

        command.append(JVM_PARAM_JAVAAGENT);
        command.append(bootstrap_jar);
        command.append(EQUALS);
        command.append(BOOTSTRAP_CONFIG_FILE_NAME);
        command.append(SPACE);
    }

    private static void appendBootstrapJar(final StringBuilder command, final String bootstrap_jar) {

        command.append(JVM_PARAM_JAR);
        command.append(SPACE);
        command.append(bootstrap_jar);
        command.append(SPACE);
    }

    @Override
    public void setMainClass(final Class<?> main_class) {

        super.setMainClass(main_class);
        configuration.setApplicationBootstrapClass(main_class);
    }

    @Override
    public void setMainClass(final String main_class) {

        super.setMainClass(main_class);
        configuration.setApplicationBootstrapClassName(main_class);
    }

    /**
     * Adds a {@link URL} of a classpath resource to the list of this process builder's classpath entries.
     * The given {@code url} is expected to be accessible by any host on which a process is {@link #start(Host, String...) started}.
     *
     * @param url the url of a resource that must be available in the classpath of started processes
     */
    public boolean addURL(URL url) {

        return configuration.addClassPathURL(url);
    }

    /**
     * Adds a file on a host to the list of this process builder's classpath entries.
     * The given {@code file} is expected to exist on any host on which a process is {@link #start(Host, String...) started}.
     *
     * @param file the path to a resource that must be available in the classpath of started processes and exists on remote hosts
     */
    public boolean addRemoteFile(String file) {

        return configuration.addClassPathFile(file);
    }

    /**
     * Adds a local file to the list of this process builder's classpath entries.
     * The given {@code file} is expected to exist on the local machine and is uploaded to the working directory of any host on which a process is {@link #start(Host, String...) started}.
     *
     * @param file the local file to add to the classpath of any process started by this process builder
     */
    public boolean addFile(File file) {

        return uploads.add(file);
    }

    /**
     * Adds a Maven repository to the list of repositories.
     * The given URL is assumed to be accessible by hosts on which Java processes to be started.
     *
     * @param repository_url the url of the Maven repository
     */
    public boolean addMavenRepository(final URL repository_url) {

        return configuration.addMavenRepository(repository_url);
    }

    /**
     * Adds a Maven dependency to the list of dependencies.
     * Any dependencies of the given dependency are resolved automatically.
     *
     * @param group_id the Maven dependency group ID
     * @param artifact_id the Maven dependency artifact ID
     * @param version the Maven dependency version
     */
    public boolean addMavenDependency(final String group_id, final String artifact_id, final String version) {

        return addMavenDependency(group_id, artifact_id, version, null);
    }

    /**
     * Adds a Maven dependency to the list of dependencies.
     * Any dependencies of the given dependency are resolved automatically.
     *
     * @param group_id the Maven dependency group ID
     * @param artifact_id the Maven dependency artifact ID
     * @param version the Maven dependency version
     */
    public boolean addMavenDependency(final String group_id, final String artifact_id, final String version, final String classifier) {

        final String artifact_coordinate = toCoordinate(group_id, artifact_id, version, classifier);
        return configuration.addMavenArtifact(artifact_coordinate);
    }

    static String toCoordinate(final String group_id, final String artifact_id, final String version, String classifier) {

        final StringBuilder builder = new StringBuilder();
        builder.append(group_id).append(COLON).append(artifact_id);
        if (classifier != null) {
            builder.append(COLON).append("jar").append(COLON).append(classifier);
        }
        builder.append(COLON).append(version);
        return builder.toString();
    }
}