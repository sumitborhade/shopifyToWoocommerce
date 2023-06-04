package com.pehnavakart.processor;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.pehnavakart.constant.AutomationConstant;
import com.pehnavakart.model.ShopifyAttributes;
import com.pehnavakart.model.WoocommerceAttributes;
import com.pehnavakart.utility.AutomationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WoocommerceService {

    private final Environment environment;
    private final WoocommerceImageService woocommerceImageService;
    private int idMax;
    private int skuMax;
    private final Map<String, String> woocommerceShopifySkuMap = new TreeMap<>();

    @Autowired
    public WoocommerceService(Environment environment,
                              WoocommerceImageService woocommerceImageService) {
        this.environment = environment;
        this.woocommerceImageService = woocommerceImageService;
    }

    public void parseWoocommerceCsv() {
        String inputDirectory = environment.getProperty(AutomationConstant.INPUT_DIRECTORY);
        String woocommerceFilename = environment.getProperty(AutomationConstant.WOOCOMMERCE_FILENAME);

        String filePath = inputDirectory + AutomationConstant.PATH_SEPARATOR + woocommerceFilename;
        List<String[]> readCsvRecordList = AutomationUtility.readCsv(filePath, 1);

        int skuMax = Integer.MIN_VALUE;
        int idMax = Integer.MIN_VALUE;
        int skuNum;

        for (String[] readCsvRecord : readCsvRecordList) {
            String id = readCsvRecord[0];
            String sku = readCsvRecord[2];

            if (AutomationUtility.isNotNumeric(id)) {
                continue;
            }

            if (sku != null && !sku.trim().equals("")) {
                String skuString = sku.replaceAll("[^0-9]", "");

                if (AutomationUtility.isNotNumeric(skuString)) {
                    continue;
                }

                skuNum = Integer.parseInt(skuString);
                skuMax = Math.max(skuMax, skuNum);
                idMax = Math.max(idMax, Integer.parseInt(id));
            }
        }

        if (Integer.MIN_VALUE == idMax) {
            throw new RuntimeException("Id is not found!");
        }

        this.idMax = idMax;
        this.skuMax = skuMax;
    }

    public Set<WoocommerceAttributes> generateWoocommerceCsvForProducts(List<ShopifyAttributes> shopifyAttributesList) {
        String inputDirectory = environment.getProperty(AutomationConstant.INPUT_DIRECTORY);
        String requiredProductsFilename = environment.getProperty(AutomationConstant.REQUIRED_PRODUCTS_FILENAME);

        String filePath = inputDirectory + AutomationConstant.PATH_SEPARATOR + requiredProductsFilename;
        Set<WoocommerceAttributes> allWoocommerceAttributes;

        //read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {

            allWoocommerceAttributes
                    = stream
                    .filter(product -> product != null && !product.startsWith("#"))
                    .filter(product -> !checkIfTheSkuIsPresentInMappingFile(product))
                    .map(product -> generateWoocommerceCsvForProduct(shopifyAttributesList, product))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File : " + filePath + " is not present!!");
        }

        return allWoocommerceAttributes;
    }

    private boolean checkIfTheSkuIsPresentInMappingFile(String product) {
        String outputDirectory = environment.getProperty(AutomationConstant.OUTPUT_DIRECTORY);
        String woocommerceOutputFile = environment.getProperty(AutomationConstant.WOOCOMMERCE_SHOPIFY_SKU_MAPPING_FILENAME);
        String woocommerceShopifySkuMappingFileAbsolutePath = outputDirectory + AutomationConstant.PATH_SEPARATOR + woocommerceOutputFile;

        try (CSVReader reader = new CSVReader(new FileReader(woocommerceShopifySkuMappingFileAbsolutePath))) {
            String[] nextLine;
            int rowNumber = 0;

            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length == 2
                        && product.equalsIgnoreCase(nextLine[1])) {
                    System.out.println("Skipping Supplier SKU " + product + ", as it is already present in mapping file.");
                    return true;
                }
                rowNumber++;
            }

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void generateWoocommerceShopifySkuMappingFile(Map<String, String> woocommerceShopifySkuMap) {
        List<String[]> csvData = woocommerceShopifySkuMap
                .entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> new String[]{entry.getKey(), entry.getValue()})
                .collect(Collectors.toCollection(LinkedList::new));

        String outputDirectory = environment.getProperty(AutomationConstant.OUTPUT_DIRECTORY);
        String woocommerceOutputFile = environment.getProperty(AutomationConstant.WOOCOMMERCE_SHOPIFY_SKU_MAPPING_FILENAME);
        String woocommerceShopifySkuMappingFileAbsolutePath = outputDirectory + AutomationConstant.PATH_SEPARATOR + woocommerceOutputFile;

        AutomationUtility.writeToCsvFile(csvData, woocommerceShopifySkuMappingFileAbsolutePath);
    }

    public Set<WoocommerceAttributes> generateWoocommerceCsvForProduct
            (List<ShopifyAttributes> shopifyAttributesList, String product) {
        Set<WoocommerceAttributes> woocommerceSet = new HashSet<>();
        WoocommerceAttributes p = new WoocommerceAttributes();

        //Images
        Set<String> imagesLocations = new HashSet<>();
        Set<String> sizes = new LinkedHashSet<>();

        for (ShopifyAttributes shopifyAttribute : shopifyAttributesList) {
            if (shopifyAttribute == null
                    || shopifyAttribute.getSku() == null
                    || !shopifyAttribute.getSku().toUpperCase().contains(product.toUpperCase())) {
                continue;
            }

            populateWoocommerceParentMainAttributes(p, imagesLocations, sizes, shopifyAttribute);
        }

        if (p.getSku() == null) {
            System.out.println(product + " is not present in Shopify file! >>>>>>>>>>>>>>>>><<<<<<<<<<<");
            return woocommerceSet;
        }

//        p.setImages(String.join(AutomationConstant.COMMA, imagesLocations));
        p.setAttribute1Values(String.join(AutomationConstant.COMMA, sizes));

        Set<String> woocommerceImageUrls = woocommerceImageService.saveImages(imagesLocations, p.getSku());
        System.out.println(product + " has " + woocommerceImageUrls.size() + " images.");
//        p.setImages(String.join(AutomationConstant.COMMA, woocommerceImageUrls));

        woocommerceSet.add(p);
        populateWoocommerceChildRecords(woocommerceSet, p, sizes);

        if (p.getSku() != null) {
            woocommerceShopifySkuMap.put(p.getSku(), product);
        }
        return woocommerceSet;
    }

    private void populateWoocommerceParentMainAttributes(WoocommerceAttributes p, Set<String> imagesLocations, Set<String> sizes, ShopifyAttributes shopifyAttribute) {
        if (shopifyAttribute.getBodyHTML() != null
                && !shopifyAttribute.getBodyHTML().trim().equals("")
                && p.getDescription() == null) {
            p.setId(String.valueOf(++idMax));
            p.setType(AutomationConstant.VARIABLE);

            p.setSku("PK-" + String.format("%05d", ++skuMax));
            p.setName(shopifyAttribute.getTitle());
            p.setPublished("1");
            p.setIsFeatured("0");
            p.setVisibilityInCatalog(AutomationConstant.VISIBLE);
            p.setTaxStatus(AutomationConstant.TAXABLE);
            p.setInStock("50");
            p.setAllowCustomerReviews("1");
            p.setCategories(shopifyAttribute.getType());
            p.setPosition("0");
            p.setAttribute1Name(AutomationConstant.SIZE);
            p.setAttribute1Visible("0");
            p.setAttribute1Global("0");
            p.setAllowCustomerReviews("1");
            String description = shopifyAttribute.getBodyHTML();

            if (description != null) {
                description = description.replaceAll("(?i)Janasya", "Branded company");
            }

            p.setDescription(description + "<br> SKU: " + p.getSku());
            p.setShortDescription(description + "<br> SKU: " + p.getSku());
            shopifyAttribute.setTags(shopifyAttribute.getTags() + ", pre-order");
            p.setTags(shopifyAttribute.getTags());
        }

        AutomationUtility.addAttributeToSet(shopifyAttribute.getImageLocation(), imagesLocations);
        AutomationUtility.addAttributeToSet(shopifyAttribute.getSize(), sizes);
    }

    private void populateWoocommerceChildRecords(Set<WoocommerceAttributes> woocommerceSet, WoocommerceAttributes p, Set<String> sizes) {
        int imagePosition = 0;

        for (String size : sizes) {
            WoocommerceAttributes a = new WoocommerceAttributes();
            a.setId(String.valueOf(++idMax));
            a.setType(AutomationConstant.VARIATION);
            a.setSku("PK-" + p.getSku() + "-" + size);
            a.setName(p.getName() + "-" + size);
            a.setPublished("1");
            a.setIsFeatured("0");
            a.setVisibilityInCatalog(AutomationConstant.VISIBLE);
            a.setTaxStatus(AutomationConstant.TAXABLE);
            a.setTaxClass(AutomationConstant.PARENT);
            a.setInStock("50");
            a.setAllowCustomerReviews("0");

            a.setParent(p.getSku());
            a.setPosition(String.valueOf(++imagePosition));
            a.setAttribute1Name(AutomationConstant.SIZE);
            a.setAttribute1Values(size);

            a.setAttribute1Global("0");
            woocommerceSet.add(a);
        }
    }

    public void generateWoocommerceFile(List<WoocommerceAttributes> allWoocommerceAttributesList) {

        String[] header = {"ID", "Type", "SKU", "Name", "Published", "Is featured?",
                "Visibility in catalog", "Short description", "Description",
                "Date sale price starts", "Date sale price ends", "Tax status",
                "Tax class", "In stock?", "Stock", "Low stock amount", "Backorders allowed?",
                "Sold individually?", "Weight (kg)", "Length (cm)", "Width (cm)",
                "Height (cm)", "Allow customer reviews?", "Purchase note", "Sale price",
                "Regular price", "Categories", "Tags", "Shipping class", "Images",
                "Download limit", "Download expiry days", "Parent", "Grouped products",
                "Upsells", "Cross-sells", "External URL", "Button text", "Position",
                "Attribute 1 name", "Attribute 1 value(s)", "Attribute 1 visible",
                "Attribute 1 global", "Attribute 1 default"};

        List<String[]> csvData = new ArrayList<>();
        csvData.add(header);
        csvData.addAll(transformShopifyAttributeToCsvData(allWoocommerceAttributesList, header));

        String outputDirectory = environment.getProperty(AutomationConstant.OUTPUT_DIRECTORY);
        String woocommerceOutputFile = environment.getProperty(AutomationConstant.WOOCOMMERCE_OUTPUT_FILE);
        String currentTimeInString = AutomationUtility.datetimeToString(LocalDateTime.now(), AutomationConstant.YYYY_MM_DD_HHMMSS);
        String woocommerceOutputFileAbsolutePath = outputDirectory + AutomationConstant.PATH_SEPARATOR + woocommerceOutputFile + currentTimeInString + ".csv";

        AutomationUtility.writeToCsvFile(csvData, woocommerceOutputFileAbsolutePath);

        generateWoocommerceShopifySkuMappingFile(woocommerceShopifySkuMap);
    }

    private List<String[]> transformShopifyAttributeToCsvData(List<WoocommerceAttributes> allWoocommerceAttributesList, String[] header) {

        List<String[]> csvData = new ArrayList<>();

        for (WoocommerceAttributes woocommerceAttributes : allWoocommerceAttributesList) {
            String[] row = new String[header.length];
            row[0] = woocommerceAttributes.getId();
            row[1] = woocommerceAttributes.getType();
            row[2] = woocommerceAttributes.getSku();
            row[3] = woocommerceAttributes.getName();
            row[4] = woocommerceAttributes.getPublished();
            row[5] = woocommerceAttributes.getIsFeatured();
            row[6] = woocommerceAttributes.getVisibilityInCatalog();
            row[7] = woocommerceAttributes.getShortDescription();
            row[8] = woocommerceAttributes.getDescription();
            row[9] = woocommerceAttributes.getDateSalePriceStarts();
            row[10] = woocommerceAttributes.getDateSalePriceEnds();
            row[11] = woocommerceAttributes.getTaxStatus();
            row[12] = woocommerceAttributes.getTaxClass();
            row[13] = woocommerceAttributes.getInStock();
            row[14] = woocommerceAttributes.getStock();
            row[15] = woocommerceAttributes.getLowStockAmount();
            row[16] = woocommerceAttributes.getBackordersAllowed();
            row[17] = woocommerceAttributes.getSoldIndividually();
            row[18] = "";
            row[19] = "";
            row[20] = "";
            row[21] = "";
            row[22] = woocommerceAttributes.getAllowCustomerReviews();
            row[23] = woocommerceAttributes.getPurchaseNote();
            row[24] = woocommerceAttributes.getSalePrice();
            row[25] = woocommerceAttributes.getRegularPrice();
            row[26] = woocommerceAttributes.getCategories();
            row[27] = woocommerceAttributes.getTags();
            row[28] = woocommerceAttributes.getShippingClass();
            row[29] = woocommerceAttributes.getImages();
            row[30] = woocommerceAttributes.getDownloadLimit();
            row[31] = woocommerceAttributes.getDownloadExpiryDays();
            row[32] = woocommerceAttributes.getParent();
            row[33] = woocommerceAttributes.getGroupedProducts();
            row[34] = woocommerceAttributes.getUpsells();
            row[35] = woocommerceAttributes.getCrossSells();
            row[36] = woocommerceAttributes.getExternalURL();
            row[37] = woocommerceAttributes.getButtonText();
            row[38] = woocommerceAttributes.getPosition();
            row[39] = woocommerceAttributes.getAttribute1Name();
            row[40] = woocommerceAttributes.getAttribute1Values();
            row[41] = woocommerceAttributes.getAttribute1Visible();
            row[42] = woocommerceAttributes.getAttribute1Global();
            row[43] = "";

            for (int columnCount = 0; columnCount < row.length; columnCount++) {
                row[columnCount] = row[columnCount] != null ? row[columnCount] : "";
            }

            csvData.add(row);
        }

        return csvData;
    }

}