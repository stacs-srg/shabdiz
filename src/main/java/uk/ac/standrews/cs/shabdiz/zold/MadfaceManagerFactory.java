/*
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

package uk.ac.standrews.cs.shabdiz.zold;

import uk.ac.standrews.cs.shabdiz.zold.api.MadfaceManager;

/**
 * A factory for creating {@link MadfaceManager} objects.
 */
public abstract class MadfaceManagerFactory {

    /** The single instance of {@link DefaultMadfaceManagerFactory}. */
    public static final DefaultMadfaceManagerFactory DEFAULT_MADFACE_MANAGER_FACTORY = new DefaultMadfaceManagerFactory();

    /**
     * New madface manager.
     * 
     * @return the madface manager
     */
    public abstract MadfaceManager newMadfaceManager();

    /**
     * The default factory for creating {@link MadfaceManager} objects.
     */
    public static final class DefaultMadfaceManagerFactory extends MadfaceManagerFactory {

        private DefaultMadfaceManagerFactory() {

        }

        /**
         * Returns a Madface manager.
         * 
         * @return a Madface manager.
         */
        @Override
        public DefaultMadfaceManager newMadfaceManager() {

            return new DefaultMadfaceManager();
        }

        /**
         * Returns a Madface manager.
         * 
         * @param configuration the configuration
         * @return a Madface manager.
         */
        public DefaultMadfaceManager newMadfaceManager(final Configuration configuration) {

            return new DefaultMadfaceManager(configuration);
        }

    }

}
