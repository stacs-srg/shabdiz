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
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class ExperimentManager extends AbstractApplicationManager {

    private static final Duration DEFAULT_STATE_PROBE_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    protected final AgentBasedJavaProcessBuilder process_builder = new AgentBasedJavaProcessBuilder();
    protected final MavenDependencyResolver resolver = new MavenDependencyResolver();

    protected ExperimentManager() {

        this(DEFAULT_STATE_PROBE_TIMEOUT);
    }

    protected ExperimentManager(final Duration command_execution_timeout) {

        super(command_execution_timeout);
    }

    protected abstract void configure(ApplicationNetwork network, boolean cold) throws Exception;

    protected void configureFileBased(ApplicationNetwork network, boolean cold, List<File> files) throws Exception {

        if (cold) {
            for (File file : files) {
                process_builder.addFile(file);
            }
        }
        else {

            //FIXME this wont work if the platform is windows
            //TODO add parametric path on each host; maybe based on process environment variables?
            final String dependencies_home = "/tmp/" + network.getApplicationName() + "_dependencies";
            for (ApplicationDescriptor descriptor : network) {
                final Host host = descriptor.getHost();
                host.upload(files, dependencies_home);
            }
            process_builder.addRemoteFile(dependencies_home + "/*");
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
                //TODO test if this works
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
}
