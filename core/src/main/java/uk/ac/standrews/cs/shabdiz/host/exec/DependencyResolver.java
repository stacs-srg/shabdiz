package uk.ac.standrews.cs.shabdiz.host.exec;

/**
 * Presents a mechanism for resolving dependencies.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
interface DependencyResolver {

    /**
     * Resolves dependencies in a {@link ClassLoader}.
     *
     * @return the class loader that is aware of the given dependency
     */
    ClassLoader resolve() throws Exception;
}
