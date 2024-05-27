package com.example.vaultdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Value("${spring.datasource.url:URL not found}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:Username not found}")
    private String datasourceUsername;

    @GetMapping("/vault-data")
    public String getVaultData() {
        logger.info("Fetching data from Vault");
        logger.info("Datasource URL: {}", datasourceUrl);
        logger.info("Datasource Username: {}", datasourceUsername);
        if ("URL not found".equals(datasourceUrl) || "Username not found".equals(datasourceUsername)) {
            logger.error("Failed to retrieve Vault data. Ensure the Vault configuration is correct.");
        }
        return String.format("Datasource URL: %s, Username: %s", datasourceUrl, datasourceUsername);
    }
}

