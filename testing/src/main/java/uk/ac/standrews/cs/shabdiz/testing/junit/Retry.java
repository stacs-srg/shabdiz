package uk.ac.standrews.cs.shabdiz.testing.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class Retry implements TestRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(Retry.class);
    private final int max_retry_count;
    private int retry_count;

    public Retry(int max_retry_count) {
        this.max_retry_count = max_retry_count;
        if (hasMaxRetryCountReached()) { throw new IllegalArgumentException("max retry count must be at least 1"); }
    }

    @Override
    public Statement apply(final Statement statement, final Description description) {

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {

                boolean successful = false;
                Throwable latest_error = null;

                while (!Thread.currentThread().isInterrupted() && !successful && !hasMaxRetryCountReached()) {
                    retry_count++;
                    try {
                        statement.evaluate();
                        successful = true;
                        LOGGER.info("retry of {} succeeded within {} retry", description, retry_count);
                    }
                    catch (final Throwable error) {
                        latest_error = error;
                        successful = false;
                        LOGGER.error("retry of {} failed after {} retry", description, retry_count);
                        LOGGER.error("retry failure cause", error);
                        afterRetryFails();
                    }
                }

                if (!successful) {
                    throw latest_error != null ? latest_error : new InterruptedException();
                }
            }
        };
    }

    public int getCurrentRetryCount() {
        return retry_count;
    }

    protected void afterRetryFails() {

    }

    private boolean hasMaxRetryCountReached() {
        return retry_count >= max_retry_count;
    }
}
