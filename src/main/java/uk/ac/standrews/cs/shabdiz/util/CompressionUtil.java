package uk.ac.standrews.cs.shabdiz.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public final class CompressionUtil {

    private CompressionUtil() {

    }

    public static final void compress(final File source, final File destination) throws IOException {

        compress(Arrays.asList(source), destination);
    }

    public static final void compress(final Collection<File> sources, final File destination) throws IOException {

        final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destination)));
        for (final File file : sources) {
            final URI base = file.getCanonicalFile().getParentFile().toURI();
            copy(file, zip, base);
        }
        zip.close();
    }

    public static void main(final String[] args) throws IOException {

        compress(Arrays.asList(new File("."), new File("/Users/masih/Documents/student_card.jpg")), new File("/Users/masih/Desktop/mm.zip"));
    }

    private static void copy(final File file, final ZipOutputStream zip, final URI base) throws IOException {

        if (file.exists()) {
            if (file.isFile()) {
                final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                final ZipEntry entry = new ZipEntry(base.relativize(file.getCanonicalFile().toURI()).getPath());
                try{
                    zip.putNextEntry(entry);
                    IOUtils.copy(in, zip);
                }catch(final ZipException e){
                    System.out.println(e.getMessage());
                }finally{
                    in.close();
                }
            }
            else {
                for (final File sub_file : file.listFiles()) {
                    copy(sub_file, zip, base);
                }
            }
        }

    }
}
