package uk.ac.standrews.cs.shabdiz.coordinator;

/**
 * Thrown if a coordinator has already deployed a set of hosts.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AlreadyDeployedException extends Exception {

    private static final long serialVersionUID = -5094378976253052411L;

    /**
     * Instantiates a new already deployed exception.
     */
    public AlreadyDeployedException() {

        super();
    }

    /**
     * Instantiates a new already deployed exception with a given message.
     *
     * @param message the message
     */
    public AlreadyDeployedException(final String message) {

        super(message);
    }
}
