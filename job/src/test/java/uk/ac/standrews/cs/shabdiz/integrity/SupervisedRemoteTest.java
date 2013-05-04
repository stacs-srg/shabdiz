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
package uk.ac.standrews.cs.shabdiz.integrity;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.AbstractHost;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.job.JobRemote;
import uk.ac.standrews.cs.shabdiz.job.Worker;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;
import uk.ac.standrews.cs.shabdiz.job.util.ObjectStore;

public class SupervisedRemoteTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisedRemoteTest.class);

    private static final class VerifyStoredTestDirJob implements JobRemote<Boolean> {

        private final UUID key_of_job_result;
        private static final long serialVersionUID = -914929899418405919L;

        private VerifyStoredTestDirJob(final UUID key_of_job_result) {

            this.key_of_job_result = key_of_job_result;
        }

        @Override
        public Boolean call() throws Exception {

            final File temp_file = (File) ObjectStore.STORE.get(key_of_job_result);
            try {
                return temp_file.exists();
            }
            finally {
                temp_file.delete();
            }
        }
    }

    private static final class StoreTestDirectoryJob implements JobRemote<UUID> {

        private static final long serialVersionUID = 7824880921808363822L;

        @Override
        public UUID call() throws Exception {

            final File temp_file = File.createTempFile("test_shabdiz_temp", ".tmp");
            final UUID key = UUID.randomUUID();
            ObjectStore.STORE.put(key, temp_file);
            return key;
        }
    }

    public static void main(final String[] args) throws Exception {

        AbstractHost remoteHost = null;
        WorkerNetwork network = null;
        Worker worker = null;

        try {
            //            remoteHost = new SSHHost("beast.cs.st-andrews.ac.uk", SSHPublicKeyCredentials.getDefaultRSACredentials(Input.readPassword("Enter Password")));
            remoteHost = new SSHHost("project07.cs.st-andrews.ac.uk", SSHPublicKeyCredentials.getDefaultRSACredentials(Input.readPassword("Enter Password")));
            final Process worker_process = remoteHost.execute("echo $PATH");
            LOGGER.info("ERR: {}", IOUtils.toString(worker_process.getErrorStream()));
            LOGGER.info("OUT: {}", IOUtils.toString(worker_process.getInputStream()));

            //            remoteHost = new SSHHost("localhost", new SSHPasswordCredentials(Input.readPassword("Enter Password")));
            //            //            remoteHost = new LocalHost();
            network = new WorkerNetwork();
            network.add(remoteHost);
            network.deployAll();
            network.awaitAnyOfStates(ApplicationState.RUNNING);
            worker = network.iterator().next().getApplicationReference();

            System.out.println("submitting job to the remote worker");
            final Future<UUID> test_dir_job_result = worker.submit(new StoreTestDirectoryJob());

            System.out.println("Await Result");
            final UUID remotely_sotored_object_key = test_dir_job_result.get(20, TimeUnit.SECONDS);

            System.out.println("An object is stored in the remore JVM ObjectStore with the key of: " + remotely_sotored_object_key);
            System.out.println("submitting the second job to retrieve the associated value with the key");
            final Future<Boolean> file_test_result = worker.submit(new VerifyStoredTestDirJob(remotely_sotored_object_key));

            System.out.println("File test result " + file_test_result.get(20, TimeUnit.SECONDS));
            System.out.println("killing network ");
        }
        finally {
            cleanUp(remoteHost, network, worker);
        }
    }

    private static void cleanUp(final Host remoteHost, final WorkerNetwork network, final Worker worker) {

        if (network != null) {
            try {
                network.killAll();
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
            network.shutdown();
        }
        if (remoteHost != null) {
            try {
                remoteHost.close();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
