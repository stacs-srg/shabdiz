package uk.ac.standrews.cs.shabdiz.examples;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public final class PrintNewAndOldPropertyListener implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {

        System.out.println("State of " + evt.getSource() + " changed from " + evt.getOldValue() + " to " + evt.getNewValue());
    }
}