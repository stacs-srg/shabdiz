package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Stream;

import org.jfree.data.general.Dataset;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class OverlaidResourceConsumptionPerScannerInterval extends OverlaidCrossRepetitionPerPropertyAnalyzer {

    static final Comparator<String> STRING_DURATION_COMPARATOR = new Comparator<String>() {

        @Override
        public int compare(final String o1, final String o2) {

            return Duration.valueOf(o1).compareTo(Duration.valueOf(o2));
        }
    };

    private OverlaidResourceConsumptionPerScannerInterval(File results_path) throws IOException {

        super(results_path, Constants.SCANNER_INTERVAL_PROPERTY);
    }

    @Override
    public String getName() {

        return super.getName() + " per Scan Interval";
    }

    protected Comparator<String> getComparator() {

        return STRING_DURATION_COMPARATOR;
    }

    static OverlaidResourceConsumptionPerScannerInterval cpuUsage(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new CpuUsageAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval memoryUsage(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new MemoryUsageAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval threadCount(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new ThreadCountAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval systemLoadAverage(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new SystemLoadAverageAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval kilobytesOutPerSecond(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new KilobytesOutPerSecondAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval kilobytesInPerSecond(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new KilobytesInPerSecondAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval packetsOutPerSecond(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new PacketsOutPerSecondAnalyzer(file);
            }
        };
    }

    static OverlaidResourceConsumptionPerScannerInterval packetsInPerSecond(File results_path) throws IOException {

        return new OverlaidResourceConsumptionPerScannerInterval(results_path) {

            @Override
            protected GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file) {

                return new PacketsInPerSecondAnalyzer(file);
            }
        };
    }
}
