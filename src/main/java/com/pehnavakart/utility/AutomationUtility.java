package com.pehnavakart.utility;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.pehnavakart.constant.AutomationConstant;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AutomationUtility {

    private AutomationUtility() {
    }

    public static List<String[]> readCsv(String filePath, int skipLines) {
        try (
                Reader reader = Files.newBufferedReader(Paths.get(filePath));
                CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
             ) {
            return csvReader.readAll();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while reading "+ filePath);
        }
    }

    public static boolean isNotNumeric(String strNum) {
        if (strNum == null) {
            return true;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return true;
        }
        return false;
    }

    public static void addAttributeToSet(String attribute, Set<String> set) {
        if (attribute != null && !attribute.trim().equals("")) {
            set.add(attribute);
        }
    }

    public static void saveImage(String imageUrl, String destinationImageWithAbsolutePath) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream();
                 OutputStream os = new FileOutputStream(destinationImageWithAbsolutePath)) {

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Image " + imageUrl + " could not be downloaded");
        }
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .map(input -> input.substring(0, input.indexOf("?")));
    }


    public static String dateToString(LocalDate localDate, String dateFormatterString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatterString);
        return localDate.format(formatter);
    }

    public static String datetimeToString(LocalDateTime localDateTime, String datetimeFormatterString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datetimeFormatterString);
        return localDateTime.format(formatter);
    }

    public static void writeToCsvFile(List<String[]> csvData, String woocommerceOutputFileAbsolutePath) {
        try (
                FileWriter writer = new FileWriter(woocommerceOutputFileAbsolutePath, true);
                CSVWriter csvWriter = new CSVWriter(writer,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.DEFAULT_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
        ) {
            csvWriter.writeAll(csvData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
