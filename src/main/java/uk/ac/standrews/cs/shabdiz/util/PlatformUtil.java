/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.shabdiz.Host;
import uk.ac.standrews.cs.shabdiz.Platform;
import uk.ac.standrews.cs.shabdiz.RemoteJavaProcessBuilder;

public class PlatformUtil {

    private static final String UNAME_COMMAND = "uname";
    private static File PLATFORM_DETECTOR_CACHED_JAR;
    private static final String PLATFORM_DETECTOR_JAR_VERSION = "1.0";

    public static Platform detectPlatform(final Host host) throws IOException {

        return detectPlatform(host, false);
    }

    public static Platform detectPlatform(final Host host, final boolean use_java) throws IOException {

        return host.isLocal() ? Platform.LOCAL : use_java ? detectRemotePlatformUsingJava(host) : detectRemotePlatformUsingUname(host);
    }

    private static Platform detectRemotePlatformUsingJava(final Host host) throws IOException {

        final File platform_detector = getPlatformDetectorJar();
        final RemoteJavaProcessBuilder java_process_builder = new RemoteJavaProcessBuilder(PlatformDetector.class);
        java_process_builder.addClasspath(platform_detector);
        final Process platform_detector_process = java_process_builder.start(host);
        final Scanner scenner = new Scanner(platform_detector_process.getInputStream());
        try {
            final JSONObject platform_as_json = new JSONObject(scenner.nextLine());
            return Platform.fromJSON(platform_as_json);
        }
        catch (final JSONException e) {
            throw new IOException(e);
        }
        finally {
            scenner.close();
            platform_detector_process.destroy();
        }
    }

    public static Platform detectRemotePlatformUsingUname(final Host host) throws IOException {

        // See: http://en.wikipedia.org/wiki/Uname#Examples
        final Process uname_process = host.execute(UNAME_COMMAND);
        final Scanner scanner = new Scanner(uname_process.getInputStream());
        try {
            final String uname_output = scanner.nextLine();
            return Platform.fromUnameOutput(uname_output);
        }
        finally {
            scanner.close();
            uname_process.destroy();
        }
    }

    private synchronized static File getPlatformDetectorJar() throws IOException {

        if (!isPlatformDetectorJarCached()) {
            PLATFORM_DETECTOR_CACHED_JAR = makePlatformDetectorJar();
        }
        return PLATFORM_DETECTOR_CACHED_JAR;
    }

    private static boolean isPlatformDetectorJarCached() {

        return PLATFORM_DETECTOR_CACHED_JAR != null && PLATFORM_DETECTOR_CACHED_JAR.exists() && PLATFORM_DETECTOR_CACHED_JAR.isFile();
    }

    private static File makePlatformDetectorJar() throws IOException {

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, PLATFORM_DETECTOR_JAR_VERSION);
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, PlatformDetector.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, ".");
        final File platform_detector_jar = File.createTempFile(null, "platform_detector.jar");
        final JarOutputStream target = new JarOutputStream(new FileOutputStream(platform_detector_jar), manifest);

        try {
            addClassToJar(PlatformDetector.class, target);
            addClassToJar(LibraryUtil.class, target);
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

        final URL resource_url = PlatformDetector.class.getResource(getResourceName(type));
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

    public static class PlatformDetector {

        public static void main(final String[] args) {

            System.out.println(Platform.LOCAL.toJSON());
        }
    }
}
