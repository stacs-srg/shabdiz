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
package uk.ac.standrews.cs.shabdiz.example.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link PropertyChangeListener} that logs the new and the old value of a changed property.
 * This class is typically used by the examples to demonstrate changes that are detected by Scanners.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class LogNewAndOldPropertyListener implements PropertyChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNewAndOldPropertyListener.class);

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {

        LOGGER.info("Property {} changed from {} to {}; Source: {}", evt.getPropertyName(), evt.getOldValue(), evt.getNewValue(), evt.getSource());
    }
}
