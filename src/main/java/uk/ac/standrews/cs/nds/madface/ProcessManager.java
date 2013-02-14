/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.nds.madface.interfaces.IStreamProcessor;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

import com.mindbright.jca.security.SecureRandom;
import com.mindbright.ssh2.SSH2AccessDeniedException;
import com.mindbright.ssh2.SSH2Connection;
import com.mindbright.ssh2.SSH2ConsoleRemote;
import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.util.RandomSeed;
import com.mindbright.util.SecureRandomAndPad;

/**
 * Utility class for creating local and remote OS processes. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author graham
 */
public class ProcessManager {

    public static final Duration PROCESS_INVOCATION_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    public static final Duration DEFAULT_SSH_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    public static final Duration DEFAULT_PROCESS_TIMEOUT = new Duration(10, TimeUnit.SECONDS);

    private static final String SHELL_PATH = "/bin/sh";

    private static final int KILL_THREADS = 1;
    private static final Duration KILL_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    private static final int SSH_PORT = 22;
    private static final String TEMP_FILES_ROOT = "madface";

    private static String local_platform_name = null;

    /**
     * References to all reader threads created by this process manager. Used by the {@link #stopReaderThreads()} method
     * to terminate extant threads.
     */
    private static List<Thread> readerThreads = new LinkedList<Thread>();

    private static final AtomicLong READER_THREAD_COUNT = new AtomicLong();

    private final HostDescriptor host_descriptor;

    private volatile SSH2SimpleClient client = null;
    private boolean discard_errors = false;
    private final TimeoutExecutor kill_executor;

    private boolean run_java_process_with_assertions_enabled = true;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new process manager for the given host.
     * 
     * @param host_descriptor descriptor for a host
     */
    ProcessManager(final HostDescriptor host_descriptor) {

        this.host_descriptor = host_descriptor;
        kill_executor = TimeoutExecutor.makeTimeoutExecutor(KILL_THREADS, KILL_TIMEOUT, true, false, "ProcessManager kill executor");
    }

    public void setDiscardErrors(final boolean discard_errors) {

        this.discard_errors = discard_errors;
    }

    public synchronized void shutdown() {

        kill_executor.shutdown();

        if (client != null) {
            client.getTransport().normalDisconnect("shutting down connection");
        }
    }

    // -------------------------------------------------------------------------------------------------------

    public Process runProcess(final ProcessDescriptor process_descriptor) throws SSH2Exception, IOException, TimeoutException, UnknownPlatformException, UnsupportedPlatformException, InterruptedException {

        if (process_descriptor instanceof JavaProcessDescriptor) {
            return runJavaProcess((JavaProcessDescriptor) process_descriptor);
        }
        else if (host_descriptor.local()) {
            return runProcessLocal(process_descriptor);
        }
        else {
            return runProcessRemote(process_descriptor);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Attempts to kill all processes whose names include the specified string.
     * 
     * @param label the string to match
     * @throws SSH2Exception if an SSH connection to a remote machine cannot be established
     * @throws IOException if an error occurs when communicating with a remote machine
     * @throws TimeoutException if the kill command has not been invoked within the default timeout period
     * @throws UnknownPlatformException
     * @throws InterruptedException
     * @throws UnsupportedPlatformException
     */
    public void killMatchingProcesses(final String label) throws SSH2Exception, IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        killMatchingProcesses(label, new ArrayList<File>());
    }

    public void killMatchingProcesses(final String label, final List<File> files_to_be_deleted) throws SSH2Exception, IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        try {
            final String kill_command = getKillCommand(label, files_to_be_deleted);
            final ProcessDescriptor process_descriptor = new ProcessDescriptor().command(kill_command).executor(kill_executor);
            runProcess(process_descriptor).waitFor();
        }
        catch (final NullPointerException e) {
            // Work round Mindterm bug.
        }
    }

