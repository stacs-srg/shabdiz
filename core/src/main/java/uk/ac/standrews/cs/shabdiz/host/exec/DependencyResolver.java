package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.Serializable;

/**
 * Presents a mechanism for resolving dependencies that can be transferred to a remote host.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
interface DependencyResolver extends Serializable {

    /**
     * Resolves dependencies in a {@link ClassLoader}.
     *
     * @return the class loader that is aware of the given dependency
     */
    ClassLoader resolve() throws Exception;
}
