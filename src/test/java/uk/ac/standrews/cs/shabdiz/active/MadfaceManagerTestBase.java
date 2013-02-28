/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;

import uk.ac.standrews.cs.shabdiz.zold.Configuration;
import uk.ac.standrews.cs.shabdiz.zold.MadfaceManagerFactory;
import uk.ac.standrews.cs.shabdiz.zold.ParameterValue;
import uk.ac.standrews.cs.shabdiz.zold.api.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.zold.util.URL;

/**
 * Common to several test classes.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class MadfaceManagerTestBase {

    protected MadfaceManager manager;
    protected Set<URL> application_urls;
    protected Configuration configuration;

    /**
     * Set-up.
     * 
     * @throws Exception if set-up fails
     */
    @Before
    public void setup() throws Exception {

        configuration = new Configuration();
        configuration.addParameter(new ParameterValue(Configuration.STATE_WAIT_DELAY_KEY, 1));
        configuration.addParameter(new ParameterValue(Configuration.SCANNER_MIN_CYCLE_TIME_KEY, 1));
        manager = MadfaceManagerFactory.DEFAULT_MADFACE_MANAGER_FACTORY.newMadfaceManager(configuration);
        application_urls = new HashSet<URL>();
    }

    /**
     * Tear-down.
     * 
     * @throws Exception if tear-down fails
     */
    @After
    public void teardown() throws Exception {

        manager.shutdown();
        manager = null;
    }

    protected void configureManager() throws IOException, InstantiationException, IllegalAccessException {

        final TestAppManager appManager = new TestAppManager();
        appManager.setApplicationLibraryURLs(application_urls);
        manager.setApplicationManager(appManager);
        manager.setHostScanning(true);
        manager.setAutoDeploy(false);
    }
}
