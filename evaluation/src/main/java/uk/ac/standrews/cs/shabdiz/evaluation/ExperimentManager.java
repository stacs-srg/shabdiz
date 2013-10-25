package uk.ac.standrews.cs.shabdiz.evaluation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class ExperimentManager extends AbstractApplicationManager {

    protected static final AttributeKey<InetSocketAddress> ADDRESS_KEY = new AttributeKey<InetSocketAddress>();
    protected static final AttributeKey<Process> PROCESS_KEY = new AttributeKey<Process>();
    protected static final AttributeKey<Integer> PID_KEY = new AttributeKey<Integer>();
    static final Duration PROCESS_START_TIMEOUT = new Duration(5, TimeUnit.MINUTES);
    static final boolean OVERRIDE_FILES_IN_WARN = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentManager.class);
    private static final Duration FILE_UPLOAD_TIMEOUT = new Duration(5, TimeUnit.MINUTES);
    private static final Duration CACHE_DELETION_TIMEOUT = new Duration(5, TimeUnit.MINUTES);
    private static final Duration DEFAULT_STATE_PROBE_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    protected final AgentBasedJavaProcessBuilder process_builder = new AgentBasedJavaProcessBuilder();
    protected final MavenDependencyResolver resolver = new MavenDependencyResolver();
    private final Class<?> main_class;

    protected ExperimentManager(Class<?> main_class) {

        this(DEFAULT_STATE_PROBE_TIMEOUT, main_class);
    }

    protected ExperimentManager(final Duration command_execution_timeout, Class<?> main_class) {

        super(command_execution_timeout);
        this.main_class = main_class;
        process_builder.setMainClass(main_class);
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            killByProcessID(descriptor);
        }
        finally {
            destroyProcess(descriptor);
        }
    }

    protected Properties getPropertiesFromProcess(final Process process) throws Exception {

        try {
            return Bootstrap.readProperties(main_class, process, PROCESS_START_TIMEOUT);
        }
        catch (final Exception e) {
            LOGGER.error("failed to read properties of " + main_class + " process", e);
            process.destroy();
            throw e;
        }
    }

    protected static void killByProcessID(final ApplicationDescriptor descriptor) throws IOException, InterruptedException {

        final Integer pid = descriptor.getAttribute(PID_KEY);
        if (pid != null) {
            final Host host = descriptor.getHost();
            ProcessUtil.killProcessOnHostByPID(host, pid);
        }
    }

    protected static void destroyProcess(final ApplicationDescriptor descriptor) {

        final Process process = descriptor.getAttribute(PROCESS_KEY);
        if (process != null) {
            process.destroy();
        }
    }

    protected void configure(ApplicationNetwork network) throws Exception {

        LOGGER.info("configuring manager {} for network {}", this, network.getApplicationName());
    }

    protected void uploadToAllHosts(ApplicationNetwork network, final List<File> files, final String destination, final boolean override) throws IOException, InterruptedException, TimeoutException, ExecutionException {

        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            final List<ListenableFuture<Void>> future_uploads = new ArrayList<ListenableFuture<Void>>();

            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                final ListenableFuture<Void> future = executor.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        final String host_name = host.getName();
                        LOGGER.info("uploading files to {}", host_name);
                        try {
                            uploadToHost(host, destination, override, files);
                            LOGGER.info("done uploading files to {}", host_name);
                        }
                        catch (Exception e) {
                            LOGGER.error("failed to upload files to " + host_name, e);
                            throw e;
                        }
                        return null;
                    }
                });
                future_uploads.add(future);
            }

            Futures.allAsList(future_uploads).get(FILE_UPLOAD_TIMEOUT.getLength(), FILE_UPLOAD_TIMEOUT.getTimeUnit());
        }
        finally {
            executor.shutdownNow();
        }

    }

    protected void clearCachedShabdizFilesOnAllHosts(ApplicationNetwork network) throws IOException, InterruptedException, TimeoutException, ExecutionException {

        LOGGER.info("Attemting to remove all shabdiz cached files on {} hosts", network.size());
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            final List<ListenableFuture<Void>> future_deletions = new ArrayList<ListenableFuture<Void>>();

            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                final ListenableFuture<Void> future = executor.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        final String host_name = host.getName();
                        LOGGER.info("removing shabdiz cached files on {}", host_name);
                        try {
                            AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
                            LOGGER.info("done removing shabdiz cached files on {}", host_name);
                        }
                        catch (Exception e) {
                            LOGGER.error("failed to remove shabdiz cached files on " + host_name, e);
                            throw e;
                        }
                        return null;
                    }
                });
                future_deletions.add(future);
            }

            Futures.allAsList(future_deletions).get(CACHE_DELETION_TIMEOUT.getLength(), CACHE_DELETION_TIMEOUT.getTimeUnit());
        }
        finally {
            executor.shutdownNow();
        }

    }

    protected void resolveMavenArtifactOnAllHosts(ApplicationNetwork network, final String artifact_coordinate) throws IOException, InterruptedException, TimeoutException, ExecutionException {

        LOGGER.info("Attemting to resolve {} on {} hosts", artifact_coordinate, network.size());
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            final List<ListenableFuture<Void>> future_resolutions = new ArrayList<ListenableFuture<Void>>();
            final AgentBasedJavaProcessBuilder mock_process_builder = new AgentBasedJavaProcessBuilder();
            mock_process_builder.addMavenDependency(artifact_coordinate);
            mock_process_builder.setMainClassName("main");

            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                final ListenableFuture<Void> future = executor.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        final String host_name = host.getName();
                        LOGGER.info("resolving {} on {}", artifact_coordinate, host_name);
                        try {
                            final Process start = mock_process_builder.start(host);
                            start.waitFor();
                            start.destroy();
                            LOGGER.info("done resolving {} on {}", artifact_coordinate, host_name);
                        }
                        catch (Exception e) {
                            LOGGER.error("failed to resolve " + artifact_coordinate + " on " + host_name, e);
                            throw e;
                        }
                        return null;
                    }
                });
                future_resolutions.add(future);
            }

            Futures.allAsList(future_resolutions).get(CACHE_DELETION_TIMEOUT.getLength(), CACHE_DELETION_TIMEOUT.getTimeUnit());
        }
        finally {
            executor.shutdownNow();
        }

    }

    private void uploadToHost(final Host host, final String destination, final boolean override, final List<File> files) throws IOException, InterruptedException {

        final boolean already_exists;
        final Platform platform = host.getPlatform();
        if (!override) {
            final Process exists = host.execute(Commands.EXISTS.get(platform, destination));
            already_exists = Boolean.valueOf(ProcessUtil.awaitNormalTerminationAndGetOutput(exists));
        }
        else {
            already_exists = false;
        }

        if (!already_exists) {
            final String delete_destination = Commands.DELETE_RECURSIVELY.get(platform, destination);
            final String mkdir_destination = Commands.MAKE_DIRECTORIES.get(platform, destination);
            final String delete_and_make = Commands.APPENDER.get(platform, delete_destination, mkdir_destination);
            final Process rm_mkdir_process = host.execute(delete_and_make);
            try {
                rm_mkdir_process.waitFor();
            }
            finally {
                rm_mkdir_process.destroy();
            }
            host.upload(files, destination);
        }
    }

    protected void configureMavenBased(ApplicationNetwork network, boolean cold, String artifact_coordinate) throws InterruptedException, IOException {

        if (cold) {
            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
            }
        }
        else {
            final AgentBasedJavaProcessBuilder mock_process_builder = new AgentBasedJavaProcessBuilder();
            mock_process_builder.addMavenDependency(artifact_coordinate);
            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                final Process start = mock_process_builder.start(host);
                start.waitFor();
                start.destroy();
            }
        }
        process_builder.addMavenDependency(artifact_coordinate);
    }

}
