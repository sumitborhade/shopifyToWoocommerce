package com.pehnavakart;

import com.pehnavakart.service.ShopifyToWoocommerceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutomationApplication implements CommandLineRunner {

	private final ShopifyToWoocommerceService shopifyToWoocommerceService;

	@Autowired
	public AutomationApplication(ShopifyToWoocommerceService shopifyToWoocommerceService) {
		this.shopifyToWoocommerceService = shopifyToWoocommerceService;
	}

	public static void main(String[] args) {
		SpringApplication.run(AutomationApplication.class, args);
	}

	@Override
	public void run(String... args)  {
		shopifyToWoocommerceService.generateWoocommerceCsv();
	}
}
