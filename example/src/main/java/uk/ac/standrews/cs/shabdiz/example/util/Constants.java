package uk.ac.standrews.cs.shabdiz.example.util;

/**
 * Provides a list of constants that are used in various examples.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Constants {

    /** Shabdiz Maven artifact ID. */
    public static final String SHABDIZ_EXAMPLES_ARTIFACT_ID = "example";
    /** Shabdiz Maven version. */
    public static final String SHABDIZ_VERSION = "2.0-SNAPSHOT";
    /** Shabdiz Maven group ID. */
    public static final String SHABDIZ_GROUP_ID = "uk.ac.standrews.cs.shabdiz";
    public static final String MAVEN_COORDINATE_SEPARATOR = ":";
    public static final String SHABDIZ_EXAMPLE_MAVEN_ARTIFACT_COORDINATES = SHABDIZ_GROUP_ID + MAVEN_COORDINATE_SEPARATOR + SHABDIZ_EXAMPLES_ARTIFACT_ID + MAVEN_COORDINATE_SEPARATOR + SHABDIZ_VERSION;

    private Constants() {

    }
}
