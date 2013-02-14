/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.nds.madface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.madface.exceptions.UnequalArrayLengthsException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.madface.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.nds.madface.interfaces.IStreamProcessor;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Describes a local or remote host and, optionally, SSH connection credentials and/or remote references to an application that is running or could run on it. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class HostDescriptor implements Comparable<HostDescriptor>, Cloneable {

    private static final int NUMBER_OF_BLUB_NODES = 60;

    private static final String PROBE_COMMAND = "uname";

    private static final String PROBE_REPLY_LINUX = "Linux";
    private static final String PROBE_REPLY_MAC = "Darwin";
    private static final String LOCAL_HOST = "localhost";

    private static String local_host = null;
    private static InetAddress local_inet_address = null;

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    private static final Map<String, String> JAVA_BIN_PATHS = new HashMap<String, String>();

    // -------------------------------------------------------------------------------------------------------

    static {
        try {
            local_inet_address = InetAddress.getLocalHost();
            local_host = local_inet_address.getHostName();

            initJavaBinPaths();
        }
        catch (final UnknownHostException e) {
            Diagnostic.trace(DiagnosticLevel.FULL, "No local IP address found");
        }
    }

    // -------------------------------------------------------------------------------------------------------

    // The host name.
    private volatile String host = null;

    // The host address.
    private volatile InetAddress inet_address = null;

    // The application port.
    private volatile int port = 0;

    // The host's platform.
    private volatile PlatformDescriptor platform_descriptor = null;

    // The classpath to be used by the application running on the host.
    private volatile ClassPath class_path = null;

    // The URLs of the library elements to be used by the application running on the host.
    private volatile Set<URL> application_urls = null;

    // The path of the Java executable on the host.
    private volatile File java_bin_path = null;

    // The Java library path on the host.
    private volatile File java_library_path = null;

    // Parameters for Java Virtual Machine deployment on the host.
    private volatile List<String> jvm_deployment_params;

    // Parameters for application deployment on the host.
    private volatile Object[] application_deployment_params;

    // The current view of the application state on the host.
    private volatile HostState host_state = HostState.UNKNOWN;

    // A reference to the application running on the host.
    private volatile Object application_reference = null;

    // A reference to the application process(es) running on the host.
    // This is a set because it's possible that multiple processes will be started, in the case that a
    // process is started but not detected before the next attempt is made.
    private final Set<Process> processes = Collections.synchronizedSet(new HashSet<Process>());

    // Map storing results of various scan operations.
    private volatile Map<String, String> scan_results = null;

    // Authentication credentials for the host.
    private volatile Credentials credentials = null;

    // Manager for executing processes on the host.
    private volatile ProcessManager process_manager = null;

    // Whether the application should be deployed in the same process.
    private volatile boolean deploy_in_local_process = false;

    // An identifier for debugging purposes.
    private int id;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initialises a descriptor for the local host.
     */
    public HostDescriptor() {

        this(null);
    }

    /**
     * Initialises a descriptor with a given address.
     * 
     * @param host the remote address
     */
    public HostDescriptor(final String host) {

        init(host);
    }

    /**
     * Initialises a descriptor using a host name and user credentials read from the user interactively. The password or passphrase is input using a Swing dialog to prevent it being displayed on the screen.
     * 
     * @param use_password true if password authentication should be used, false if public key authentication should be used
     * @throws IOException if an error occurs while reading user input
     */
    public HostDescriptor(final boolean use_password) throws IOException {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("enter machine name: ");
        final String host = reader.readLine();

        init(host);
        credentials(new Credentials(use_password));
    }

    // -------------------------------------------------------------------------------------------------------

    /*
     * The setter methods are written in the style returning 'this', to enable a set of attributes to be set succinctly. For example: new HostDescriptor().credentials(cred).javaBinPath(bin_path).javaLibraryPath(lib_path)
     */

    /**
     * Sets a remote reference to the application running on the remote host.
     * 
     * @param application_reference a remote reference to the application running on the remote host
     * @return this object
     */
    public HostDescriptor applicationReference(final Object application_reference) {

        this.application_reference = application_reference;
        return this;
    }

    /**
     * Sets the library elements used by the application running on the host. This should only be called once during this descriptor's lifetime.
     * 
     * @param application_urls the library elements used by the application running on the host
     * @return this object
     */
    public HostDescriptor applicationURLs(final Set<URL> application_urls) {

        this.application_urls = application_urls;
        return this;
    }

    /**
     * Sets the classpath used by the application running on the host.
     * 
     * @param class_path the classpath used by the application running on the host
     * @return this object
     */
    public HostDescriptor classPath(final ClassPath class_path) {

        this.class_path = class_path;
        return this;
    }

    /**
     * Sets the authentication credentials for the host.
     * 
     * @param credentials the authentication credentials for the host
     * @return this object
     */
    public HostDescriptor credentials(final Credentials credentials) {

        this.credentials = credentials;
        return this;
    }

    /**
     * Sets the state of the remote host and the application.
     * 
     * @param host_state the state of the remote host and the application
     * @return this object
     */
    public HostDescriptor hostState(final HostState host_state) {

        this.host_state = host_state;
        return this;
    }

    /**
     * Sets the path of the Java executable on the host.
     * 
     * @param java_bin_path the path of the Java executable on the host
     * @return this object
     */
    public HostDescriptor javaBinPath(final File java_bin_path) {

        this.java_bin_path = java_bin_path;
        return this;
    }

    /**
     * Sets the Java library path on the host.
     * 
     * @param java_library_path the Java library path on the host
     * @return this object
     */
    public HostDescriptor javaLibraryPath(final File java_library_path) {

        this.java_library_path = java_library_path;
        return this;
    }

    /**
     * Sets the JVM deployment parameters on the host. The given parameters have no effect if {@link #deployInLocalProcess(boolean)} is set to <code>true</code>.
     * 
     * @param jvm_deployment_params the JVM deployment parameters on the host
     * @return this object
     */
    public HostDescriptor jvmDeploymentParams(final List<String> jvm_deployment_params) {

        this.jvm_deployment_params = new ArrayList<String>(jvm_deployment_params);
        return this;
    }

    /**
     * Sets the application deployment parameters on the host.
     * 
     * @param application_deployment_params the application deployment parameters on the host
     * @return this object
     */
    public HostDescriptor applicationDeploymentParams(final Object[] application_deployment_params) {

        this.application_deployment_params = application_deployment_params.clone();
        return this;
    }

    /**
     * Sets the application port.
     * 
     * @param port the application port
     * @return this object
     */
    public HostDescriptor port(final int port) {

        this.port = port;
        return this;
    }

    /**
     * Sets a handle on the application process running on the remote host.
     * 
     * @param process a handle on the application process running on the remote host
     * @return this object
     */
    public synchronized HostDescriptor process(final Process process) {

        processes.add(process);
        return this;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns true if this descriptor represents the local host.
     * 
     * @return true if this descriptor represents the local host
     */
    public boolean local() {

        return local(host);
    }

    /**
     * Returns a remote reference to the application running on the remote host.
     * 
     * @return a remote reference to the application running on the remote host, or null if not set
     */
    public Object getApplicationReference() {

        return application_reference;
    }

    /**
     * Returns the URLs of the library elements used by the application running on the host.
     * 
     * @return the URLs of the library elements used by the application running on the host
     */
    public Set<URL> getApplicationURLs() {

        return application_urls;
    }

    /**
     * Returns the class path.
     * 
     * @return the class path
     */
    public synchronized ClassPath getClassPath() {

        if (class_path == null) {
            class_path = ClassPath.getCurrentClassPath();
        }

        return class_path;
    }

    /**
     * Returns the authentication credentials.
     * 
     * @return the authentication credentials
     */
    public Credentials getCredentials() {

        return credentials;
    }

    /**
     * Returns the network address.
     * 
     * @return the network address
     */
    public String getHost() {

        return host;
    }

    /**
     * Returns the state of the remote host and the application. Of course this is only an educated guess.
     * 
     * @return the state of the remote host and the application
     */
    public HostState getHostState() {

        return host_state;
    }

    /**
     * Returns the network address.
     * 
     * @return the network address
     */
    public InetAddress getInetAddress() {

        return inet_address;
    }

    /**
     * Returns the network address.
     * 
     * @return the network address
     * @throws UnknownHostException if the address cannot be resolved
     */
    public InetSocketAddress getInetSocketAddress() throws UnknownHostException {

        return NetworkUtil.getInetSocketAddress(host, port);
    }

    /**
     * Returns the path of the Java executable.
     * 
     * @return the path of the Java executable
     */
    public File getJavaBinPath() {

        return java_bin_path;
    }

    /**
     * Returns the Java library path.
     * 
     * @return the Java library path
     */
    public File getJavaLibraryPath() {

        return java_library_path;
    }

    /**
     * Returns the JVM deployment parameters on the host.
     * 
     * @return the JVM deployment parameters on the host
     */
    public List<String> getJVMDeploymentParams() {

        return jvm_deployment_params == null ? null : new CopyOnWriteArrayList<String>(jvm_deployment_params);
    }

    /**
     * Returns the application deployment parameters on the host.
     * 
     * @return the application deployment parameters on the host
     */
    public Object[] getApplicationDeploymentParams() {

        return application_deployment_params == null ? null : application_deployment_params.clone();
    }

    /**
     * Returns the network port.
     * 
     * @return the network port, or zero if not set
     */
    public int getPort() {

        return port;
    }

    /**
     * Returns the host's platform.
     * 
     * @return the host's platform
     */
    public synchronized PlatformDescriptor getPlatform() {

        if (platform_descriptor == null) {
            initPlatform();
        }
        return platform_descriptor;
    }

    /**
     * Returns a manager for executing processes on the host.
     * 
     * @return a manager for executing processes on the host
     */
    public ProcessManager getProcessManager() {

        return process_manager;
    }

    /**
     * Returns a map containing results recorded by application-specific scanners.
     * 
     * @return a map containing results recorded by application-specific scanners
     */
    public Map<String, String> getAttributes() {

        return scan_results;
    }

    /**
     * Kills all known application processes on the host represented by this host descriptor, and waits for completion.
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for application processes to die
     */
    public synchronized void killProcesses() throws InterruptedException {

        for (final Process process : processes) {

            process.destroy();
            process.waitFor();
        }

        processes.clear();
    }

    /**
     * Kills all application processes matching the given label on the host represented by this host descriptor.
     * 
     * @param label the label to be matched
     * @throws SSH2Exception if an SSH connection to a remote machine cannot be established
     * @throws IOException if an error occurs when reading output from the process
     * @throws TimeoutException if the remote process cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of a remote host cannot be established
     * @throws InterruptedException if the thread is interrupted while waiting for processes to die
     * @throws UnsupportedPlatformException
     */
    public void killProcesses(final String label) throws SSH2Exception, IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        process_manager.killMatchingProcesses(label);
    }

    /**
     * Returns the number of known application processes.
     * 
     * @return the number of known application processes
     */
    public int getNumberOfProcesses() {

        return processes.size();
    }

    /**
     * Returns the id.
     * 
     * @return the id
     */
    public int getId() {

        return id;
    }

    /**
     * Sets whether the application should be deployed in the local process.
     * 
     * @param deploy_in_local_process true if the application should be deployed in the local process
     */
    public void deployInLocalProcess(final boolean deploy_in_local_process) {

        this.deploy_in_local_process = deploy_in_local_process;
    }

    /**
     * Tests whether the application should be deployed in the local process.
     * 
     * @return true if the application should be deployed in the local process
     */
    public boolean deployInLocalProcess() {

        return deploy_in_local_process;
    }

    /**
     * Shuts down the process manager for this host descriptor.
     */
    public void shutdown() {

        process_manager.shutdown();
    }

    /**
     * Tests whether debugging is set for this host descriptor.
     * 
     * @return true if debugging is set for this host descriptor.
     */
    public boolean debug() {

        return false;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();

        builder.append("host: " + host + "\n");
        builder.append("port: " + port + "\n");
        builder.append("inet_address: " + inet_address + "\n");
        builder.append("platform_descriptor: " + platform_descriptor + "\n");
        builder.append("class_path: " + class_path + "\n");
        builder.append("host_state: " + getHostState() + "\n");
        builder.append("credentials: " + credentials + "\n");
        builder.append("id: " + id + "\n");
        builder.append("java_bin_path: " + java_bin_path + "\n");
        builder.append("local: " + local(host) + "\n");
        builder.append("application urls: " + setToString(application_urls) + "\n");

        return builder.toString();
    }

    private <T> String setToString(final Set<T> array) {

        final StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (final T element : array) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(element);
        }

        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object obj) {

        if (!(obj instanceof HostDescriptor)) { return false; }
        final HostDescriptor other = (HostDescriptor) obj;

        return host.equals(other.host) && id == other.id;
    }

    @Override
    public int hashCode() {

        return host.hashCode() + id;
    }

    @Override
    public int compareTo(final HostDescriptor other) {

        if (equals(other)) { return 0; }

        final int compare_hosts = host.compareTo(other.host);
        return compare_hosts != 0 ? compare_hosts : id < other.id ? -1 : 1;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a list of host descriptors from interactively entered username/password credentials for the specified hosts. Optionally the same credentials can be used for all hosts.
     * 
     * @param hosts the hosts
     * @param same_credentials_for_all true if the same credentials should be used for all hosts
     * @return a list of host descriptors
     * @throws IOException if an error occurs when trying to read in a username or password
     */
    public static SortedSet<HostDescriptor> createDescriptorsUsingPassword(final List<String> hosts, final boolean same_credentials_for_all) throws IOException {

        final SortedSet<HostDescriptor> connections = new ConcurrentSkipListSet<HostDescriptor>();

        final HostDescriptor first_connection = createUsernamePasswordConnection(hosts.get(0));
        connections.add(first_connection);

        for (int i = 1; i < hosts.size(); i++) {

            if (same_credentials_for_all) {
                connections.add(createConnection(hosts.get(i), first_connection.credentials));
            }
            else {
                connections.add(createUsernamePasswordConnection(hosts.get(i)));
            }
        }

        return connections;
    }

    /**
     * Creates a list of host descriptors from interactively entered username/public key credentials for the specified hosts. Optionally the same credentials can be used for all hosts.
     * 
     * @param hosts the hosts
     * @param same_credentials_for_all true if the same credentials should be used for all hosts
     * @return a list of hosts descriptors
     * @throws IOException if an error occurs when trying to read in a username or key passphrase
     */
    public static SortedSet<HostDescriptor> createDescriptorsUsingPublicKey(final List<String> hosts, final boolean same_credentials_for_all) throws IOException {

        final SortedSet<HostDescriptor> connections = new ConcurrentSkipListSet<HostDescriptor>();

        final HostDescriptor first_connection = createPublicKeyConnection(hosts.get(0));
        connections.add(first_connection);

        for (int i = 1; i < hosts.size(); i++) {

            if (same_credentials_for_all) {
                connections.add(createConnection(hosts.get(i), first_connection.credentials));
            }
            else {
                connections.add(createPublicKeyConnection(hosts.get(i)));
            }
        }

        return connections;
    }

    /**
     * Create a list of host descriptors from the credentials provided (credentials must be the same for each host).
     * @param hosts the hosts
     * @param credentials how to connect each host.
     * @return a list of hosts descriptors
     * @throws IOException if an error occurs when trying to read in a username or key passphrase
     */
    public static SortedSet<HostDescriptor> createDescriptorsUsingPublicKey(final List<String> hosts, final Credentials credentials) throws IOException {

        final SortedSet<HostDescriptor> connections = new ConcurrentSkipListSet<HostDescriptor>();

        for (int i = 0; i < hosts.size(); i++) {
            connections.add(createConnection(hosts.get(i), credentials));
        }

        return connections;
    }

    /**
     * Sets the class paths for a list of host descriptors.
     * 
     * @param host_descriptors the host descriptors
     * @param class_paths the class paths
     * @throws UnequalArrayLengthsException if the lists are not of equal length
     */
    public static void setClassPaths(final SortedSet<HostDescriptor> host_descriptors, final List<ClassPath> class_paths) throws UnequalArrayLengthsException {

        if (host_descriptors.size() != class_paths.size()) { throw new UnequalArrayLengthsException(); }

        int i = 0;
        for (final HostDescriptor host_descriptor : host_descriptors) {
            host_descriptor.classPath(class_paths.get(i++));
        }
    }

    /**
     * Sets the application URLs for a list of host descriptors.
     * 
     * @param host_descriptors the host descriptors
     * @param application_urls the URLs
     */
    public static void setApplicationURLs(final SortedSet<HostDescriptor> host_descriptors, final Set<URL> application_urls) {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            host_descriptor.applicationURLs(application_urls);
        }
    }

    private void init(final String host) {

        id = NEXT_ID.getAndIncrement();

        if (local(host)) {
            initLocal();
        }
        else {
            initRemote(host);
        }

        checkForHardwiredJavaBinPath();
    }

    private void initLocal() {

        host = local_host;
        inet_address = local_inet_address;

        scan_results = Collections.synchronizedMap(new HashMap<String, String>());
        process_manager = new ProcessManager(this);

        initLocalPlatform();
    }

    private void initRemote(final String host) {

        this.host = host;
        try {
            inet_address = InetAddress.getByName(host);
        }
        catch (final UnknownHostException e) {
            // Ignore, and allow for host_address being potentially null.
        }

        scan_results = Collections.synchronizedMap(new HashMap<String, String>());
        process_manager = new ProcessManager(this);

        hostState(HostState.UNKNOWN);
    }

    private void initPlatform() {

        if (local(host)) {
            initLocalPlatform();
        }
        else {
            initRemotePlatform();
        }
    }

    private synchronized void initLocalPlatform() {

        String platform_name = System.getProperty("os.name");

        if (platform_name.startsWith(PlatformDescriptor.NAME_WINDOWS)) {
            platform_name = PlatformDescriptor.NAME_WINDOWS;
        }

        if (platform_name.equals(PlatformDescriptor.NAME_MAC) || platform_name.equals(PlatformDescriptor.NAME_LINUX) || platform_name.equals(PlatformDescriptor.NAME_WINDOWS)) {

            final String file_separator = System.getProperty("file.separator");
            final String class_path_separator = System.getProperty("path.separator");
            final String temp_path = System.getProperty("java.io.tmpdir");

            try {
                platform_descriptor = new PlatformDescriptor(platform_name, file_separator, class_path_separator, temp_path);
            }
            catch (final UnknownPlatformException e) {
                ErrorHandling.hardExceptionError(e, "Unexpected unknown platform");
            }
        }
        else {
            platform_descriptor = new PlatformDescriptor();
        }
    }

    @Override
    public HostDescriptor clone() {

        return new HostDescriptor(host).credentials(credentials);
    }

    private void initRemotePlatform() {

        // Probe remote machine to discover OS.

        // Examples of expected replies:
        // Linux
        // Darwin

        final CountDownLatch latch = new CountDownLatch(1);
        final StringBuffer remote_reply_buffer = new StringBuffer();

        // Processor to read the output from the remote process and extract the platform name.
        final IStreamProcessor extract_platform = new IStreamProcessor() {

            @Override
            public boolean processByte(final int byte_value) {

                // Accumulate the output.
                remote_reply_buffer.append((char) byte_value);

                if (byte_value == '\n') {
                    latch.countDown();
                }

                return false;
            }
        };

        HostDescriptor cloned_host_descriptor = null;
        ProcessDescriptor probe_platform_process_descriptor = null;
        try {
            // Default is unknown platform.
            platform_descriptor = new PlatformDescriptor();

            // Clone the SSH connection to avoid interference with other use of this one.

            cloned_host_descriptor = clone();
            final TimeoutExecutor probe_process_executor = TimeoutExecutor.makeTimeoutExecutor(1, new Duration(20, TimeUnit.SECONDS), true, true, "HostDescriptor remote platform probe");
            probe_platform_process_descriptor = new ProcessDescriptor().command(PROBE_COMMAND).outputProcessor(extract_platform).executor(probe_process_executor);
            cloned_host_descriptor.getProcessManager().runProcess(probe_platform_process_descriptor);

            // Wait for the remote process to complete.
            latch.await();

            final String remote_reply = remote_reply_buffer.toString();

            if (remote_reply.contains(PROBE_REPLY_MAC)) {
                platform_descriptor = new PlatformDescriptor(PlatformDescriptor.NAME_MAC);
            }
            else if (remote_reply.contains(PROBE_REPLY_LINUX)) {
                platform_descriptor = new PlatformDescriptor(PlatformDescriptor.NAME_LINUX);
            }
            else {
                Diagnostic.trace("unexpected remote platform reply: " + remote_reply);
            }
        }
        catch (final Exception e) {
            launderException(e);
        }
        finally {
            if (cloned_host_descriptor != null) {
                // System.out.println("shutting down cloned host descriptor");
                cloned_host_descriptor.shutdown();
                // System.out.println("done");
            }
            if (probe_platform_process_descriptor != null) {
                // System.out.println("shutting down probe platform descriptor");
                probe_platform_process_descriptor.shutdown();
                // System.out.println("done");
            }
        }
    }

    private void launderException(final Exception e) {

        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        if (e instanceof InterruptedException || e instanceof TimeoutException || e instanceof IOException || e instanceof SSH2Exception || e instanceof UnknownPlatformException) {

            final StringBuilder builder = new StringBuilder();
            builder.append("exception determining remote platform: ");
            builder.append(host);
            builder.append(": ");
            builder.append(e.getClass().getName());
            builder.append(" : ");
            builder.append(e.getMessage());

            Diagnostic.trace(builder.toString());
            e.printStackTrace();
        }
        else {
            throw new IllegalStateException("Unexpected checked exception", e);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    private void checkForHardwiredJavaBinPath() {

        if (java_bin_path == null && host != null) {
            final String path = JAVA_BIN_PATHS.get(host);
            if (path != null) {
                javaBinPath(new File(path));
            }
        }
    }

    private static void initJavaBinPaths() {

        JAVA_BIN_PATHS.put("teaching-1.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-2.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-3.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-4.cs.st-andrews.ac.uk", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-1", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-2", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-3", "/usr/local/jdk/bin");
        JAVA_BIN_PATHS.put("teaching-4", "/usr/local/jdk/bin");

        for (int i = 0; i < NUMBER_OF_BLUB_NODES; i++) {
            JAVA_BIN_PATHS.put("compute-0-" + i, "/usr/java/latest/bin");
        }
    }

    private static HostDescriptor createUsernamePasswordConnection(final String host) throws IOException {

        final Credentials credentials = new Credentials(true);
        return new HostDescriptor(host).credentials(credentials);
    }

    private static HostDescriptor createPublicKeyConnection(final String host) throws IOException {

        final Credentials credentials = new Credentials(false);
        return new HostDescriptor(host).credentials(credentials);
    }

    private static HostDescriptor createConnection(final String host, final Credentials credentials) {

        return new HostDescriptor(host).credentials(credentials);
    }

    private static boolean local(final String host) {

        return host == null || host.equals("") || host.equals(LOCAL_HOST) || host.equals(local_host);
    }
}
