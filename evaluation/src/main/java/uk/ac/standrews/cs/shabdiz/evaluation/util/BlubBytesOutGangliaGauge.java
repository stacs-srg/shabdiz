package uk.ac.standrews.cs.shabdiz.evaluation.util;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BlubBytesOutGangliaGauge extends BlubGangliaGauge<Float> {

    private static final String BYTES_OUT_METRIC_NAME = "bytes_out";

    public BlubBytesOutGangliaGauge() throws ParserConfigurationException, SAXException {

        super(BYTES_OUT_METRIC_NAME);
    }

    @Override
    public Float get() {

        return getAsFloat();
    }
}
