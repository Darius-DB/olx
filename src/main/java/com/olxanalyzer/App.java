package com.olxanalyzer;

import com.olxanalyzer.cli.AnalyzeCommand;
import picocli.CommandLine;

/**
 * CLI entry point for the OLX.ro Apartment Price Analyzer.
 *
 * <p>Usage:
 * <pre>
 *   java -jar olx-price-analyzer-1.0.0.jar --cities bucuresti,cluj-napoca --pages 3
 * </pre>
 */
public class App {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(args);
        System.exit(exitCode);
    }
}
