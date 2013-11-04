package uk.ac.standrews.cs.shabdiz.evaluation.util;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BlubPacketsInGangliaGauge extends BlubGangliaGauge<Float> {

    private static final String PACKETS_IN_METRIC_NAME = "pkts_in";

    public BlubPacketsInGangliaGauge() throws ParserConfigurationException, SAXException {

        super(PACKETS_IN_METRIC_NAME);
    }

    @Override
    public Float get() {

        return getAsFloat();
    }
}
