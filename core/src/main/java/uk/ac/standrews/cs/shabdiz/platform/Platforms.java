/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.standrews.cs.shabdiz.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/**
 * Factory for {@link Platform} and {@link SimplePlatform}, and utility methods to detect {@link Platform platform} from {@link Host host} by executing {@code uname} command or a Java-based platform detector.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Platforms {

    private static final String UNAME_COMMAND = "uname";
    private static final String VER_COMMAND = "ver";
    private static final String PLATFORM_DETECTOR_JAR_VERSION = "1.0";
    private static File cached_platform_detector_jar;

    private Platforms() {

    }

    /**
     * Detects the platform of a given {@link Host host}.
     * If the {@code host} {@link Host#isLocal() is local}, returns an instance of {@link LocalPlatform}.
     * Otherwise, attempts to detect the platform from the output that is produced by executing the {@code uname} command on the host.
     *
     * @param host the host
     * @return the platform
     * @throws IOException Signals that an I/O exception has occurred.
     * @see #detectPlatform(Host, boolean)
     */
    public static Platform detectPlatform(final Host host) throws IOException {

        return detectPlatform(host, false);
    }

    /**
     * Detects the platform of a given {@link Host host}.
     * If the {@code host} {@link Host#isLocal() is local}, returns an instance of {@link LocalPlatform}.
     * If the {@code use_java} flag is set to {@code true}, attempts to detect the platform by a Java-based application on the remote host.
     * Otherwise, attempts to detect the platform from the output that is produced by executing the {@code uname} command on the host.
     *
     * @param host the host to detect the platform of
     * @param use_java whether to use a Java-based platform detector
     * @return the detected platform of the given {@code host}
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Platform detectPlatform(final Host host, final boolean use_java) throws IOException {

        return host.isLocal() ? LocalPlatform.getInstance() : use_java ? detectRemotePlatformUsingJava(host) : detectRemotePlatformUsingUname(host);
    }

    /**
     * Constructs a {@link SimplePlatform} from the output that is produced by the execution of {@code uname} command.
     * Sets the platform operating system name to the given output.
     * If the given output contains {@link WindowsPlatform#WINDOWS_OS_NAME_KEY} it is assumed the platform is Windows.
     * If the given output contains {@link CygwinPlatform#CYGWIN_OS_NAME_KEY} it is assumed the platform is Cygwin.
     * Otherwise the platform is assumed to be Unix-based.
     *
     * @param uname_output the output produced by the execution of {@code uname} commad
     * @return an isntance of {@link SimplePlatform} that represents Windowns, Cygwin or Unix platform
     */
    public static SimplePlatform fromOSName(final String uname_output) {

        final String output = uname_output.toLowerCase().trim();
        if (output.contains(CygwinPlatform.CYGWIN_OS_NAME_KEY)) { return new CygwinPlatform(output); }
        if (output.contains(WindowsPlatform.WINDOWS_OS_NAME_KEY)) { return new WindowsPlatform(output); }
        return new UnixPlatform(output);
    }

    /**
     * Gets the current user from system properties.
     *
     * @return the current user
     * @see System#getProperty(String)
     */
    public static String getCurrentUser() {

        return System.getProperty("user.name");
    }

    /**
     * Checks if a given platform presents a UNIX based platform.
     *
     * @param target the target platform
     * @return true, if the path separator and separator of the target platform are equal to UNIX platform
     */
    public static boolean isUnixBased(final Platform target) {

        return target.getPathSeparator() == Platform.UNIX_PATH_SEPARATOR && target.getSeparator() == Platform.UNIX_SEPARATOR;
    }

    private static Platform detectRemotePlatformUsingJava(final Host host) throws IOException {

        final File platform_detector = getPlatformDetectorJar();
        final AgentBasedJavaProcessBuilder java_process_builder = new AgentBasedJavaProcessBuilder();
        java_process_builder.setMainClass(PlatformDetector.class);
        java_process_builder.addFile(platform_detector);

        final Process platform_detector_process = java_process_builder.start(host);
        try {
            final List<String> output_lines = IOUtils.readLines(platform_detector_process.getInputStream());
            return parseOutputLines(output_lines);
        }
        finally {
            platform_detector_process.destroy();
        }
    }

    private static Platform parseOutputLines(final List<String> output_lines) throws IOException {

        PlatformDetector.validate(output_lines);
        final String os_name = output_lines.get(0);
        final char path_separator = output_lines.get(1).charAt(0);
        final char separator = output_lines.get(2).charAt(0);
        final String temp_dir = output_lines.get(3);
        return new SimplePlatform(os_name, path_separator, separator, temp_dir);
    }

    private static Platform detectRemotePlatformUsingUname(final Host host) throws IOException {

        String os_name;
        try {
            try {
                os_name = ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(UNAME_COMMAND));
            }
            catch (final IOException e) {
                os_name = ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(VER_COMMAND));
            }
        }
        catch (final InterruptedException e) {
            throw new IOException(e);
        }

        final SimplePlatform platform = fromOSName(os_name);
        detectTempDirectory(host, platform);
        return platform;
    }

    private static void detectTempDirectory(final Host host, final SimplePlatform platform) throws IOException {
        final String temp_dir;
        final String get_tmp_command = Commands.GET_TEMP_DIR.get(platform);
        try {
            temp_dir = ProcessUtil.awaitNormalTerminationAndGetOutput(host.execute(get_tmp_command));
        }
        catch (InterruptedException e) {
            throw new IOException(e);
        }

        if (temp_dir != null && !temp_dir.isEmpty()) {
            platform.setTempDirectory(temp_dir);
        }
    }

    private static synchronized File getPlatformDetectorJar() throws IOException {

        if (!isPlatformDetectorJarCached()) {
            cached_platform_detector_jar = makePlatformDetectorJar();
        }
        return cached_platform_detector_jar;
    }

    private static boolean isPlatformDetectorJarCached() {

        return cached_platform_detector_jar != null && cached_platform_detector_jar.exists() && cached_platform_detector_jar.isFile();
    }

    private static File makePlatformDetectorJar() throws IOException {

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, PLATFORM_DETECTOR_JAR_VERSION);
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, PlatformDetector.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, ".");
        final File platform_detector_jar = File.createTempFile("platform_detector", ".jar");
        final JarOutputStream target = new JarOutputStream(new FileOutputStream(platform_detector_jar), manifest);

        try {
            addClassToJar(PlatformDetector.class, target);
            return platform_detector_jar;
        }
        catch (final URISyntaxException e) {
            throw new IOException(e);
        }
        finally {
            target.close();
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

final class PlatformDetector {

    private static final int NUMBER_OF_EXPECTED_OUTPUT_LINES = 4;

    private PlatformDetector() {

    }

    public static void main(final String[] args) {

        printPlatformInLines();
    }

    public static void validate(final List<String> output_lines) throws IOException {

        System.out.println(output_lines);
        if (output_lines == null) { throw new IOException("cannot instantiate platform, no output was produced by platform detector"); }
        if (output_lines.size() != NUMBER_OF_EXPECTED_OUTPUT_LINES) { throw new IOException("cannot instantiate platform, unparsable was produced by platform detector"); }
    }

    private static void printPlatformInLines() {

        System.out.println(System.getProperty("os.name"));
        System.out.println(File.pathSeparatorChar);
        System.out.println(File.separatorChar);
        System.out.println(System.getProperty("java.io.tmpdir"));
    }
}
