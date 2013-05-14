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
package uk.ac.standrews.cs.shabdiz.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

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

/**
 * A utility class for compressing files.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class CompressionUtil {

    //TODO use jar to make jar files of directories in classpath
    // e.g. jar cf aaa.jar -C /Users/masih/Documents/PhD/Code/P2P\ Workspace/shabdiz/target/classes .

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CompressionUtil.class);

    private CompressionUtil() {

    }

    /**
     * Compresses the given {@code source} into a {@code zip} file located at the given {@code destination}.
     *
     * @param source the file to zip
     * @param destination the zipped file
     * @throws IOException if an IO error occurs
     */
    public static void toZip(final File source, final File destination) throws IOException {

        toZip(Arrays.asList(source), destination);
    }

    /**
     * Compresses the given {@code sources} into a {@code zip} file located at the given {@code destination}.
     *
     * @param sources the files to zip
     * @param destination the zipped file
     * @throws IOException if an IO error occurs
     */
    public static void toZip(final Collection<File> sources, final File destination) throws IOException {

        final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destination)));
        try {
            for (final File file : sources) {
                final URI base = file.getCanonicalFile().getParentFile().toURI();
                copyToZipStream(file, zip, base);
            }
        } finally {
            zip.close();
        }
    }

    private static void copyToZipStream(final File file, final ZipOutputStream zip, final URI base) throws IOException {

        if (file.exists()) {
            if (file.isFile()) {
                copyFileToZipStream(file, zip, base);
            }
            else {
                copyDirectoryToZipStream(file, zip, base);
            }
        }
    }

    private static void copyDirectoryToZipStream(final File file, final ZipOutputStream zip, final URI base) throws IOException {
        final File[] files_list = file.listFiles();
        if (files_list != null) {
            for (final File sub_file : files_list) {
                copyToZipStream(sub_file, zip, base);
            }
        }
    }

    private static void copyFileToZipStream(final File file, final ZipOutputStream zip, final URI base) throws IOException {
        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        final ZipEntry entry = new ZipEntry(base.relativize(file.getCanonicalFile().toURI()).getPath());
        putZipEntry(zip, in, entry);
    }

    private static void putZipEntry(final ZipOutputStream zip, final BufferedInputStream in, final ZipEntry entry) throws IOException {
        try {
            zip.putNextEntry(entry);
            IOUtils.copy(in, zip);
        } catch (final ZipException e) {
            LOGGER.warn("failed to put entry to zip stream", e);
        } finally {
            in.close();
        }
    }
}
