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

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.RuntimeMXBean;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.IOUtils;
import org.mashti.jetson.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;

/**
 * A utility class for awaiting {@link Process} termination and parsing {@link Process} outputs.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class ProcessUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);
    private static final String UTF_8 = "UTF-8";
    private static final String DELIMITER = "=";
    private static final int NORMAL_TERMINATION = 0;

    private ProcessUtil() {

    }

    /**
     * Attempts to get a PID from a given runtime MXBean name.
     * The expected format is {@code <pid>@<machine_name>}.
     * Returns {@code null} if the given MXBean name does not match the above pattern.
     *
     * @return the pid from the given name or {@code null} if the name does not match the expected pattern
     * @see RuntimeMXBean#getName()
     */
    public static Integer getPIDFromRuntimeMXBeanName(final String runtime_mxbean_name) {

        Integer pid = null;
        final int index_of_at = runtime_mxbean_name.indexOf("@");
        if (index_of_at != -1) {
            pid = Integer.parseInt(runtime_mxbean_name.substring(0, index_of_at));
        }
        return pid;
    }

    public static void killProcessOnHostByPID(Host host, int pid) throws IOException, InterruptedException {

        final Platform platform = host.getPlatform();
        final String kill_command = Commands.FORCE_KILL_BY_PROCESS_ID.get(platform, String.valueOf(pid));
        //        final String kill_command = Commands.KILL_BY_PROCESS_ID.get(platform, String.valueOf(pid));
        final Process kill = host.execute(kill_command);
        awaitNormalTerminationAndGetOutput(kill);
    }

    /**
     * Awaits normal termination of a given {@code process} and returns its output from {@link Process#getInputStream()}.
     * A process is considered to have terminated normally when: its exit value is equal to {@code 0}, or on output was produced from its {@link Process#getErrorStream()}.
     * The output produced by the process's {@link Process#getErrorStream()} is wrapped around an {@link IOException} and thrown.
     *
     * @param process the process to wait for its normal termination
     * @return the output produced by the process's {@link Process#getInputStream()}
     * @throws InterruptedException if interrupted while waiting for process termination
     * @throws IOException if the process terminates with some output in its error stream
     */
    public static String awaitNormalTerminationAndGetOutput(final Process process) throws InterruptedException, IOException {

        final ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("process_util_"));
        try {
            final Future<Void> future_error = executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws IOException {

                    final String error = IOUtils.toString(process.getErrorStream());
                    if (error != null && !error.equals("")) { throw new IOException(error); }
                    return null; // Void task
                }
            });
            final Future<String> future_result = executor.submit(new Callable<String>() {

                @Override
                public String call() throws IOException {

                    return IOUtils.toString(process.getInputStream());
                }
            });

            try {
                final int exit_value = process.waitFor();
                LOGGER.debug("done waiting for process, exit value: {}", exit_value);
                future_error.get();
                if (exit_value != NORMAL_TERMINATION) {
                    LOGGER.warn("No error occurred while executing the process but the exit value is non zero: {}", exit_value);
                }
                return future_result.get().trim();
            }
            catch (final ExecutionException e) {
                LOGGER.debug("error occurred on process execution", e);
                final Throwable cause = e.getCause();
                throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
            }
        }
        finally {
            process.destroy();
            executor.shutdownNow();
        }
    }

    /**
     * Prints a line containing the given {@code key value} pair to the given stream.
     * This method is thread-safe.
     *
     * @param out the stream to write to
     * @param key the key
     * @param value the value
     */
    public static void printKeyValue(final PrintStream out, final String key, final Object value) {

        synchronized (out) {
            out.println(key + DELIMITER + value);
        }
    }

    /**
     * Scans the process output for a value that is specified with the give {@code key}.
     *
     * @param process the process to scan its output
     * @param key the key of the value to scan for
     * @param timeout the maximum time to wait for the value
     * @return the value associated to the given key in the process output stream
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException if the timeout has elapsed before a result is produced
     * @throws IOException if an IO error occurs
     */
    public static String scanProcessOutput(final Process process, final String key, final Duration timeout) throws InterruptedException, TimeoutException, IOException {

        boolean scan_succeeded = false;
        final Callable<String> scan_task = createProcessOutputScanTask(process, key);
        try {
            final String value = TimeoutExecutorService.awaitCompletion(scan_task, timeout);
            scan_succeeded = true;
            return value;
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw IOException.class.isInstance(cause) ? (IOException) cause : new IOException(cause);
        }
        finally {
            if (!scan_succeeded) {
                process.destroy();
            }
        }
    }

    private static Callable<String> createProcessOutputScanTask(final Process process, final String key) {

        return new Callable<String>() {

            @Override
            public String call() throws Exception {

                String worker_address;
                final Scanner scanner = new Scanner(process.getInputStream(), UTF_8);
                do {
                    final String output_line = scanner.nextLine();
                    System.out.println(output_line);
                    worker_address = findValueInLine(output_line, key);
                }
                while (worker_address == null && !Thread.currentThread().isInterrupted());
                // Scanner is not closed on purpose. The stream belongs to Process instance.
                return worker_address;
            }
        };
    }

    private static String findValueInLine(final String line, final String key) {

        return line != null && line.startsWith(key + DELIMITER) ? line.split(DELIMITER)[1] : null;
    }
}
