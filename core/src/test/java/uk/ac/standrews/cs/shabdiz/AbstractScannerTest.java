package uk.ac.standrews.cs.shabdiz;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.Duration;

public class AbstractScannerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScannerTest.class);
    private static final String TEST_PROPERTY_NAME = "SomeProperty";
    private static final Integer TEST_PROPERTY_OLD_VALUE = null;
    private static final Integer TEST_PROPERTY_NEW_VALUE = 65536;
    private static final CountDownLatch PROPERTY_CHANGE_ASSERTION_LATCH = new CountDownLatch(1);
    private AbstractScanner scanner;
    private Duration delay = new Duration(1, TimeUnit.SECONDS);
    private Duration timeout = new Duration(10, TimeUnit.SECONDS);
    private PropertyChangeListener listener;
    private boolean enabled = false;

    @Before
    public void setUp() throws Exception {

        scanner = new AbstractScanner(delay, timeout, enabled) {

            @Override
            public void scan(final ApplicationNetwork network) {

                LOGGER.info("scanning {}", network);
            }
        };

        listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {

                Assert.assertEquals(TEST_PROPERTY_NAME, evt.getPropertyName());
                Assert.assertEquals(TEST_PROPERTY_OLD_VALUE, evt.getOldValue());
                Assert.assertEquals(TEST_PROPERTY_NEW_VALUE, evt.getNewValue());
                PROPERTY_CHANGE_ASSERTION_LATCH.countDown();
            }
        };
    }

    @Test
    public void testGetCycleDelay() throws Exception {

        Assert.assertEquals(delay, scanner.getCycleDelay());
    }

    @Test
    public void testGetScanTimeout() throws Exception {

        Assert.assertEquals(timeout, scanner.getScanTimeout());
    }

    @Test
    public void testIsEnabled() throws Exception {

        Assert.assertEquals(enabled, scanner.isEnabled());
    }

    @Test
    public void testSetEnabled() throws Exception {

        scanner.setEnabled(false);
        Assert.assertFalse(scanner.isEnabled());

        scanner.setEnabled(true);
        Assert.assertTrue(scanner.isEnabled());
    }

    @Test
    public void testAddPropertyChangeListener() throws Exception {

        scanner.addPropertyChangeListener(TEST_PROPERTY_NAME, listener);
        Assert.assertTrue(scanner.property_change_support.hasListeners(TEST_PROPERTY_NAME));
        Assert.assertTrue(Arrays.asList(scanner.property_change_support.getPropertyChangeListeners(TEST_PROPERTY_NAME)).contains(listener));
    }

    @Test
    public void testRemovePropertyChangeListener() throws Exception {

        testAddPropertyChangeListener();
        scanner.removePropertyChangeListener(TEST_PROPERTY_NAME, listener);
        Assert.assertFalse(scanner.property_change_support.hasListeners(TEST_PROPERTY_NAME));
        Assert.assertFalse(Arrays.asList(scanner.property_change_support.getPropertyChangeListeners(TEST_PROPERTY_NAME)).contains(listener));
    }

    @Test
    public void testFirePropertyChange() throws Exception {

        scanner.addPropertyChangeListener(TEST_PROPERTY_NAME, listener);
        scanner.firePropertyChange(TEST_PROPERTY_NAME, TEST_PROPERTY_OLD_VALUE, TEST_PROPERTY_NEW_VALUE);
        // The assertion is done in the listener; here we just wait for the listener to signal success
        PROPERTY_CHANGE_ASSERTION_LATCH.await();
    }
}
