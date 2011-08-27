package uk.ac.standrews.cs.shabdiz.interfaces;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Presents a computation to be performed on a remote worker.
 * 
 * @param <Result> the type of result returned by this job
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IRemoteJob<Result extends Serializable> extends Callable<Result>, Serializable {

}
