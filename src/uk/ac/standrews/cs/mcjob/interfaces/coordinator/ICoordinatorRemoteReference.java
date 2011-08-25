package uk.ac.standrews.cs.mcjob.interfaces.coordinator;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.madface.IPingable;

/**
 * The Class IMcJobRemoteReference.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface ICoordinatorRemoteReference extends IPingable {

    /**
     * Returns the address associated with this reference.
     *
     * @return the address associated with this reference
     */
    InetSocketAddress getCachedAddress();

    /**
     * Returns the remote reference.
     *
     * @return the remote reference
     */
    ICoordinatorRemote getRemote();
}
