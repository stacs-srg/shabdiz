package uk.ac.standrews.cs.nds.madface;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.util.Input;

import com.mindbright.ssh2.SSH2Exception;

public class ProcessManagerConcurrency {

    public static void main(final String[] args) throws IOException, SSH2Exception, TimeoutException, InterruptedException {

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
                            final HostDescriptor host_descriptor = new HostDescriptor("compute-0-" + count).credentials(new Credentials().user(user).password(password));
                            final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, timeout, true, false, "");
                            start_latch.await();
                            final ProcessDescriptor process_descriptor = new ProcessDescriptor().command(command).executor(timeout_executor);
                            p = host_descriptor.getProcessManager().runProcess(process_descriptor);

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
