package uk.ac.standrews.cs.shabdiz.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class JarUtils {

    public static void currentClasspathToExecutableJar(File destination, Class<?> main_class) throws IOException {

        final String classpath = getCurrentJvmClasspathForManifest();
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.CLASS_PATH, classpath);
        attributes.put(Attributes.Name.MAIN_CLASS, main_class.getName());

        JarOutputStream jar_outupt = null;
        try {
            jar_outupt = new JarOutputStream(new FileOutputStream(destination), manifest);
            jar_outupt.flush();
        }
        finally {
            IOUtils.closeQuietly(jar_outupt);

        }
    }

    public static void toJar(File directory, File jar, Manifest manifest) throws IOException {

        if (!directory.isDirectory()) { throw new IllegalArgumentException("expected directory"); }

        final JarOutputStream jar_out = new JarOutputStream(new FileOutputStream(jar), manifest);

        copyDirectory(directory, directory, jar_out);
        jar_out.finish();
        jar_out.flush();
        jar_out.close();
    }

    private static String getCurrentJvmClasspathForManifest() throws MalformedURLException {

        final StringBuilder classpath_builder = new StringBuilder();
        for (String classpath_entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            URL cp_entry = new File(classpath_entry).toURI().toURL();
            classpath_builder.append(cp_entry.toExternalForm());
            classpath_builder.append(" ");
        }

        return classpath_builder.toString().trim();
    }

    private static void copyDirectory(File base, final File directory, final JarOutputStream jar) throws IOException {

        for (File subDir : directory.listFiles()) {

            if (subDir.isFile()) {
                copyFile(base, base.toURI().relativize(subDir.toURI()).getPath(), jar);
            }
            else {
                copyDirectory(base, subDir, jar);
            }
        }
    }

    private static void copyFile(File base, final String file, final JarOutputStream jar) throws IOException {

        System.out.println(file);
        final JarEntry entry = new JarEntry(file);
        jar.putNextEntry(entry);
        IOUtils.copy(new BufferedInputStream(new FileInputStream(new File(base, file))), jar);
        jar.closeEntry();
    }
}
