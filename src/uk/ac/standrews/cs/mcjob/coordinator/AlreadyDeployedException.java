package uk.ac.standrews.cs.mcjob.coordinator;

public class AlreadyDeployedException extends Exception {

    public AlreadyDeployedException() {

        super();
    }

    public AlreadyDeployedException(final String message) {

        super(message);
    }

}
