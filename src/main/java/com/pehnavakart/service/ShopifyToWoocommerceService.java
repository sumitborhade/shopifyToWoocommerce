package com.pehnavakart.service;

import com.pehnavakart.model.ShopifyAttributes;
import com.pehnavakart.model.WoocommerceAttributes;
import com.pehnavakart.processor.ShopifyService;
import com.pehnavakart.processor.WoocommerceService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public record ShopifyToWoocommerceService(
        ShopifyService shopifyService,
        WoocommerceService woocommerceService) {

    public void generateWoocommerceCsv() {
        //1. Parse CSV
        List<ShopifyAttributes> shopifyAttributesList = shopifyService.parseShopfiyCsv();

        //2. Find the max id and SKU
        woocommerceService.parseWoocommerceCsv();
//        idMax = 3532;
//        skuMax = 128;

        //3. Find Products in shopify
        Set<WoocommerceAttributes> allWoocommerceAttributes
                = woocommerceService.generateWoocommerceCsvForProducts(shopifyAttributesList);

        List<WoocommerceAttributes> allWoocommerceAttributesList = allWoocommerceAttributes.stream()
                .sorted(Comparator.comparing(WoocommerceAttributes::getId))
                .collect(Collectors.toList());

        //4. Generate Woocommerce file
        woocommerceService.generateWoocommerceFile(allWoocommerceAttributesList);

    }

}