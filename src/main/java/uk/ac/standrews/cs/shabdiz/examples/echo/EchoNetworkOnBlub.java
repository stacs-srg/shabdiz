/*
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
package uk.ac.standrews.cs.shabdiz.examples.echo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.jobs.JobRemote;
import uk.ac.standrews.cs.shabdiz.jobs.Worker;
import uk.ac.standrews.cs.shabdiz.jobs.WorkerNetwork;

public class EchoNetworkOnBlub {

    public static void main(final String[] args) throws Exception {

        final WorkerNetwork worker_network = new WorkerNetwork();
        final SSHPublicKeyCredentials credentials = SSHPublicKeyCredentials.getDefaultRSACredentials(Input.readPassword("Enter public key password"));
        final SSHHost blub_head = new SSHHost("blub.cs.st-andrews.ac.uk", credentials);
        worker_network.add(blub_head);
        worker_network.deployAll();
        worker_network.awaitAnyOfStates(ApplicationState.RUNNING);
        final Worker worker = worker_network.first().getApplicationReference();

        System.out.println(worker.getAddress());

        final Future<ArrayList<String>> submit = worker.submit(new EchoNetworkStarterOnBlub());
        final ArrayList<String> lines = submit.get();
        for (final String s : lines) {
            System.out.println("line " + s);
        }
        worker_network.killAll();
        worker_network.shutdown();
    }

    static List<SSHHost> getBlubNodes() throws IOException {

        final List<SSHHost> blub_nodes = new ArrayList<SSHHost>();
        for (int i = 0; i < 5; i++) {
            final SSHHost node = new SSHHost("compute-0-" + i, SSHPublicKeyCredentials.getDefaultRSACredentials(new String().toCharArray()));
            blub_nodes.add(node);
        }
        return blub_nodes;
    }

    public static class EchoNetworkStarterOnBlub implements JobRemote<ArrayList<String>> {

        private static final long serialVersionUID = -111492677244978456L;

        @Override
        public ArrayList<String> call() throws Exception {

            final ArrayList<String> report = new ArrayList<String>();
            final EchoNetwork network = new EchoNetwork();
            try {
                final PropertyChangeListener report_listener = new PropertyChangeListener() {

                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {

                        report.add("State of " + evt.getSource() + " changed from " + evt.getOldValue() + " to " + evt.getNewValue());

                    }
                };

                final EchoApplicationManager application_manager = new EchoApplicationManager();
                final List<SSHHost> nodes = getBlubNodes();

                for (final SSHHost node : nodes) {
                    final EchoApplicationDescriptor descriptor = new EchoApplicationDescriptor(node, application_manager);
                    descriptor.addStateChangeListener(report_listener);
                    network.add(descriptor);
                }

                report.add("Deploying all...");
                network.deployAll();
                report.add("Awaiting RUNNING state...");
                network.awaitAnyOfStates(ApplicationState.RUNNING);
                report.add("All instances are in RUNNING state");

                report.add("About to kill all..");
                network.killAll();
                report.add("Awaiting AUTH state...");
                network.awaitAnyOfStates(ApplicationState.AUTH);

                report.add("All done, shutting down");
            }
            finally {
                network.killAll();
                network.shutdown();
            }
            report.add("done");
            return report;
        }

    }
}
