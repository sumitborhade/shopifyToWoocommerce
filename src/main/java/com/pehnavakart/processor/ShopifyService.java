package com.pehnavakart.processor;

import com.pehnavakart.constant.AutomationConstant;
import com.pehnavakart.model.ShopifyAttributes;
import com.pehnavakart.utility.AutomationUtility;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public record ShopifyService(Environment environment) {

    public List<ShopifyAttributes> parseShopfiyCsv() {
        String inputDirectory = environment.getProperty(AutomationConstant.INPUT_DIRECTORY);
        String shopifyFilename= environment.getProperty(AutomationConstant.SHOPIFY_FILENAME);

        String shopifyFilePath = inputDirectory + AutomationConstant.PATH_SEPARATOR + shopifyFilename;
        List<String[]> readCsvRecordList = AutomationUtility.readCsv(shopifyFilePath, 1);

        List<ShopifyAttributes> shopifyAttributesList = new ArrayList<>();

        for (String[] readCsvRecord : readCsvRecordList) {
            ShopifyAttributes psa = new ShopifyAttributes();
            psa.setTitle(readCsvRecord[1]);
            psa.setBodyHTML(readCsvRecord[2]);
            psa.setType(readCsvRecord[4]);
            psa.setTags(readCsvRecord[5]);
            psa.setImageLocation(readCsvRecord[24]);
            psa.setSize(readCsvRecord[10]);
            psa.setSku(readCsvRecord[13]);
            shopifyAttributesList.add(psa);
//            System.out.println(psa.getSku());
        }

        System.out.println("Shopify products size : " + shopifyAttributesList.size());
        return shopifyAttributesList;
    }
}
