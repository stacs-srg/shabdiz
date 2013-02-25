package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;

import uk.ac.standrews.cs.shabdiz.util.CredentialsUtil;


public abstract class Credentials {

    private final String username;

    protected Credentials() {

        this(CredentialsUtil.getCurrentUser());
    }

    protected Credentials(final String username) {

        this.username = username;
    }

    /**
     * Authenticates a given object.
     * 
     * @param obj the object to authenticate
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public abstract void authenticate(final Object obj) throws IOException;

    /**
     * Gets the specified username.
     * 
     * @return the username
     */
    public String getUsername() {

        return username;
    }
}
