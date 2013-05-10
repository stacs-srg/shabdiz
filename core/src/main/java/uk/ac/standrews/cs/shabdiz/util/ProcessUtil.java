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
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.staticiser.jetson.util.NamingThreadFactory;

public final class ProcessUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);
    private static final String UTF_8 = "UTF-8";
    private static final String DELIMITER = "=";
    private static final int NORMAL_TERMINATION = 0;

    private ProcessUtil() {

    }

    public static String waitForNormalTerminationAndGetOutput(final Process process) throws InterruptedException, IOException {

        final ExecutorService error_reader_executor = Executors.newFixedThreadPool(2, new NamingThreadFactory("process_util_"));
        try {
            final Future<Void> future_error = error_reader_executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws IOException {

                    final String error = IOUtils.toString(process.getErrorStream());
                    if (error != null && !error.equals("")) { throw new IOException(error); }
                    return null; // Void task
                }
            });
            final Future<String> future_result = error_reader_executor.submit(new Callable<String>() {

                @Override
                public String call() throws IOException {

                    return IOUtils.toString(process.getInputStream());
                }
            });

            try {
                final int exit_value = process.waitFor();
                LOGGER.info("done waiting for process, exit value: {}", exit_value);
                future_error.get();
                if (exit_value != NORMAL_TERMINATION) {
                    LOGGER.warn("No error occured while executing the process but the exit value is non zero: {}", exit_value);
                }
                return future_result.get().trim();
            }
            catch (final ExecutionException e) {
                LOGGER.debug("error occured on process execution", e);
                final Throwable cause = e.getCause();
                throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
            }
        }
        finally {
            process.destroy();
            error_reader_executor.shutdownNow();
        }
    }

    public static String getValueFromProcessOutput(final Process process, final String key, final Duration timeout) throws InterruptedException, TimeoutException, IOException {

        boolean scan_succeeded = false;
        final Callable<String> scan_task = createProcessOutputScanTask(process, key);
        try {
            final String value = TimeoutExecutorService.awaitCompletion(scan_task, timeout);
            scan_succeeded = true;
            return value;
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw IOException.class.isInstance(cause) ? IOException.class.cast(cause) : new IOException(cause);
        }
        finally {
            if (!scan_succeeded) {
                process.destroy();
            }
        }
    }

    public static Integer getPIDFromRuntimeMXBeanName(final String runtime_mxbean_name) {

        Integer pid = null;
        final int index_of_at = runtime_mxbean_name.indexOf("@");

        if (index_of_at != -1) {
            try {
                pid = Integer.parseInt(runtime_mxbean_name.substring(0, index_of_at));
            }
            catch (final NumberFormatException e) {
                LOGGER.debug("failed to extract pid from runtime MXBean name", e);
            }
        }
        return pid;
    }

    private static Callable<String> createProcessOutputScanTask(final Process process, final String key) {

        return new Callable<String>() {

            @Override
            public String call() throws Exception {

                String worker_address;
                final Scanner scanner = new Scanner(process.getInputStream(), UTF_8);
                do {
                    final String output_line = scanner.nextLine();
                    worker_address = findValueInLine(output_line, key);
                }
                while (worker_address == null && !Thread.currentThread().isInterrupted());
                // Scanner is not closed on purpose. The stream belongs to Process instance.
                return worker_address;
            }
        };
    }

    public static void printKeyValue(final PrintStream out, final String key, final Object value) {

        synchronized (out) {
            out.println(key + DELIMITER + value);
        }
    }

    private static String findValueInLine(final String line, final String key) throws UnknownHostException {

        return line != null && line.startsWith(key + DELIMITER) ? line.split(DELIMITER)[1] : null;
    }
}
