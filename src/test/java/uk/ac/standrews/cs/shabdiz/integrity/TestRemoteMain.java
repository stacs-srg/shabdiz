package uk.ac.standrews.cs.shabdiz.integrity;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.credentials.SSHPasswordCredential;
import uk.ac.standrews.cs.shabdiz.host.AbstractHost;
import uk.ac.standrews.cs.shabdiz.host.RemoteSSHHost;
import uk.ac.standrews.cs.shabdiz.zold.DefaultLauncher;
import uk.ac.standrews.cs.shabdiz.zold.api.JobRemote;
import uk.ac.standrews.cs.shabdiz.zold.api.Launcher;
import uk.ac.standrews.cs.shabdiz.zold.api.Worker;
import uk.ac.standrews.cs.shabdiz.zold.util.ObjectStore;

public class TestRemoteMain {

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
        Launcher launcher = null;
        Worker worker = null;

        try {
            remoteHost = new RemoteSSHHost("localhost", new SSHPasswordCredential(Input.readPassword("Enter Password")));
            //            remoteHost = new LocalHost();
            launcher = new DefaultLauncher();
            worker = launcher.deployWorkerOnHost(remoteHost);

            System.out.println("submitting job to the remote worker");
            final Future<UUID> test_dir_job_result = worker.submit(new StoreTestDirectoryJob());

            System.out.println("Await Result");
            final UUID remotely_sotored_object_key = test_dir_job_result.get(20, TimeUnit.SECONDS);

            System.out.println("An object is stored in the remore JVM ObjectStore with the key of: " + remotely_sotored_object_key);
            System.out.println("submitting the second job to retrieve the associated value with the key");
            final Future<Boolean> file_test_result = worker.submit(new VerifyStoredTestDirJob(remotely_sotored_object_key));

            System.out.println("File test result " + file_test_result.get(20, TimeUnit.SECONDS));
            System.out.println("killing Shabdiz ");
        }
        finally {
            cleanUp(remoteHost, launcher, worker);
        }
    }

    private static void cleanUp(final Host remoteHost, final Launcher launcher, final Worker worker) {

        if (worker != null) {
            try {
                worker.shutdown();
            }
            catch (final RPCException e) {
                //ignore; expected
            }
        }
        if (launcher != null) {
            launcher.shutdown();
        }
        if (remoteHost != null) {
            remoteHost.shutdown();
        }
    }
}
