package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/**
 * Builds Java process on hosts and resolves any dependencies using Maven.
 * The process is started by executing a bootstrap jar that constructs a maven repository of any needed dependency.
 * By default the Maven central repository and the Maven repository at the school of computer science University of St Andrews are loaded.
 * Any additional repository may be added using {@link #addMavenRepository(URL)}.
 * Dependencies are added using {@link #addMavenDependency(String, String, String)}. Please note that any child dependency of an added dependency will be downloaded automatically.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class MavenManagedJavaProcessBuilder extends JavaProcessBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenManagedJavaProcessBuilder.class);
    private static final String BOOTSTRAP_JAR_NAME = "bootstrap.jar";
    private static final File BOOTSTRAP_JAR = new File(JavaBootstrap.LOCAL_BOOTSTRAP_HOME, BOOTSTRAP_JAR_NAME);
    private static final String COLON = ":";
    private final List<String> dependency_coordinates;
    private final List<String> maven_repositories;

    /** Initialises a new Maven managed Java process builder. */
    public MavenManagedJavaProcessBuilder() {

        dependency_coordinates = new ArrayList<String>();
        maven_repositories = new ArrayList<String>();
    }

    @Override
    public Process start(final Host host, final String... parameters) throws IOException {

        final Platform platform = host.getPlatform();
        final String host_bootstrap_home = platform.getTempDirectory() + JavaBootstrap.SHABDIZ_HOME_NAME + platform.getSeparator() + JavaBootstrap.BOOTSTRAP_HOME_NAME;
        uploadBootstrapJar(host, host_bootstrap_home);
        final String command = assembleCommand(platform, host_bootstrap_home, parameters);
        LOGGER.info("executing {}", command);
        return host.execute(getWorkingDirectory(), command);
    }

    /**
     * Adds a Maven repository to the list of repositories.
     * The given URL is assumed to be accessible by hosts on which Java processes to be started.
     *
     * @param repository_url the url of the Maven repository
     */
    public void addMavenRepository(final URL repository_url) {

        maven_repositories.add(repository_url.toString());
    }

    /**
     * Adds a Maven dependency to the list of dependencies.
     * Any dependencies of the given dependency are resolved automatically.
     *
     * @param group_id the Maven dependency group ID
     * @param artifact_id the Maven dependency artifact ID
     * @param version the Maven dependency version
     */
    public void addMavenDependency(final String group_id, final String artifact_id, final String version) {

        addMavenDependency(group_id, artifact_id, version, null);
    }

    /**
     * Adds a Maven dependency to the list of dependencies.
     * Any dependencies of the given dependency are resolved automatically.
     *
     * @param group_id the Maven dependency group ID
     * @param artifact_id the Maven dependency artifact ID
     * @param version the Maven dependency version
     */
    public void addMavenDependency(final String group_id, final String artifact_id, final String version, final String classifier) {

        dependency_coordinates.add(toCoordinate(group_id, artifact_id, version, classifier));
    }

    private void uploadBootstrapJar(final Host host, final String host_bootstrap_home) throws IOException {

        final String exist_command = Commands.EXISTS.get(host.getPlatform(), host_bootstrap_home);
        final Process bootstrap_jar_exists = host.execute(exist_command);
        boolean already_exists;
        try {
            final String result = ProcessUtil.awaitNormalTerminationAndGetOutput(bootstrap_jar_exists);
            already_exists = Boolean.valueOf(result);
        }
        catch (InterruptedException e) {
            LOGGER.debug("interrupted while waiting to determine whether bootstrap.jar exists on remote host", e);
            already_exists = false;
        }

        if (!already_exists) {
            host.upload(getBootstrapJar(), host_bootstrap_home);
            LOGGER.debug("uploading bootstrap.jar to {} on host {}", host_bootstrap_home, host);
        }
        else {
            LOGGER.debug("skipped bootstrap.jar upload to {} on host {}; bootstrap home directory already exists", host_bootstrap_home, host);
        }
    }

    private String assembleCommand(final Platform platform, final String host_bootstrap_home, final String[] parameters) {

        final StringBuilder command = new StringBuilder();
        appendJavaBinPath(command, platform);
        appendJVMArguments(command);
        appendBootstrapJar(command, platform, host_bootstrap_home);
        appendRepositories(command, platform);
        appendDependencyCoordinates(command, platform);
        appendMainClass(command);
        appendCommandLineArguments(command, platform, parameters);
        return command.toString();
    }

    @Override
    protected void appendCommandLineArguments(final StringBuilder command, final Platform platform, final String[] parameters) {

        command.append(platform.quote(Arrays.toString(parameters)));
        command.append(SPACE);
    }

    private void appendRepositories(final StringBuilder command, final Platform platform) {

        command.append(platform.quote(Arrays.toString(maven_repositories.toArray())));
        command.append(SPACE);
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

    private void appendDependencyCoordinates(final StringBuilder command, final Platform platform) {

        command.append(platform.quote(Arrays.toString(dependency_coordinates.toArray())));
        command.append(SPACE);
    }

    private void appendBootstrapJar(final StringBuilder command, final Platform platform, final String bootstrap_home) {

        final char separator = platform.getSeparator();

        command.append("-jar ");
        command.append(bootstrap_home);
        command.append(separator);
        command.append(BOOTSTRAP_JAR_NAME);
        command.append(SPACE);
    }

    private static synchronized File getBootstrapJar() throws IOException {

        return !BOOTSTRAP_JAR.isFile() ? reconstructBootstrapJar() : BOOTSTRAP_JAR;
    }

    private static File reconstructBootstrapJar() throws IOException {

        final Manifest manifest = new Manifest();
        final Attributes main_attributes = manifest.getMainAttributes();
        main_attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        main_attributes.put(Attributes.Name.MAIN_CLASS, JavaBootstrap.class.getName());
        main_attributes.put(Attributes.Name.CLASS_PATH, ".");

        FileUtils.deleteDirectory(JavaBootstrap.LOCAL_BOOTSTRAP_HOME);
        FileUtils.forceMkdir(JavaBootstrap.LOCAL_BOOTSTRAP_HOME);

        final JarOutputStream jar_stream = new JarOutputStream(new FileOutputStream(BOOTSTRAP_JAR), manifest);

        try {
            addClassToJar(JavaBootstrap.class, jar_stream);
            addClassToJar(MavenDependencyResolver.class, jar_stream);
            addClassToJar(MavenDependencyResolver.URLCollector.class, jar_stream);
            addClassToJar(DependencyResolver.class, jar_stream);
            return BOOTSTRAP_JAR;
        }
        finally {
            jar_stream.flush();
            jar_stream.close();
        }
    }

    protected static void addClassToJar(final Class<?> type, final JarOutputStream jar) throws IOException {

        final String resource_path = getResourcePath(type);
        final JarEntry entry = new JarEntry(resource_path);
        final InputStream resource_stream = getResurceInputStream(type, resource_path);
        jar.putNextEntry(entry);
        IOUtils.copy(resource_stream, jar);
        jar.closeEntry();
    }

    private static InputStream getResurceInputStream(final Class<?> type, final String resource_path) throws IOException {

        final ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        final InputStream resource_stream = class_loader.getResourceAsStream(resource_path);
        if (resource_stream != null) { return resource_stream; }
        LOGGER.error("cannot locate resource {}", type);
        throw new IOException("unable to locate resource " + type);
    }

    private static String getResourcePath(final Class<?> type) {

        return type.getName().replaceAll("\\.", "/") + ".class";
    }
}
