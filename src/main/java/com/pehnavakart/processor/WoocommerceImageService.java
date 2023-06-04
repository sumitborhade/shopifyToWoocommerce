package com.pehnavakart.processor;

import com.pehnavakart.constant.AutomationConstant;
import com.pehnavakart.utility.AutomationUtility;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public record WoocommerceImageService(Environment environment) {

    public Set<String> saveImages(Set<String> imageUrls, String woocommerceSku) {
        Set<String> woocommerceImageUrls = new TreeSet<>();
        int imageCounter = 1;

        for (String imageUrl : imageUrls) {
            String woocommerceImageAbsolutePath = saveImage(imageUrl, woocommerceSku, imageCounter++);
            woocommerceImageUrls.add(woocommerceImageAbsolutePath);
        }

        return woocommerceImageUrls;
    }

    public String saveImage(String imageUrl, String sku, int i) {
        String destinationImageFilename = null;

        String woocommerceUploadBaseUrl = environment.getProperty(AutomationConstant.WOOCOMMERCE_UPLOAD_BASE_URL);
        String outputDirectory = environment.getProperty(AutomationConstant.OUTPUT_DIRECTORY);
        String dateDirectory = AutomationUtility.dateToString(LocalDate.now(), AutomationConstant.YYYY_MM);

        try {
            //Create folder if not present
            String parentDirectory = outputDirectory + AutomationConstant.PATH_SEPARATOR + dateDirectory;
            createDirectoryIfNotPresent(parentDirectory, sku);

            Optional<String> extensionOptional = AutomationUtility.getExtensionByStringHandling(imageUrl);
            String extension = extensionOptional.orElse("jpg");

            destinationImageFilename = sku + "-" + i + "." + extension;
            String destinationImageFileAbsolutePath = outputDirectory
                    + AutomationConstant.PATH_SEPARATOR + dateDirectory
                    + AutomationConstant.PATH_SEPARATOR + sku
                    + AutomationConstant.PATH_SEPARATOR + destinationImageFilename;
            AutomationUtility.saveImage(imageUrl, destinationImageFileAbsolutePath);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Image " + imageUrl + " could not be downloaded");
        }

        return woocommerceUploadBaseUrl
                + AutomationConstant.PATH_SEPARATOR + dateDirectory
                + AutomationConstant.PATH_SEPARATOR + sku
                + AutomationConstant.PATH_SEPARATOR + destinationImageFilename;
    }

    private void createDirectoryIfNotPresent(String parentDirectory, String directoryName) throws IOException {
        if (parentDirectory == null) {
            throw new RuntimeException("Output directory is empty");
        }
        Files.createDirectories(Paths.get(parentDirectory + "/" + directoryName));
    }
}
