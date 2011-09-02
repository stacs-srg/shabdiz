/*
 * trombone Library
 * Copyright (C) 2010-2011 Distributed Systems Architecture Research Group
 * <http://asa.cs.st-andrews.ac.uk/>
 *
 * This file is part of trombone, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
 *
 * trombone is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with trombone.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <http://blogs.cs.st-andrews.ac.uk/trombone/>.
 */

package uk.ac.standrews.cs.shabdiz.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests run nightly.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
//    LocalRecoveryTestsSeparateProcesses.class
})
public class NightlyTests {

    // @Test public void testDummy() { }
}
