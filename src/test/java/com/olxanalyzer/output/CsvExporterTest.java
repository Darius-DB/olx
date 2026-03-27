package com.olxanalyzer.output;

import com.olxanalyzer.analysis.PriceAnalyzer.CityStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    private CsvExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CsvExporter();
    }

    @Test
    void toCsvRow_normalValues() {
        CityStats stats = new CityStats("bucuresti", 42, 1820.45, 1750.0, 800.0, 4200.0, "EUR");
        String row = exporter.toCsvRow(stats);
        assertEquals("bucuresti,42,1820.45,1750.00,800.00,4200.00,EUR", row);
    }

    @Test
    void toCsvRow_noData_nanValues_emptyNumericFields() {
        CityStats stats = new CityStats("test", 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, "N/A");
        String row = exporter.toCsvRow(stats);
        assertEquals("test,0,,,,,N/A", row);
    }

    @Test
    void toCsvRow_cityWithComma_isQuoted() {
        CityStats stats = new CityStats("Cluj, Napoca", 5, 2000.0, 1900.0, 1500.0, 2500.0, "EUR");
        String row = exporter.toCsvRow(stats);
        assertTrue(row.startsWith("\"Cluj, Napoca\""));
    }

    @Test
    void export_writesHeaderAndRows(@TempDir Path tmpDir) throws IOException {
        Path outFile = tmpDir.resolve("test_output.csv");

        CityStats s1 = new CityStats("bucuresti", 10, 1800.0, 1750.0, 1000.0, 3000.0, "EUR");
        CityStats s2 = new CityStats("cluj-napoca", 8, 2000.0, 1950.0, 1200.0, 3500.0, "EUR");

        exporter.export(List.of(s1, s2), outFile.toString());

        List<String> lines = Files.readAllLines(outFile, StandardCharsets.UTF_8);
        assertEquals(3, lines.size()); // header + 2 rows
        assertEquals("city,validListings,avgPricePerSqm,medianPricePerSqm,minPricePerSqm,maxPricePerSqm,currency",
                lines.get(0));
        assertTrue(lines.get(1).startsWith("bucuresti,10,"));
        assertTrue(lines.get(2).startsWith("cluj-napoca,8,"));
    }

    @Test
    void export_emptyList_writesOnlyHeader(@TempDir Path tmpDir) throws IOException {
        Path outFile = tmpDir.resolve("empty_output.csv");
        exporter.export(List.of(), outFile.toString());

        List<String> lines = Files.readAllLines(outFile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
    }
}
