package uk.ac.standrews.cs.shabdiz.example.cluster_monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.job.JobRemote;
import uk.ac.standrews.cs.shabdiz.job.Worker;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;
import uk.ac.standrews.cs.shabdiz.job.util.SerializableVoid;
import uk.ac.standrews.cs.shabdiz.util.Input;

public class Main {

    private static final char[] EMPTY_PASSWORD = new String().toCharArray();
    private static final String BLUB_NODE_NAME_PREFIX = "compute-0-";
    private static final int BLUB_NODE_COUNT = 48;
    private static final String BLUB_HEAD_NODE_HOST_NAME = "blub.cs.st-andrews.ac.uk";

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(final String[] args) throws Exception {

        final WorkerNetwork worker_network = new WorkerNetwork();
        try {
            final SSHPublicKeyCredentials credentials = SSHPublicKeyCredentials.getDefaultRSACredentials(Input.readPassword("Please enter the public key passphrase"));
            final SSHHost blub_head = new SSHHost(BLUB_HEAD_NODE_HOST_NAME, credentials);
            worker_network.add(blub_head);
            worker_network.deployAll();
            worker_network.awaitAnyOfStates(ApplicationState.RUNNING);

            final ApplicationDescriptor worker_descriptor = worker_network.first();
            final ExecutorService executorService = Executors.newCachedThreadPool();
            for (final Process process : worker_descriptor.getProcesses()) {
                executorService.submit(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                            }
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                executorService.submit(new Runnable() {

                    @Override
                    public void run() {

                        try {
                            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                            }
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            final Worker worker = worker_descriptor.getApplicationReference();
            LOGGER.info("Address of the deployed worker on the cluster head node: {}", worker.getAddress());
            LOGGER.info("submitting the cluster network starter job to the head node");
            final Future<SerializableVoid> submit = worker.submit(new DeployMonitorNetworkJob());
            LOGGER.info("Awaiting job completion");
            submit.get();
        }
        finally {
            worker_network.shutdown();
        }
    }

    public static class DeployMonitorNetworkJob implements JobRemote<SerializableVoid> {

        @Override
        public SerializableVoid call() throws Exception {

            final ApplicationNetwork network = new ApplicationNetwork("monitor network");
            final ClusterMonitorManager manager = new ClusterMonitorManager();
            final LogNewAndOldPropertyListener listener = new LogNewAndOldPropertyListener();
            for (final Host host : getBlubNodes()) {
                final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);
                descriptor.addStateChangeListener(listener);
                network.add(descriptor);
            }
            network.awaitAnyOfStates(ApplicationState.AUTH);

            System.out.println(">>>>>>>>>>>>>>>>>>> ALL ARE IN AUTH");

            Thread.sleep(50000);
            return null;
        }

    }

    static List<SSHHost> getBlubNodes() throws IOException {

        final List<SSHHost> blub_nodes = new ArrayList<SSHHost>();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < BLUB_NODE_COUNT; i++) {
            builder.append(BLUB_NODE_NAME_PREFIX);
            builder.append(i);
            final SSHHost node = new SSHHost(builder.toString(), SSHPublicKeyCredentials.getDefaultRSACredentials(EMPTY_PASSWORD));
            blub_nodes.add(node);
            builder.setLength(0);
        }
        return blub_nodes;
    }
}
