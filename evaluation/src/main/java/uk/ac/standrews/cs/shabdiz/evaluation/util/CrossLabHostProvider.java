package uk.ac.standrews.cs.shabdiz.evaluation.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;

public class CrossLabHostProvider implements Provider<Host> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossLabHostProvider.class);
    private final List<String> host_names;
    private int index;

    public CrossLabHostProvider() {

        host_names = new ArrayList<String>();
    }

    public static void main(String[] args) {

        List<String> host_names = new ArrayList<String>();
        host_names.addAll(discoverReachableHosts("mac1-%03d-m.cs.st-andrews.ac.uk", 0, 200));
        host_names.addAll(discoverReachableHosts("pc1-%03d-l.cs.st-andrews.ac.uk", 0, 200));
        host_names.addAll(discoverReachableHosts("pc2-%03d-l.cs.st-andrews.ac.uk", 0, 200));

        System.out.println();
        for (String host_name : host_names) {
            System.out.println(host_name);
        }

        System.out.println();
        System.out.println("TOTAL: " + host_names.size());

    }

    @Override
    public synchronized Host get() {

        if (host_names.isEmpty()) {
            LOGGER.info("initializing host names..");
            host_names.addAll(discoverReachableHosts("compute-0-%d.local", 0, 50));
            host_names.addAll(discoverReachableHosts("mac1-%03d-m.cs.st-andrews.ac.uk", 0, 200));
            host_names.addAll(discoverReachableHosts("pc1-%03d-l.cs.st-andrews.ac.uk", 0, 200));
            host_names.addAll(discoverReachableHosts("pc2-%03d-l.cs.st-andrews.ac.uk", 0, 200));
            host_names.addAll(discoverReachableHosts("pc2-%03d-l.cs.st-andrews.ac.uk", 0, 200));
            LOGGER.info("initialized total of {} host names", host_names.size());
        }
        final int host_index = index++;
        try {
            final String host_name = host_names.get(host_index);
            LOGGER.info("initializing SSH host {}", host_name);
            final SSHHost host = new SSHHost(host_name, BlubHostProvider.SSHJ_AUTH);
            LOGGER.info("initialized SSH host {}", host_name);

            //            try {
            //                final Process execute = host.execute("killall java");
            //                execute.waitFor();
            //                execute.destroy();
            //            }
            //            catch (Exception e) {
            //                e.printStackTrace();
            //            }

            return host;
        }
        catch (IOException e) {
            LOGGER.error("failed to construct host with index: " + host_index, e);
            //            throw new RuntimeException(e);
            return get();
        }
    }

    private static Set<String> discoverReachableHosts(String host_name_format, int start_index, int end_index) {

        final Set<String> host_names = new ConcurrentSkipListSet<String>();
        final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(50));
        try {
            final List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>();
            for (int i = start_index; i <= end_index; i++) {
                final String host_name = String.format(host_name_format, i);
                final ListenableFuture<Void> submit = service.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        boolean reachable;
                        try {
                            reachable = InetAddress.getByName(host_name).isReachable(5000);
                        }
                        catch (IOException e) {
                            reachable = false;
                        }
                        if (reachable) {
                            host_names.add(host_name);
                        }
                        return null;
                    }
                });
                futures.add(submit);
            }
            Futures.allAsList(futures).get();
        }
        catch (Exception e) {
            LOGGER.error("failed to add predefined hosts", e);
            throw new RuntimeException(e);
        }
        finally {
            service.shutdownNow();
        }
        return host_names;
    }
}
