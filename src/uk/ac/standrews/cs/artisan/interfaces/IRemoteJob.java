package uk.ac.standrews.cs.mcjob.interfaces;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * A job.
 * 
 * @param <Result> the type of result returned by this job
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface IRemoteJob<Result extends Serializable> extends Callable<Result>, Serializable {

}
