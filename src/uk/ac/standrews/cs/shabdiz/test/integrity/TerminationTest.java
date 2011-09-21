/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.test.integrity;

import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.impl.Launcher;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.test.util.TestJobRemoteFactory;

/**
 * The Class TerminationTest.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class TerminationTest {

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test(expected = RPCException.class)
    public void test() throws Exception {

        final Launcher launcher = new Launcher();

        final HostDescriptor host_descriptor = new HostDescriptor();
        host_descriptor.deployInLocalProcess(true);

        final IWorker worker = launcher.deployWorkerOnHost(host_descriptor);
        worker.submit(TestJobRemoteFactory.makeEchoJob("hello"));

        try {
            worker.shutdown();
        }
        catch (final RPCException e) {
            e.printStackTrace();
        }
        launcher.shutdown();

        System.out.println("Done");
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(final String[] args) throws Exception {

        final TerminationTest t = new TerminationTest();

        t.test();
    }
}
