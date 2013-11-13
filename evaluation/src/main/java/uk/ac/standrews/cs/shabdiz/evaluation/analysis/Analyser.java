package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.IOException;
import org.jfree.chart.JFreeChart;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
interface Analyser {

    String getName();

    JFreeChart getChart() throws IOException;
}
