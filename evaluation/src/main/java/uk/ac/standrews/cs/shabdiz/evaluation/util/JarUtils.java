package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class JarUtils {

    public static void toJar(File directory, File jar, Manifest manifest) throws IOException {

        if (!directory.isDirectory()) { throw new IllegalArgumentException("expected directory"); }

        final JarOutputStream jar_out = new JarOutputStream(new FileOutputStream(jar), manifest);

        copyDirectory(directory, directory, jar_out);
        jar_out.finish();
        jar_out.flush();
        jar_out.close();
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
