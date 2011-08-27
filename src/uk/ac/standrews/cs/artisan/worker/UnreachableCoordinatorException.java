package uk.ac.standrews.cs.artisan.worker;

public class UnreachableCoordinatorException extends Exception {

    private static final long serialVersionUID = -6466964884327556885L;

    public UnreachableCoordinatorException(final Throwable cause) {

        super(cause);
    }
}
