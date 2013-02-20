package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.PasswordCredentials;

public class ProcessManagerConcurrency {

    public static void main(final String[] args) throws IOException,  TimeoutException, InterruptedException {

        final String command = "uname -a";

        final Duration timeout = new Duration(Integer.valueOf(Input.readLine("enter timeout in seconds: ")), TimeUnit.SECONDS);
        final String user = Input.readLine("enter user: ");
        final String password = Input.readMaskedLine("enter password: ");

        for (int number_of_hosts = 1; number_of_hosts <= 20; number_of_hosts++) {

            final CountDownLatch start_latch = new CountDownLatch(1);
            final CountDownLatch end_latch = new CountDownLatch(number_of_hosts);
            final CountDownLatch tidy_latch = new CountDownLatch(number_of_hosts);

            for (int i = 0; i < number_of_hosts; i++) {

                final int count = i;
                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Process p = null;
                        try {
                            final HostDescriptor host_descriptor = new HostDescriptor("compute-0-" + count, new PasswordCredentials(user, password.toCharArray()));
                            final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, timeout, true, false, "");
                            start_latch.await();
                            p = host_descriptor.getManagedHost().execute(command);

                            p.waitFor();
                        }
                        catch (final Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            end_latch.countDown();
                            if (p != null) {
                                p.destroy();
                            }
                            tidy_latch.countDown();
                        }
                    }
                }).start();
            }

            final Duration start = Duration.elapsed();
            start_latch.countDown();
            end_latch.await();
            final Duration elapsed = Duration.elapsed(start);
            tidy_latch.await();
            System.out.println("\nhosts: " + number_of_hosts);
            System.out.println("elapsed time: " + elapsed + "s");
            System.out.println("throughput: " + (double) number_of_hosts / (double) elapsed.getLength(TimeUnit.SECONDS) + "/s");
        }
        System.exit(0);
    }
}
