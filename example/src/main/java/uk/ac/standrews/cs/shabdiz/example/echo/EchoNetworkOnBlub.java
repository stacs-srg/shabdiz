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
package uk.ac.standrews.cs.shabdiz.example.echo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.job.JobRemote;
import uk.ac.standrews.cs.shabdiz.job.Worker;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;
import uk.ac.standrews.cs.shabdiz.job.util.SerializableVoid;

/**
 * Deploys a network of echo services on all the Blub nodes.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class EchoNetworkOnBlub {

    private static final char[] EMPTY_PASSWORD = new String().toCharArray();
    private static final String BLUB_NODE_NAME_PREFIX = "compute-0-";
    private static final int BLUB_NODE_COUNT = 48;
    private static final String BLUB_HEAD_NODE_HOST_NAME = "blub.cs.st-andrews.ac.uk";
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoNetworkOnBlub.class);

    private EchoNetworkOnBlub() {

    }

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

            final Worker worker = worker_network.first().getApplicationReference();
            LOGGER.info("Address of the deployed worker on the cluster head node: {}", worker.getAddress());
            LOGGER.info("submitting the cluster network starter job to the head node");
            final Future<SerializableVoid> submit = worker.submit(new EchoNetworkStarterOnBlub());
            LOGGER.info("Awaiting job completion");
            submit.get();
        }
        finally {
            worker_network.shutdown();
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

    /**
     * Starts a new network of echo services on all the blub nodes.
     * 
     * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
     */
    public static class EchoNetworkStarterOnBlub implements JobRemote<SerializableVoid> {

        private static final long serialVersionUID = -111492677244978456L;
        private static final Logger LOGGER = LoggerFactory.getLogger(EchoNetworkOnBlub.EchoNetworkStarterOnBlub.class);

        @Override
        public SerializableVoid call() {

            final EchoNetwork network = new EchoNetwork();
            try {

                final EchoApplicationManager application_manager = new EchoApplicationManager();
                final List<SSHHost> nodes = getBlubNodes();
                final LogNewAndOldPropertyListener listener = new LogNewAndOldPropertyListener();
                for (final SSHHost node : nodes) {
                    final EchoApplicationDescriptor descriptor = new EchoApplicationDescriptor(node, application_manager);
                    descriptor.addStateChangeListener(listener);
                    network.add(descriptor);
                }

                LOGGER.info("Deploying all...");
                network.deployAll();
                LOGGER.info("Awaiting RUNNING state...");
                network.awaitAnyOfStates(ApplicationState.RUNNING);
                LOGGER.info("All instances are in RUNNING state");

                LOGGER.info("About to kill all..");
                network.killAll();
                LOGGER.info("Awaiting AUTH state...");
                network.awaitAnyOfStates(ApplicationState.AUTH);
                LOGGER.info("All done");
            }
            catch (final Exception e) {
                LOGGER.error("failure occured on the cluster head node", e);
            }
            finally {
                LOGGER.info("Shutting down the network");
                network.shutdown();
            }

            return null; // void result
        }
    }
}
