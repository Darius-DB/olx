package com.olxanalyzer.analysis;

import com.olxanalyzer.model.ApartmentListing;

import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Computes statistical summaries (average, median, min, max) of price-per-m²
 * values for a set of {@link ApartmentListing} objects.
 */
public class PriceAnalyzer {

    /**
     * Compute a {@link CityStats} summary for the provided listings.
     *
     * @param city     city name / slug
     * @param listings all listings scraped for that city (may include invalid ones)
     * @return a summary object
     */
    public CityStats analyze(String city, List<ApartmentListing> listings) {
        List<Double> values = listings.stream()
                .filter(ApartmentListing::isValid)
                .map(ApartmentListing::getPricePerSqm)
                .sorted()
                .collect(Collectors.toList());

        String currency = listings.stream()
                .filter(ApartmentListing::isValid)
                .map(ApartmentListing::getCurrency)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("N/A");

        if (values.isEmpty()) {
            return new CityStats(city, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, currency);
        }

        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        double median = computeMedian(values);
        double min = values.get(0);
        double max = values.get(values.size() - 1);

        return new CityStats(city, values.size(), avg, median, min, max, currency);
    }

    /**
     * Compute the median of a <em>sorted</em> list of doubles.
     * Returns {@code Double.NaN} for an empty list.
     */
    public double computeMedian(List<Double> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return Double.NaN;
        }
        int n = sortedValues.size();
        if (n % 2 == 1) {
            return sortedValues.get(n / 2);
        } else {
            return (sortedValues.get(n / 2 - 1) + sortedValues.get(n / 2)) / 2.0;
        }
    }

    // -------------------------------------------------------------------------
    // Inner result class
    // -------------------------------------------------------------------------

    /** Holds computed statistics for one city. */
    public static final class CityStats {
        private final String city;
        private final int validListings;
        private final double avg;
        private final double median;
        private final double min;
        private final double max;
        private final String currency;

        public CityStats(String city, int validListings,
                         double avg, double median, double min, double max,
                         String currency) {
            this.city = city;
            this.validListings = validListings;
            this.avg = avg;
            this.median = median;
            this.min = min;
            this.max = max;
            this.currency = currency;
        }

        public String getCity()          { return city; }
        public int getValidListings()    { return validListings; }
        public double getAvg()           { return avg; }
        public double getMedian()        { return median; }
        public double getMin()           { return min; }
        public double getMax()           { return max; }
        public String getCurrency()      { return currency; }
    }
}
