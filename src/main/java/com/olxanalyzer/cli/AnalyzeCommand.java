package com.olxanalyzer.cli;

import com.olxanalyzer.analysis.PriceAnalyzer;
import com.olxanalyzer.analysis.PriceAnalyzer.CityStats;
import com.olxanalyzer.model.ApartmentListing;
import com.olxanalyzer.output.CsvExporter;
import com.olxanalyzer.output.TableFormatter;
import com.olxanalyzer.scraper.OlxScraper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * picocli command that drives the full scrape → analyse → output pipeline.
 */
@Command(
        name = "olx-analyzer",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Fetch OLX.ro apartment-for-sale listings and compare price/m² across cities."
)
public class AnalyzeCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(AnalyzeCommand.class.getName());

    @Option(
            names = "--cities",
            description = "Comma-separated OLX.ro city slugs (default: ${DEFAULT-VALUE})",
            defaultValue = "bucuresti,cluj-napoca,timisoara"
    )
    private String citiesOption;

    @Option(
            names = "--pages",
            description = "Maximum listing pages to fetch per city (default: ${DEFAULT-VALUE})",
            defaultValue = "3"
    )
    private int pages;

    @Option(
            names = "--delay-ms",
            description = "Milliseconds to sleep between HTTP requests (default: ${DEFAULT-VALUE})",
            defaultValue = "1000"
    )
    private long delayMs;

    @Option(
            names = "--out",
            description = "Optional CSV output file path for summary statistics"
    )
    private String outFile;

    @Option(
            names = "--currency",
            description = "Informational: currency to highlight in output. No conversion is performed."
    )
    private String currency;

    // -------------------------------------------------------------------------

    @Override
    public void run() {
        List<String> cities = Arrays.stream(citiesOption.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        System.out.printf("Fetching data for %d city/cities (up to %d page(s) each, delay %d ms)...%n",
                cities.size(), pages, delayMs);

        OlxScraper scraper   = new OlxScraper(null, delayMs);
        PriceAnalyzer analyzer = new PriceAnalyzer();
        List<CityStats> stats = new ArrayList<>();

        for (String city : cities) {
            List<ApartmentListing> listings = scraper.scrape(city, pages);
            CityStats cs = analyzer.analyze(city, listings);
            stats.add(cs);
            System.out.printf("[%s] %d valid listing(s) out of %d total%n",
                    city, cs.getValidListings(), listings.size());
        }

        System.out.println();
        System.out.println(new TableFormatter().format(stats));

        if (outFile != null && !outFile.isBlank()) {
            try {
                new CsvExporter().export(stats, outFile);
                System.out.println("Summary written to: " + outFile);
            } catch (IOException e) {
                System.err.println("Failed to write CSV: " + e.getMessage());
            }
        }
    }
}
