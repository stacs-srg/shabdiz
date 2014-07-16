package uk.ac.standrews.cs.shabdiz.evaluation.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AmazonEC2HostProvider implements Supplier<Host> {

    // see: http://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region
    private static final String EC2_WEST_EUROPE_ENDPOINT = "https://ec2.eu-west-1.amazonaws.com";
    private final AmazonEC2 ec2_client;
    private final Function<Instance, Host> instance_converter;
    private List<Instance> instances;
    private AtomicInteger next_host_index;

    public AmazonEC2HostProvider(final AWSCredentials credentials, Function<Instance, Host> instance_converter) {

        this(credentials, new ClientConfiguration(), EC2_WEST_EUROPE_ENDPOINT, instance_converter);
    }

    public AmazonEC2HostProvider(final AWSCredentials credentials, final ClientConfiguration configuration, String endpoint, Function<Instance, Host> instance_converter) {

        this.instance_converter = instance_converter;
        ec2_client = new AmazonEC2Client(credentials, configuration);
        ec2_client.setEndpoint(endpoint);
        next_host_index = new AtomicInteger();
    }

    @Override
    public synchronized Host get() {

        if (instances == null) {
            instances = getInstances();
        }

        final int host_index = next_host_index.getAndIncrement();
        return instance_converter.apply(instances.get(host_index));
    }

    private List<Instance> getInstances() {

        final List<Instance> all_instances = new ArrayList<>();
        final DescribeInstancesRequest request = new DescribeInstancesRequest();
        final DescribeInstancesResult result = ec2_client.describeInstances(request);
        final List<Reservation> reservations = result.getReservations();

        for (Reservation reservation : reservations) {
            List<Instance> instances = reservation.getInstances();
            all_instances.addAll(instances);
        }

        return all_instances;
    }
}
