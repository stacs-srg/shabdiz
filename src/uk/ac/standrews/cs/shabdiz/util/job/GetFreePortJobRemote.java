package uk.ac.standrews.cs.shabdiz.util.job;

import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;

/**
 * Finds a free port on the host that executes this job.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class GetFreePortJobRemote implements IJobRemote<Integer> {

    private static final long serialVersionUID = 162177514113121810L;

    @Override
    public Integer call() throws Exception {

        final int free_port = NetworkUtil.getFreePort();
        return Integer.valueOf(free_port);
    }
}
