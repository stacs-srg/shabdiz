package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.WordUtils;
import org.mashti.sina.distribution.statistic.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.ift.LongCellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;
import uk.ac.standrews.cs.shabdiz.util.ArrayUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
final class AnalyticsUtil {

    public static final Integer[] GAUGE_CSV_COLUMN_INDICES = new Integer[]{0, 1};
    public static final String GROUP_DELIMITER = "@";
    static final long ONE_SECOND_IN_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
    static final NumberConverter NANOSECOND_TO_SECOND = new NanosecondToSecond();
    static final CellProcessor RELATIVE_TIME_IN_SECONDS_PROCESSOR = new ParseRelativeTime(new ConvertTime(TimeUnit.NANOSECONDS, TimeUnit.SECONDS));
    static final CellProcessor DOUBLE_PROCESSOR = new ParseDouble();
    static final CellProcessor[] DEFAULT_GAUGE_CSV_PROCESSORS = new CellProcessor[]{RELATIVE_TIME_IN_SECONDS_PROCESSOR, DOUBLE_PROCESSOR};
    private static final NumberConverter AS_IS_CONVERTER = new AsIsConverter();
    private static final FileFilterByName EXPERIMENT_PROPERTIES_FILE_FILTER = new FileFilterByName(Constants.PROPERTIES_FILE_NAME);
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsUtil.class);

    private AnalyticsUtil() {

    }

    
    static Statistics getPropertyStatistics(String property_key, Properties[] properties_collection) {

        return getPropertyStatistics(property_key, properties_collection, AS_IS_CONVERTER);
    }

    static Statistics getPropertyStatistics(String property_key, Properties[] properties_collection, NumberConverter converter) {

        final Statistics statistics = new Statistics();

        for (final Properties properties : properties_collection) {
            if (isSuccessful(properties)) {
                final String property_as_string = properties.getProperty(property_key);
                final Double property = Double.parseDouble(property_as_string);
                statistics.addSample(converter.convert(property));
            }
            else {
                LOGGER.warn("ignored unsuccessful properties {}", properties);
            }
        }

        return statistics;
    }

    static Map<String, Statistics> getPropertyStatistics(String property_key, Properties[] properties_collection, NumberConverter converter, String[] group_by_property) {

        final Map<String, Statistics> statistics_map = new LinkedHashMap<String, Statistics>();

        for (final Properties properties : properties_collection) {
            if (isSuccessful(properties)) {
                final String property_as_string = properties.getProperty(property_key);
                final Double property = Double.parseDouble(property_as_string);

                final StringBuilder key_builder = new StringBuilder();
                for (String group : group_by_property) {
                    key_builder.append(properties.getProperty(group));
                    key_builder.append(GROUP_DELIMITER);
                }

                final String key = key_builder.toString();

                final Statistics statistics;
                if (statistics_map.containsKey(key)) {
                    statistics = statistics_map.get(key);
                }
                else {
                    statistics = new Statistics();
                    statistics_map.put(key, statistics);
                }

                statistics.addSample(converter.convert(property));
            }
            else {
                LOGGER.warn("ignored unsuccessful properties {}", properties);
            }
        }
        return statistics_map;

    }

    static Properties[] getAllExperimentPropertiesInPath(File search_path) throws IOException {

        final Collection<File> files = FileUtils.listFiles(search_path, EXPERIMENT_PROPERTIES_FILE_FILTER, TrueFileFilter.INSTANCE);
        final int found_properties_count = files.size();
        final Properties[] found_properties = new Properties[found_properties_count];
        int i = 0;
        for (File file : files) {

            final Properties properties = new Properties();
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                properties.load(input);
            }
            finally {
                IOUtils.closeQuietly(input);
            }
            found_properties[i] = properties;
            i++;
        }
        return found_properties;
    }
    
    static Collection<File> getFilesByName(File search_path, String name) {

        return FileUtils.listFiles(search_path, new FileFilterByName(name), TrueFileFilter.INSTANCE);
    }

    static boolean isSuccessful(Properties properties) {

        final String status = properties.getProperty(Constants.EXPERIMENT_STATUS_PROPERTY, Constants.FAILURE);
        return status.equals(Constants.SUCCESS);
    }

    static Collection<Statistics[]> getCombinedGaugeCsvStatistics(final Collection<File> csv_files, CellProcessor[] processors) throws IOException {

        return getCombinedCsvStatistics(csv_files, processors, GAUGE_CSV_COLUMN_INDICES, true);
    }

    static Collection<Statistics[]> getCombinedCsvStatistics(final Collection<File> csv_files, CellProcessor[] processors, Integer[] column_indices, boolean header) throws IOException {

        final List<Statistics[]> point_statistics = new ArrayList<Statistics[]>();
        final List<CsvListReader> readers = getCsvListReaders(csv_files, header);

        while (!readers.isEmpty()) {

            final Iterator<CsvListReader> readers_iterator = readers.iterator();
            final Statistics[] combined_statistics = new Statistics[column_indices.length];
            while (readers_iterator.hasNext()) {
                final CsvListReader reader = readers_iterator.next();
                final List<Object> row = reader.read(processors);
                if (row != null) {

                    int i = 0;
                    for (Integer index : column_indices) {
                        if (combined_statistics[i] == null) {
                            combined_statistics[i] = new Statistics();
                        }
                        combined_statistics[i].addSample((Number) row.get(index));
                        i++;
                    }
                }
                else {
                    reader.close();
                    readers_iterator.remove();
                }
            }
            if (combined_statistics[0] != null) {
                point_statistics.add(combined_statistics);
            }
        }

        return point_statistics;
    }

    static Statistics getAgregatedCsvStatistic(final Collection<File> csv_files, CellProcessor[] processors, Integer column_index, boolean header) throws IOException {

        final Statistics statistics = new Statistics();
        for (File csv : csv_files) {
            final CsvListReader reader = getCsvListReader(csv, header);
            try {
                List<Object> row;
                while ((row = reader.read(processors)) != null) {
                    statistics.addSample((Number) row.get(column_index));
                }
            }
            finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return statistics;
    }

    private static List<CsvListReader> getCsvListReaders(final Collection<File> csv_files, final boolean header) throws IOException {

        final List<CsvListReader> readers = new ArrayList<CsvListReader>();
        for (File csv : csv_files) {

            final CsvListReader reader = getCsvListReader(csv, header);
            readers.add(reader);
        }
        return readers;
    }

    private static CsvListReader getCsvListReader(final File csv, boolean header) throws IOException {

        final CsvListReader reader = new CsvListReader(new FileReader(csv), CsvPreference.STANDARD_PREFERENCE);
        reader.getHeader(header);
        return reader;
    }

    static String decorateManagerAsApplicationName(final String manager) {

        final String tidy_manager_name = manager.replaceAll("_", " ").replaceAll("\\..*", "");
        return tidy_manager_name.contains("Chord") ? "Chord" : manager.contains("Echo") ? "Echo" : WordUtils.capitalize(tidy_manager_name);

    }

    static String decorateManagerAsDeploymentStrategy(final String manager) {

        return splitCamelCase(manager.substring(manager.indexOf(".") + 1));

    }

    static String splitCamelCase(String s) {

        return s.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
    }

    static String decoratePoolSize(final String pool_size) {

        final int size = Integer.parseInt(pool_size);
        return size != Integer.MAX_VALUE ? pool_size : "Maximum";
    }

    static String decorateKillPortion(final String kill_portion) {

        return kill_portion + "%";
    }

    static File[] listSubDirectoriesExcluding(final File results_path, final String... excludes) {

        return results_path.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File file) {

                return file.isDirectory() && !ArrayUtil.contains(file.getName(), excludes);
            }
        });
    }

    interface NumberConverter {

        Number convert(Number value);
    }

    private static class NanosecondToSecond implements NumberConverter {

        @Override
        public Number convert(final Number value) {

            return value.doubleValue() / ONE_SECOND_IN_NANOS;
        }
    }

    private static class FileFilterByName implements IOFileFilter {

        private final String file_name;

        private FileFilterByName(String file_name) {

            this.file_name = file_name;
        }

        @Override
        public boolean accept(final File file) {

            return file.isFile() && matchesFileName(file.getName());
        }

        @Override
        public boolean accept(final File dir, final String name) {

            return matchesFileName(name);
        }

        private boolean matchesFileName(final String name) {

            return name.equals(file_name);
        }
    }

    private static class AsIsConverter implements NumberConverter {

        @Override
        public Number convert(final Number value) {

            return value;
        }
    }

    static class ParseRelativeTime extends ParseLong implements LongCellProcessor {

        private Long first_time;

        ParseRelativeTime() {

        }

        ParseRelativeTime(final LongCellProcessor next) {

            super(next);
        }

        @Override
        public synchronized Object execute(final Object value, final CsvContext context) {

            final Long time = (Long) super.execute(value, context);
            if (first_time == null) {
                first_time = time;
            }
            final Long relative_time = time - first_time;
            return relative_time;
        }
    }

    static class ConvertTime extends CellProcessorAdaptor implements LongCellProcessor {

        private final TimeUnit source_unit;
        private final TimeUnit target_unit;

        ConvertTime(TimeUnit source_unit, TimeUnit target_unit) {

            this.source_unit = source_unit;
            this.target_unit = target_unit;
        }

        @Override
        public Object execute(final Object value, final CsvContext context) {

            return next.execute(target_unit.convert((Long) value, source_unit), context);
        }
    }
}
