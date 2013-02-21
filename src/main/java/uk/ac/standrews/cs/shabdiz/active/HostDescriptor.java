/***************************************************************************
 * * nds Library * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group * University of St Andrews, Scotland * http://www-systems.cs.st-andrews.ac.uk/ * * This file is part of nds, a package of utility classes. * * nds is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation, either version 3 of the License, or * (at your option) any later version. * * nds is distributed in the
 * hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with nds. If not, see <http://www.gnu.org/licenses/>. * *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.rpc.interfaces.IPingable;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnequalArrayLengthsException;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.shabdiz.active.exceptions.UnsupportedPlatformException;
import uk.ac.standrews.cs.shabdiz.impl.Host;
import uk.ac.standrews.cs.shabdiz.impl.LocalHost;
import uk.ac.standrews.cs.shabdiz.impl.Platform;
import uk.ac.standrews.cs.shabdiz.impl.RemoteSSHHost;
import uk.ac.standrews.cs.shabdiz.util.CredentialsUtil;
import uk.ac.standrews.cs.shabdiz.util.URL;

/**
 * Describes a local or remote host and, optionally, SSH connection credentials and/or remote references to an application that is running or could run on it. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class HostDescriptor implements Comparable<HostDescriptor>, Cloneable {

    private static final String TEMP_FILES_ROOT = "madface";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    // -------------------------------------------------------------------------------------------------------

    // The host name.
    private volatile String host = null;

    // The host address.
    private volatile InetAddress address = null;

    // The application port.
    private volatile int port = 0;

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
    private volatile IPingable application_reference = null;

    // A reference to the application process(es) running on the host.
    // This is a set because it's possible that multiple processes will be started, in the case that a
    // process is started but not detected before the next attempt is made.
    private final Set<Process> processes = new ConcurrentSkipListSet<Process>();

    // Map storing results of various scan operations.
    private volatile Map<String, String> scan_results = null;

    // Authentication credentials for the host.
    private volatile Credentials credentials = null;

    private volatile Host managed_host = null;

    // Whether the application should be deployed in the same process.
    private volatile boolean deploy_in_local_process = false;

    // An identifier for debugging purposes.
    private int id;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initialises a descriptor for the local host.
     * 
     * @throws IOException
     */
    public HostDescriptor() throws IOException {

        this(null, null);
    }

    /**
     * Initialises a descriptor with a given address.
     * 
     * @param host the remote address
     * @throws IOException
     */
    public HostDescriptor(final String host, final Credentials credentials) throws IOException {

        credentials(credentials);
        init(host);
    }

    /**
     * Initialises a descriptor using a host name and user credentials read from the user interactively. The password or passphrase is input using a Swing dialog to prevent it being displayed on the screen.
     * 
     * @param use_password true if password authentication should be used, false if public key authentication should be used
     * @throws IOException if an error occurs while reading user input
     */
    public HostDescriptor(final boolean use_password) throws IOException {

        final String host = Input.readLine("enter machine name: ");

        credentials(CredentialsUtil.initCredentials(use_password));
        init(host);
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
    public HostDescriptor applicationReference(final IPingable application_reference) {

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

        return NetworkUtil.isValidLocalAddress(address);
    }

    /**
     * Returns a remote reference to the application running on the remote host.
     * 
     * @return a remote reference to the application running on the remote host, or null if not set
     */
    public IPingable getApplicationReference() {

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

        return address;
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
     * @throws IOException
     */
    public synchronized Platform getPlatform() throws IOException {

        return managed_host.getPlatform();
    }

    /**
     * Returns a manager for executing processes on the host.
     * 
     * @return a manager for executing processes on the host
     */
    public Host getManagedHost() {

        return managed_host;
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
    public void killMatchingProcesses(final String label) throws IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        killMatchingProcesses(label, new ArrayList<File>());
    }

    public void killMatchingProcesses(final String label, final List<File> files_to_be_deleted) throws IOException, UnknownPlatformException, InterruptedException {

        final String kill_command = getKillCommand(label, files_to_be_deleted);
        final Process kill_process = managed_host.execute(kill_command);
        kill_process.waitFor();
        kill_process.destroy();
    }

    public void clearTempFiles() throws IOException, UnknownPlatformException, TimeoutException, InterruptedException, UnsupportedPlatformException {

        final String clear_temp_files_command = getClearTempFilesCommand();
        final Process clear_temp_files_process = managed_host.execute(clear_temp_files_command);
        clear_temp_files_process.waitFor();
        clear_temp_files_process.destroy();
    }

    private String getClearTempFilesCommand() throws IOException {

        final Platform platform = getPlatform();
        return "rm -rf " + platform.getTempDirectory() + platform.getSeparator() + TEMP_FILES_ROOT + "*";
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

        managed_host.shutdown();
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
        builder.append("inet_address: " + address + "\n");
        try {
            builder.append("platform_descriptor: " + getPlatform() + "\n");
        }
        catch (final IOException e) {
            builder.append("platform_descriptor: UNDEFINED \n");
        }
        builder.append("class_path: " + class_path + "\n");
        builder.append("host_state: " + getHostState() + "\n");
        builder.append("credentials: " + credentials + "\n");
        builder.append("id: " + id + "\n");
        builder.append("java_bin_path: " + java_bin_path + "\n");
        builder.append("local: " + local() + "\n");
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
     * 
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

    private void init(final String host) throws IOException {

        id = NEXT_ID.getAndIncrement();

        address = InetAddress.getLocalHost();
        this.host = address.getHostName();

        if (NetworkUtil.isValidLocalAddress(address)) {
            initLocal();
        }
        else {
            initRemote(host, credentials);
        }

        checkForHardwiredJavaBinPath();
    }

    private void initLocal() throws IOException {

        scan_results = Collections.synchronizedMap(new HashMap<String, String>());
        managed_host = new LocalHost();
    }

    private void initRemote(final String host, final Credentials credentials) throws IOException {

        this.host = host;
        try {
            address = InetAddress.getByName(host);
        }
        catch (final UnknownHostException e) {
            // Ignore, and allow for host_address being potentially null.
        }

        scan_results = Collections.synchronizedMap(new HashMap<String, String>());
        managed_host = new RemoteSSHHost(host, credentials);

        hostState(HostState.UNKNOWN);
    }

    @Override
    public HostDescriptor clone() {

        try {
            return new HostDescriptor(host, credentials);
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkForHardwiredJavaBinPath() {

        if (java_bin_path == null && host != null) {
            final String path = ClassPath.JAVA_BIN_PATHS.get(host);
            if (path != null) {
                javaBinPath(new File(path));
            }
        }
    }

    private static HostDescriptor createUsernamePasswordConnection(final String host) throws IOException {

        final Credentials credentials = CredentialsUtil.initCredentials(true);
        return new HostDescriptor(host, credentials);
    }

    private static HostDescriptor createPublicKeyConnection(final String host) throws IOException {

        final Credentials credentials = CredentialsUtil.initCredentials(false);
        return new HostDescriptor(host, credentials);
    }

    private static HostDescriptor createConnection(final String host, final Credentials credentials) throws IOException {

        return new HostDescriptor(host, credentials);
    }

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

    private String getKillCommand(final String label, final List<File> files_to_be_deleted) throws IOException {

        // Construct a shell command of this form:
        //
        // ps auxw | grep <match> | sed 's/^[a-z]*[ ]*\([0-9]*\).*/\1/' | tr '\n' ' ' | sed 's/^\(.*\)/kill -9 \1/' > /tmp/kill1; chmod +x /tmp/kill1; /tmp/kill1

        final Platform platform_descriptor = getPlatform();
        final String temp_file_path = getTimestampedTempPath(new File(platform_descriptor.getTempDirectory()), "kill");

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
}
