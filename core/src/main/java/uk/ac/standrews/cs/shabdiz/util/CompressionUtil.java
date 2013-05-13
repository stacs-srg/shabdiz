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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public final class CompressionUtil {

    //FIXME use jar to make jar files of directories in classpath
    // e.g. jar cf aaa.jar -C /Users/masih/Documents/PhD/Code/P2P\ Workspace/shabdiz/target/classes .

    private static final Logger LOGGER = Logger.getLogger(CompressionUtil.class.getName());

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

    private static void copy(final File file, final ZipOutputStream zip, final URI base) throws IOException {

        if (file.exists()) {
            if (file.isFile()) {
                final BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                final ZipEntry entry = new ZipEntry(base.relativize(file.getCanonicalFile().toURI()).getPath());
                try {
                    zip.putNextEntry(entry);
                    IOUtils.copy(in, zip);
                } catch (final ZipException e) {
                    LOGGER.severe(e.getMessage());
                } finally {
                    in.close();
                }
            } else {
                for (final File sub_file : file.listFiles()) {
                    copy(sub_file, zip, base);
                }
            }
        }

    }
}
