package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.platform.Platform;

/**
 * Tests {@link AbstractHost}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AbstractHostTest {

    private static final String LOCAL_HOST_NAME = "localhost";
    private static final String REMOTE_HOST_NAME = "bbc.co.uk";
    private AbstractHost local_host_by_name;
    private AbstractHost local_host_by_address;
    private AbstractHost remote_host_by_name;
    private AbstractHost remote_host_by_address;
    private InetAddress local_host_address;
    private InetAddress remote_host_address;

    @Before
    public void setUp() throws Exception {

        local_host_address = InetAddress.getLocalHost();
        remote_host_address = InetAddress.getByName(REMOTE_HOST_NAME);

        local_host_by_name = new MockAbstractHost(LOCAL_HOST_NAME);
        local_host_by_address = new MockAbstractHost(local_host_address);
        remote_host_by_name = new MockAbstractHost(REMOTE_HOST_NAME);
        remote_host_by_address = new MockAbstractHost(remote_host_address);
    }

    @After
    public void tearDown() throws Exception {

        local_host_by_name.close();
        local_host_by_address.close();
        remote_host_by_name.close();
        remote_host_by_address.close();
    }

    @Test
    public void testGetAddress() throws Exception {

        Assert.assertEquals(InetAddress.getByName(LOCAL_HOST_NAME), local_host_by_name.getAddress());
        Assert.assertEquals(local_host_address, local_host_by_address.getAddress());
        Assert.assertEquals(remote_host_address, remote_host_by_name.getAddress());
        Assert.assertEquals(remote_host_address, remote_host_by_address.getAddress());
    }

    @Test
    public void testGetName() throws Exception {

        Assert.assertEquals(LOCAL_HOST_NAME, local_host_by_name.getName());
        Assert.assertEquals(local_host_address.getHostName(), local_host_by_address.getName());
        Assert.assertEquals(REMOTE_HOST_NAME, remote_host_by_name.getName());
        Assert.assertEquals(REMOTE_HOST_NAME, remote_host_by_address.getName());
    }

    @Test
    public void testIsLocal() throws Exception {

        Assert.assertTrue(local_host_by_name.isLocal());
        Assert.assertTrue(local_host_by_address.isLocal());
        Assert.assertFalse(remote_host_by_name.isLocal());
        Assert.assertFalse(remote_host_by_address.isLocal());
    }

    @Test
    public void testEquals() throws Exception {

        Assert.assertEquals(remote_host_by_name, remote_host_by_address);
        Assert.assertFalse(local_host_by_name.equals(local_host_by_address));
        Assert.assertEquals(new MockAbstractHost(InetAddress.getByName(LOCAL_HOST_NAME)), local_host_by_name);
        Assert.assertEquals(new MockAbstractHost(local_host_address.getHostName()), local_host_by_address);
    }

    private static class MockAbstractHost extends AbstractHost {

        protected MockAbstractHost(final String name) throws IOException {

            super(name);
        }

        private MockAbstractHost(final InetAddress address) {

            super(address);
        }

        @Override
        public void upload(final File source, final String destination) throws IOException {

            throw new UnsupportedOperationException();
        }

        @Override
        public void upload(final Collection<File> sources, final String destination) throws IOException {

            throw new UnsupportedOperationException();
        }

        @Override
        public void download(final String source, final File destination) throws IOException {

            throw new UnsupportedOperationException();
        }

        @Override
        public Process execute(final String command) throws IOException {

            throw new UnsupportedOperationException();
        }

        @Override
        public Process execute(final String working_directory, final String command) throws IOException {

            throw new UnsupportedOperationException();
        }

        @Override
        public Platform getPlatform() throws IOException {

            throw new UnsupportedOperationException();
        }
    }
}
