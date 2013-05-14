/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

/**
 * Common to several test classes.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ApplicationNetworkTestBase {

    /**
     * Set-up.
     * 
     * @throws Exception if set-up fails
     */
    @Before
    public void setup() throws Exception {

    }

    /**
     * Tear-down.
     * 
     * @throws Exception if tear-down fails
     */
    @After
    public void teardown() throws Exception {

    }

    protected void configureManager() throws IOException, InstantiationException, IllegalAccessException {

    }
}
