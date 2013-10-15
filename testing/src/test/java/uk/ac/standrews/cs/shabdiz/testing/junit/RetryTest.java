package uk.ac.standrews.cs.shabdiz.testing.junit;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class RetryTest {

    private static final int SUCCESS_RETRY = 5;
    private static int current_retry;
    @Rule
    public Retry retry = new Retry(5);

    @Test
    public void testRetry() throws Exception {
        current_retry++;
        if (current_retry != SUCCESS_RETRY) {
            fail();
        }
        assertEquals(current_retry, retry.getCurrentRetryCount());
    }
}

