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

package uk.ac.standrews.cs.shabdiz.zold.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;

import uk.ac.standrews.cs.nds.util.Duration;

public final class ProcessUtil {

    private static final String DELIMITER = "=";
    private static final int NORMAL_TERMINATION = 0;

    private ProcessUtil() {

    }

    public static String waitForAndReadOutput(final Process process) throws IOException, InterruptedException {

        final int exit_value = process.waitFor();
        try {
            switch (exit_value) {
                case NORMAL_TERMINATION:
                    return IOUtils.toString(process.getInputStream());
                default:
                    throw new IOException();
            }
        }
        finally {
            process.destroy();
        }
    }

    public static String getValueFromProcessOutput(final Process process, final ExecutorService executor, final String key, final Duration timeout) throws InterruptedException, TimeoutException, IOException  {

        final Future<String> future_value = executeValueScan(process, executor, key);
        boolean scan_succeeded = false;
        try {
            final String value = future_value.get(timeout.getLength(), timeout.getTimeUnit());
            scan_succeeded = true;
            return value;
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            final Class<IOException> io_exception = IOException.class;
            throw io_exception.isInstance(cause) ? io_exception.cast(cause) : new IOException(cause);
        }
        finally {
            if (!scan_succeeded) {
                if (!future_value.isDone()) {
                    future_value.cancel(true);
                }
                process.destroy();
            }
        }
    }

    private static Future<String> executeValueScan(final Process worker_process, final ExecutorService executor, final String key) {

        return executor.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {

                String worker_address;
                final Scanner scanner = new Scanner(worker_process.getInputStream()); // Scanner is not closed on purpose. The stream belongs to Process instance.
                do {
                    final String output_line = scanner.nextLine();
                    worker_address = findValueInLine(output_line, key);
                }
                while (worker_address == null && !Thread.currentThread().isInterrupted());
                return worker_address;
            }
        });
    }

    public static void printValue(final PrintStream out, final String key, final Object value) {

        synchronized (out) {
            out.println(key + DELIMITER + value);
        }
    }

    private static String findValueInLine(final String line, final String key) throws UnknownHostException {
        return line != null && line.startsWith(key + DELIMITER) ? line.split(DELIMITER)[1] : null;
    }
}