    public void clearTempFiles() throws SSH2Exception, IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        try {
            final String clear_temp_files_command = getClearTempFilesCommand();
            final ProcessDescriptor process_descriptor = new ProcessDescriptor().command(clear_temp_files_command).executor(kill_executor);
            runProcess(process_descriptor).waitFor();
        }
        catch (final NullPointerException e) {
            // Work round Mindterm bug.
        }
    }

    public void runJavaProcessesWithAssertionsEnabled(final boolean run_java_process_with_assertions_enabled) {

        this.run_java_process_with_assertions_enabled = run_java_process_with_assertions_enabled;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Runs the specified class in a new OS process. Output is written to the specified streams.
     * 
     * @param jvm_params the JVM parameters
     * @param clazz the class to be executed
     * @param args command line arguments
     * @param output_stream the stream to which output from the process should be directed
     * @param error_stream the stream to which error output from the process should be directed
     * @param timeout the timeout for completing the issue of the command (NB not for the remote process to complete)
     * @return a handle to the new OS process
     * @throws SSH2Exception if an SSH connection to a remote machine cannot be established
     * @throws IOException if an error occurs when reading output from the process
     * @throws TimeoutException if the remote process cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of a remote host cannot be established
     * @throws InterruptedException
     * @throws UnsupportedPlatformException
     */
    private Process runJavaProcess(final JavaProcessDescriptor java_process_descriptor) throws IOException, SSH2Exception, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        final List<String> jvm_params = java_process_descriptor.getJVMParams();
        final Class<?> clazz = java_process_descriptor.getClassToBeInvoked();
        final List<String> args = java_process_descriptor.getArgs();

        final ProcessDescriptor process_descriptor = new ProcessDescriptor();

        try {
            if (useApplicationUrlsForJava()) {

                final PlatformDescriptor platform_descriptor = host_descriptor.getPlatform();
                final File wget_path = new File(platform_descriptor.getWgetPath());

                final File lib_install_dir = new File(getTimestampedTempPath(new File(platform_descriptor.getTempPath()), clazz.getName()));
                final ClassPath class_path = getClassPathForTempDir(host_descriptor.getApplicationURLs(), lib_install_dir);

                host_descriptor.classPath(class_path);
                host_descriptor.javaLibraryPath(lib_install_dir);

                final String install_command = getInstallLibsCommand(host_descriptor.getApplicationURLs(), lib_install_dir, wget_path);

                final String java_command = getJavaCommand(jvm_params, clazz, args);
                final String command = combineCommands(install_command, java_command);

                process_descriptor.command(command);
                process_descriptor.label(java_command);
                process_descriptor.deleteOnExit(lib_install_dir);

                return runProcess(process_descriptor);
            }
            else {

                final String command = getJavaCommand(jvm_params, clazz, args);
                process_descriptor.command(command);

                return runProcess(process_descriptor);
            }
        }
        finally {
            process_descriptor.shutdown();
        }
    }

    /**
     * Determines whether application-specific URLs should be incorporated into a Java invocation command. This is true iff it is a remote command and some URLs are set.
     * 
     * @return true if application-specific URLs should be incorporated into a Java invocation command
     */
    private boolean useApplicationUrlsForJava() {

        final Set<URL> application_urls = host_descriptor.getApplicationURLs();
        return !host_descriptor.local() && application_urls != null && application_urls.size() > 0;
    }

    // -------------------------------------------------------------------------------------------------------

    private String getLocalPlatformName() {

        // Race condition here but doesn't matter since it would just get initalized twice.
        if (local_platform_name == null) {
            local_platform_name = host_descriptor.getPlatform().getName();
        }
        return local_platform_name;
    }

    private Process runProcessLocal(final ProcessDescriptor process_descriptor) throws IOException {

        final String command = process_descriptor.getCommand();
        final OutputStream output_stream = process_descriptor.getOutputStream();
        final OutputStream error_stream = process_descriptor.getErrorStream();
        final IStreamProcessor output_processor = process_descriptor.getOutputProcessor();
        final IStreamProcessor error_processor = process_descriptor.getErrorProcessor();

        // Get streams that process the output and error streams.
        final OutputStream intercept_output_stream = makeInterceptStream(output_stream, output_processor);
        final OutputStream intercept_error_stream = discard_errors ? null : makeInterceptStream(error_stream, error_processor);

        final Process process;

        if (!getLocalPlatformName().equals(PlatformDescriptor.NAME_WINDOWS)) {

            // If not on Windows, run the command in a new OS shell.
            final String[] wrapped_command = new String[]{SHELL_PATH, "-c", command};
            process = Runtime.getRuntime().exec(wrapped_command);
        }
        else {

            process = Runtime.getRuntime().exec(command);
        }

        final PrintStream output_print_stream = new PrintStream(intercept_output_stream);
        final PrintStream error_print_stream = new PrintStream(intercept_error_stream);

        // Get streams reading from the standard out and standard error for the new process.
        final BufferedReader command_output_stream = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
        final BufferedReader command_error_stream = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getErrorStream())));

        // Start a thread to capture any output and write it locally.
        makeReaderThread(command_output_stream, output_print_stream).start();

        // Start a thread to capture any errors and write them locally.
        makeReaderThread(command_error_stream, error_print_stream).start();

        return process;
    }

    /**
     * The MindTerm package supports the creation of a remote process, but not the later destruction of that process should it become necessary. So this method returns a process object that wraps the label to be used for killing the remote process.
     * 
     * @throws InterruptedException
     * @throws UnknownPlatformException
     * @throws UnsupportedPlatformException
     */
    private Process runProcessRemote(final ProcessDescriptor process_descriptor) throws SSH2Exception, IOException, TimeoutException, InterruptedException, UnknownPlatformException, UnsupportedPlatformException {

        // Can't check remote platform at this point, since it may not have been initialised yet.

        String command = process_descriptor.getCommand();

        final List<File> files_to_be_deleted = process_descriptor.getFilesDeletedOnExit();
        for (final File file : files_to_be_deleted) {
            final String uninstall_command = getUninstallLibsCommand(file);
            command = combineCommands(command, uninstall_command);
        }

        final String label = process_descriptor.getLabel();
        final OutputStream output_stream = process_descriptor.getOutputStream();
        final OutputStream error_stream = process_descriptor.getErrorStream();
        final IStreamProcessor output_processor = process_descriptor.getOutputProcessor();
        final IStreamProcessor error_processor = process_descriptor.getErrorProcessor();
        final TimeoutExecutor timeout_executor = process_descriptor.getExecutor();

        // Get streams that process the output and error streams.
        final OutputStream intercept_output_stream = makeInterceptStream(output_stream, output_processor);
        final OutputStream intercept_error_stream = discard_errors ? null : makeInterceptStream(error_stream, error_processor);

        // Construct the action to run the command in a new thread.
        final Callable<SSH2ConsoleRemote> run_command_action = makeRunCommandAction(host_descriptor.debug(), command, intercept_output_stream, intercept_error_stream);

        try {
            // Run the action subject to timeout.

            final SSH2ConsoleRemote console = timeout_executor.executeWithTimeout(run_command_action);

            // Return a Java process handle that allows the remote process to be killed.
            return makeProcessForConsole(label, files_to_be_deleted, console);
        }
        catch (final Exception e) {
            launderException(e);
            return null;
        }
        catch (final Error t) {
            System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> caught throwable in process manager");
            throw t;
        }
    }

    private static void launderException(final Exception e) throws SSH2Exception, IOException, TimeoutException, InterruptedException {

        if (e instanceof ExecutionException) {
            launderException((Exception) e.getCause());
        }

        if (e instanceof InterruptedException) { throw (InterruptedException) e; }
        if (e instanceof SSH2Exception) { throw (SSH2Exception) e; }
        if (e instanceof IOException) { throw (IOException) e; }
        if (e instanceof TimeoutException) { throw (TimeoutException) e; }
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        throw new IllegalStateException("Unexpected checked exception", e);
    }

    /**
     * Creates a new Process object that wraps the given remote console. In particular it allows the remote process to be destroyed. Note that this method does not itself create a new remote process.
     * 
     * @param label a label that can be used to identify the remote process
     * @param console the remote SSH console
     * @return the new Process object
     */
    private Process makeProcessForConsole(final String label, final List<File> files_to_be_deleted, final SSH2ConsoleRemote console) {

        return new Process() {

            @Override
            public void destroy() {

                try {
                    killMatchingProcesses(label, files_to_be_deleted);
                }
                catch (final Exception e) {
                    Diagnostic.trace(DiagnosticLevel.RUN, "exception while trying to kill remote process: ", e.getMessage());
                }

                console.close();
            }

            @Override
            public int exitValue() {

                return waitFor();
            }

            @Override
            public InputStream getErrorStream() {

                return null;
            }

            @Override
            public InputStream getInputStream() {

                return console.getStdOut();
            }

            @Override
            public OutputStream getOutputStream() {

                return console.getStdIn();
            }

            @Override
            public int waitFor() {

                try {
                    return console.waitForExitStatus();
                }
                catch (final NullPointerException e) {
                    // Allow for Mindterm bug.
                    return -1;
                }
            }
        };
    }

    /**
     * Creates an action that executes a given remote command.
     * 
     * @param command the command to be executed
     * @param output_stream the stream to which output from the process should be directed
     * @param error_stream the stream to which error output from the process should be directed
     * @return the new action
     */
    private Callable<SSH2ConsoleRemote> makeRunCommandAction(final boolean debug, final String command, final OutputStream output_stream, final OutputStream error_stream) {

        return new Callable<SSH2ConsoleRemote>() {

            @Override
            public SSH2ConsoleRemote call() throws Exception {

                // Create the remote console to use for command execution.

                final SSH2Connection connection = getSSH2Client().getConnection();

                final SSH2ConsoleRemote console = new SSH2ConsoleRemote(connection);

                // Run the command (returns a boolean indicating success, ignored here).
                console.command(command, output_stream, error_stream);

                return console;
            }
        };
    }

    private synchronized SSH2SimpleClient getSSH2Client() throws SSH2Exception, IOException {

        if (client == null) {
            client = makeSSH2Client();
        }

        return client;
    }

    private SSH2SimpleClient makeSSH2Client() throws SSH2Exception, IOException {

        final Credentials credentials = host_descriptor.getCredentials();
        if (credentials == null) { throw new SSH2AccessDeniedException("No credentials for host descriptor: " + host_descriptor.getHost()); }

        if (host_descriptor.getInetAddress() == null) { throw new IOException("The host descriptor's InetAddress reference was null."); }
        final Socket socket = new Socket(host_descriptor.getInetAddress(), SSH_PORT);

        final byte[] seed = RandomSeed.getSystemStateHash();
        final SecureRandomAndPad secure_random = new SecureRandomAndPad(new SecureRandom(seed));
        final SSH2Transport transport = new SSH2Transport(socket, secure_random);

        return credentials.makeSSHClient(transport);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a path to a timestamped file or directory within the specified 'tmp' directory.
     * 
     * @param remote_temp_dir the 'tmp' directory
     * @param identifier 
     * @return the path
     */
    private String getTimestampedTempPath(final File remote_temp_dir, final String identifier) {

        final StringBuilder builder = new StringBuilder();

        builder.append(TEMP_FILES_ROOT);
        builder.append("_");
        builder.append(identifier);
        builder.append("_");
        builder.append(Duration.elapsed().getLength(TimeUnit.MILLISECONDS));

        return new File(remote_temp_dir, builder.toString()).getAbsolutePath();
    }

    /**
     * Don't use spaces in label.
     * 
     * @param label
     * @return
     * @throws UnknownPlatformException 
     */
    private String getKillCommand(final String label, final List<File> files_to_be_deleted) throws UnknownPlatformException {

        // Construct a shell command of this form:
        //
        // ps auxw | grep <match> | sed 's/^[a-z]*[ ]*\([0-9]*\).*/\1/' | tr '\n' ' ' | sed 's/^\(.*\)/kill -9 \1/' > /tmp/kill1; chmod +x /tmp/kill1; /tmp/kill1

        final PlatformDescriptor platform_descriptor = host_descriptor.getPlatform();
        final String temp_file_path = getTimestampedTempPath(new File(platform_descriptor.getTempPath()), "kill");

        final StringBuilder string_builder = new StringBuilder();
        final PipedCommandBuilder command = new PipedCommandBuilder(string_builder);

        // List all processes.
        command.append("ps auxw");

        // Find those containing the specified string.
        command.append("grep \"" + label + "\"");

        // Get the PIDS, assuming each is preceded by a username and whitespace.
        command.append("sed 's/^[a-z]*[ ]*\\([0-9]*\\).*/\\1/'");

        // Join the PIDs onto a single line.
        command.append("tr '\\n' ' '");

        // Construct a command to kill them, discarding error output.
        command.append("sed 's/^\\(.*\\)/kill -9 \\1 2> \\/dev\\/null/' > " + temp_file_path);

        string_builder.append("; chmod +x ");
        string_builder.append(temp_file_path);
        string_builder.append("; ");
        string_builder.append(temp_file_path);

        string_builder.append("; rm -rf ");
        string_builder.append(temp_file_path);

        for (final File file_to_be_deleted : files_to_be_deleted) {
            string_builder.append("; rm -rf ");
            string_builder.append(file_to_be_deleted.getAbsolutePath());
        }

        return string_builder.toString();
    }

    private String getClearTempFilesCommand() throws UnknownPlatformException {

        final PlatformDescriptor platform = host_descriptor.getPlatform();
        return "rm -rf " + platform.getTempPath() + platform.getFileSeparator() + TEMP_FILES_ROOT + "*";
    }

    /**
     * Creates a shell command to invoke the Java interpreter with the specified class and arguments on the specified platform, using the specified class path.
     * 
     * @param jvm_params JVM parameters
     * @param clazz the class to be executed
     * @param args the command line arguments
     * @return a shell command to execute the class
     * @throws FileNotFoundException if the Java interpreter executable cannot be found locally
     * @throws UnknownPlatformException if the local platform cannot be determined
     */
    private String getJavaCommand(final List<String> jvm_params, final Class<?> clazz, final List<String> args) throws FileNotFoundException, UnknownPlatformException {

        // TODO add VM args to host descriptor to support remote debugging flags.
        final StringBuilder buffer = new StringBuilder();

        final PlatformDescriptor platform_descriptor = host_descriptor.getPlatform();
        final File java_bin_path = host_descriptor.getJavaBinPath();
        final File java_library_path = host_descriptor.getJavaLibraryPath();
        final ClassPath class_path = host_descriptor.getClassPath();

        addJavaCommand(platform_descriptor, java_bin_path, buffer);
        addLibraryPath(java_library_path, buffer);
        addJVMArgs(jvm_params, buffer);
        addAssertionsFlag(buffer);
        addClassPath(platform_descriptor, class_path, buffer);
        addClassName(clazz, buffer);
        addJavaArgs(args, buffer);

        return escapeDollarSigns(buffer.toString());
    }

    private void addJavaArgs(final List<String> args, final StringBuilder buffer) {

        for (final String arg : args) {
            buffer.append(" ");
            buffer.append(arg);
        }
    }

    private void addJavaCommand(final PlatformDescriptor platform_descriptor, final File java_bin_path, final StringBuilder buffer) throws UnknownPlatformException {

        final String quote = platform_descriptor.getPathQuote();
        buffer.append(quote);
        if (java_bin_path != null) {
            buffer.append(java_bin_path.getAbsolutePath() + platform_descriptor.getFileSeparator());
        }
        buffer.append(PlatformDescriptor.JAVA_EXECUTABLE_NAME);

        buffer.append(quote);
    }

    private void addClassName(final Class<?> clazz, final StringBuilder buffer) {

        buffer.append(" ");
        buffer.append(clazz.getName());
    }

    private void addLibraryPath(final File java_library_path, final StringBuilder buffer) {

        if (java_library_path != null) {
            buffer.append(" -Djava.library.path=");
            buffer.append(java_library_path.getAbsolutePath());
        }
    }

    private void addJVMArgs(final List<String> jvm_params, final StringBuilder buffer) {

        if (jvm_params != null) {
            addJavaArgs(jvm_params, buffer);
        }
    }

    private void addClassPath(final PlatformDescriptor platform_descriptor, final ClassPath class_path, final StringBuilder buffer) {

        if (class_path.size() > 0) {
            buffer.append(" -cp");
            buffer.append(" ");
            buffer.append(class_path.toString(platform_descriptor));
        }
    }

    private void addAssertionsFlag(final StringBuilder buffer) {

        if (run_java_process_with_assertions_enabled) {
            buffer.append(" -ea");
        }
    }

    /**
     * Creates a thread that reads from one stream and writes to another.
     * 
     * @param command_stream the stream to be read from
     * @param print_stream the stream to be written to
     * @return the new thread
     */
    private static Thread makeReaderThread(final BufferedReader command_stream, final PrintStream print_stream) {

        final Thread newReaderThread = new Thread() {

            @Override
            public void run() {

                String s = null;
                try {
                    while ((s = command_stream.readLine()) != null && !interrupted()) {
                        print_stream.println(s);
                    }
                }
                catch (final IOException e) {
                    /* Ignore read errors; they mean process is done. */
                }
                finally {
                    try {
                        command_stream.close();
                    }
                    catch (final IOException e) {
                    }
                }
            }
        };

        newReaderThread.setName("Process Reader Thread " + READER_THREAD_COUNT.incrementAndGet()); // Name the reader thread to help differentiating between threads at the time of debug.
        readerThreads.add(newReaderThread);

        return newReaderThread;
    }

    /**
     * Concatenates a sequence of shell commands.
     * 
     * @param commands the commands
     * @return a composite command that executes the commands in sequence
     */
    private static String combineCommands(final String... commands) {

        final StringBuilder builder = new StringBuilder();
        for (final String command : commands) {

            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(command);
        }

        return builder.toString();
    }

    /**
     * Creates a shell command that downloads the specified files using wget.
     * 
     * @param lib_urls the URLs of the files to be downloaded
     * @param install_dir the directory to which the files should be downloaded
     * @param wget_path the path of the wget command
     * @return the composite command
     */
    private static String getInstallLibsCommand(final Set<URL> lib_urls, final File install_dir, final File wget_path) {

        String command = "";

        for (final URL lib_url : lib_urls) {

            final StringBuilder builder = new StringBuilder();
            builder.append(wget_path);
            builder.append(" ");
            builder.append(lib_url);

            if (lib_url.getProtocol().equalsIgnoreCase(URL.HTTPS_PROTOCOL_NAME)) { // Check whether the URL's protocol is HTTPS
                builder.append(" --no-check-certificate"); // Don't validate the server's certificate; this is to work around self-signed certificates.
            }

            builder.append(" -q --directory-prefix=");
            builder.append(install_dir.getAbsolutePath());

            command = combineCommands(command, builder.toString());
        }

        return command;
    }

    private static String getUninstallLibsCommand(final File install_dir) {

        return "rm -rf " + install_dir.getAbsolutePath();
    }

    /**
     * Creates a classpath containing the specified library files, assuming that they have been downloaded to the specified directory.
     * 
     * @param lib_urls the URLs of the library files
     * @param download_dir the directory to which the files have been downloaded
     * @return the classpath
     */
    private static ClassPath getClassPathForTempDir(final Set<URL> lib_urls, final File download_dir) {

        final ClassPath class_path = new ClassPath();

        for (final URL url : lib_urls) {
            class_path.add(0, new File(download_dir, new File(url.getPath()).getName()));
        }

        return class_path;
    }

    /**
     * Creates an output stream that processes bytes using the specified processor and passes further output to the specified stream.
     * 
     * @param stream OutputStream
     * @param processor IStreamProcessor
     * @return OutputStream
     */
    private static OutputStream makeInterceptStream(final OutputStream stream, final IStreamProcessor processor) {

        return new OutputStream() {

            @Override
            public void write(final int b) throws IOException {

                // Allow further processing if the processor says so, or if there is no processor.
                final boolean further_processing = processor == null || processor.processByte(b);

                if (further_processing && stream != null) {
                    stream.write(b);
                }
            }

            @Override
            public void close() throws IOException {

                // Do nothing.
            }

            @Override
            public void flush() throws IOException {

                stream.flush();
            }
        };
    }

    private static String escapeDollarSigns(final String command) {

        return command.replaceAll("\\$", "\\\\\\$");
    }

    /**
     * Stop any active reader threads created by this process manager.
     */
    public void stopReaderThreads() {

        for (final Thread readerThread : readerThreads) {
            readerThread.interrupt();
        }
    }
}
