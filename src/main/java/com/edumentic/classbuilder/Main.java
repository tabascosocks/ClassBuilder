package com.edumentic.classbuilder;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

@Slf4j
public class Main {

    @Getter @Setter
    private int myNum;

    public static void main(String[] args) {
        try {
            // Load logging configuration
            InputStream inputStream = Main.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (IOException e) {
            System.err.println("Error loading logging configuration: " + e.getMessage());
        }

        //work your magic!
        log.info("Lets do this!");
    }

    /**
     * This function processes the data from a CSV file.
     *
     * @param subjectsDataFile The path to the CSV file containing the subject data.
     *
     * @throws IOException If there is an error reading the CSV file.
     */
    private static void processSubjectsData(String subjectsDataFile) {
        try (FileReader fr = new FileReader(subjectsDataFile)) {

            // Parse the CSV file using the CSVFormat.EXCEL builder
            Iterable<CSVRecord> recordsIterator = CSVFormat.EXCEL.builder()
                    .setSkipHeaderRecord(true) // Skip the header record
                    .build()
                    .parse(fr);

            // Iterate over each record in the CSV file
            for(CSVRecord record : recordsIterator) {
                // Check if the record has enough data
                if(record.size() < 3) continue;

            }
        } catch (IOException e) {
            // Print an error message if there is an error reading the CSV file
            log.error("Error reading CSV file: " + e.getMessage());
        }
    }
}