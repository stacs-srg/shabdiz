package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenManagedJavaProcessBuilder extends BaseJavaProcessBuilder {

    private static final String BOOTSTRAP_JAR_NAME = "bootstrap.jar";
    private static final File BOOTSTRAP_JAR = new File(JavaBootstrap.LOCAL_BOOTSTRAP_HOME, BOOTSTRAP_JAR_NAME);
    private static String COLON = ":";
    private final List<String> dependency_coordinates;
    private final List<String> maven_repositories;

    public MavenManagedJavaProcessBuilder() {

        dependency_coordinates = new ArrayList<String>();
        maven_repositories = new ArrayList<String>();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        MavenManagedJavaProcessBuilder builder = new MavenManagedJavaProcessBuilder();
        builder.setMainClass("uk.ac.standrews.cs.trombone.servers.NodeServer");
        builder.addMavenDependency("uk.ac.standrews.cs", "trombone", "2.0.1-SNAPSHOT");
        final Process process = builder.start(new LocalHost());
        ProcessUtil.awaitNormalTerminationAndGetOutput(process);
    }

    @Override
    public Process start(final Host host) throws IOException {

        final Platform platform = host.getPlatform();
        final String host_bootstrap_home = platform.getTempDirectory() + JavaBootstrap.SHABDIZ_HOME_NAME + platform.getSeparator() + JavaBootstrap.BOOTSTRAP_HOME_NAME;

        host.upload(getBootstrapJar(), host_bootstrap_home);

        final StringBuilder command = new StringBuilder();
        appendJavaBinPath(command, platform);
        appendJVMArguments(command);
        appendBootstrapJar(command, platform, host_bootstrap_home);
        appendRepositories(command, platform);
        appendDependencyCoordinates(command, platform);
        appendMainClass(command);
        appendCommandLineArguments(command, platform);

        return host.execute(getWorkingDirectory(), command.toString());
    }

    @Override
    protected void appendCommandLineArguments(final StringBuilder command, final Platform platform) {
        command.append(platform.quote(Arrays.toString(getCommandLineArguments().toArray())));
        command.append(SPACE);
    }

    public void addMavenRepository(URL repository_url) {
        maven_repositories.add(repository_url.toString());
    }

    public void addMavenDependency(String group_id, String artifact_id, String version) {
        dependency_coordinates.add(toCoordinate(group_id, artifact_id, version));
    }

    private void appendRepositories(final StringBuilder command, final Platform platform) {
        command.append(platform.quote(Arrays.toString(maven_repositories.toArray())));
        command.append(SPACE);
    }

    private static String toCoordinate(String group_id, String artifact_id, String version) {

        return new StringBuilder().append(group_id).append(COLON).append(artifact_id).append(COLON).append(version).toString();
    }

    private void appendDependencyCoordinates(final StringBuilder command, final Platform platform) {

        command.append(platform.quote(Arrays.toString(dependency_coordinates.toArray())));
        command.append(SPACE);
    }

    private void appendBootstrapJar(final StringBuilder command, final Platform platform, String bootstrap_home) {
        final char separator = platform.getSeparator();

        command.append("-jar ");
        command.append(bootstrap_home);
        command.append(separator);
        command.append(BOOTSTRAP_JAR_NAME);

        command.append(SPACE);
    }

    private static synchronized File getBootstrapJar() throws IOException {

        return !BOOTSTRAP_JAR.isFile() ? constructBootstrapJar() : BOOTSTRAP_JAR;
    }

    private static File constructBootstrapJar() throws IOException {

        final Manifest manifest = new Manifest();
        final Attributes main_attributes = manifest.getMainAttributes();
        main_attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        main_attributes.put(Attributes.Name.MAIN_CLASS, JavaBootstrap.class.getName());
        main_attributes.put(Attributes.Name.CLASS_PATH, ".");

        BOOTSTRAP_JAR.getParentFile().mkdirs();

        final JarOutputStream jar_stream = new JarOutputStream(new FileOutputStream(BOOTSTRAP_JAR), manifest);

        try {
            addClassToJar(JavaBootstrap.class, jar_stream);
            addClassToJar(DependencyResolutionUtils.class, jar_stream);
            addClassToJar(DependencyResolutionUtils.URLCollector.class, jar_stream);
            return BOOTSTRAP_JAR;
        }
        catch (final URISyntaxException e) {
            throw new IOException(e);
        }
        finally {
            jar_stream.close();
        }
    }

    private static void addClassToJar(final Class<?> type, final JarOutputStream jar) throws URISyntaxException, IOException {

        final URL resource_url = type.getResource(getResourceName(type));
        final File source = new File(resource_url.toURI());
        final String entry_name = type.getPackage().getName().replaceAll("\\.", "/") + "/" + source.getName();
        final JarEntry entry = new JarEntry(entry_name);
        entry.setTime(source.lastModified());
        jar.putNextEntry(entry);

        FileUtils.copyFile(source, jar);
        jar.closeEntry();
    }

    private static String getResourceName(final Class<?> type) {

        return type.getName().replace(type.getPackage().getName() + ".", "") + ".class";
    }
}
