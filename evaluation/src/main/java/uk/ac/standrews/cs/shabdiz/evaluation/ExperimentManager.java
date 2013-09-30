package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class ExperimentManager extends AbstractApplicationManager {

    protected static final AttributeKey<Process> PROCESS_KEY = new AttributeKey<Process>();
    protected static final AttributeKey<Integer> PID_KEY = new AttributeKey<Integer>();
    private static final Duration DEFAULT_STATE_PROBE_TIMEOUT = new Duration(15, TimeUnit.SECONDS);
    protected final AgentBasedJavaProcessBuilder process_builder = new AgentBasedJavaProcessBuilder();
    protected final MavenDependencyResolver resolver = new MavenDependencyResolver();

    protected ExperimentManager() {

        this(DEFAULT_STATE_PROBE_TIMEOUT);
    }

    protected ExperimentManager(final Duration command_execution_timeout) {

        super(command_execution_timeout);
    }

    protected abstract void configure(ApplicationNetwork network, boolean cold) throws Exception;

    protected void configureFileBased(ApplicationNetwork network, boolean cold, List<File> files, String application_name) throws Exception {

        if (cold) {
            for (File file : files) {
                process_builder.addFile(file);
            }
        }
        else {

            //FIXME this wont work if the platform is windows
            //TODO add parametric path on each host; maybe based on process environment variables?
            final String dependencies_home = "/tmp/" + application_name + "_dependencies";
            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                final Process exists = host.execute(Commands.EXISTS.get(host.getPlatform(), dependencies_home));
                final String already_exists = ProcessUtil.awaitNormalTerminationAndGetOutput(exists);
                if (!Boolean.valueOf(already_exists)) {
                    host.upload(files, dependencies_home);
                }
            }
            for (File file : files) {
                process_builder.addRemoteFile(dependencies_home + '/' + file.getName());
            }
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

    protected void configureURLBased(ApplicationNetwork network, boolean cold, List<URL> urls) {

        // TODO discuss cold url based with graham?
        // TODO check if JarFile can be a remote url instead of file if so the problem is solved. cold would be adding URL to URLClassloader
        for (URL url : urls) {
            process_builder.addURL(url);
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
}
