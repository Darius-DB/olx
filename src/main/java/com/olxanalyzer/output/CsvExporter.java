package com.olxanalyzer.output;

import com.olxanalyzer.analysis.PriceAnalyzer.CityStats;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports per-city summary statistics to a CSV file.
 */
public class CsvExporter {

    private static final String HEADER =
            "city,validListings,avgPricePerSqm,medianPricePerSqm,minPricePerSqm,maxPricePerSqm,currency";

    /**
     * Write the summary CSV for the given stats to the provided file path.
     *
     * @param stats    list of {@link CityStats} to export
     * @param filePath destination file path
     * @throws IOException if the file cannot be written
     */
    public void export(List<CityStats> stats, String filePath) throws IOException {
        Path path = Path.of(filePath);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            pw.println(HEADER);
            for (CityStats s : stats) {
                pw.println(toCsvRow(s));
            }
        }
    }

    /**
     * Convert a single {@link CityStats} to a CSV row string (no newline).
     */
    String toCsvRow(CityStats s) {
        return String.join(",",
                escapeCsv(s.getCity()),
                String.valueOf(s.getValidListings()),
                formatDouble(s.getAvg()),
                formatDouble(s.getMedian()),
                formatDouble(s.getMin()),
                formatDouble(s.getMax()),
                escapeCsv(s.getCurrency() != null ? s.getCurrency() : "")
        );
    }

    private String formatDouble(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "";
        return String.format("%.2f", v);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
