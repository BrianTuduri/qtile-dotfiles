package com.example.vaultdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class VaultDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultDemoApplication.class, args);
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
    public ResponseEntity<String> getVaultData() {
        try {
            logger.info("Fetching data from Vault");
            logger.info("Datasource URL: {}", datasourceUrl);
            logger.info("Datasource Username: {}", datasourceUsername);
            return ResponseEntity.ok(String.format("Datasource URL: %s, Username: %s", datasourceUrl, datasourceUsername));
        } catch (Exception e) {
            logger.error("Error fetching data from Vault", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching data from Vault: " + e.getMessage());
        }
    }

    @GetMapping("/vault-status")
    public ResponseEntity<String> getVaultStatus() {
        try {
            logger.info("Checking Vault configuration");
            logger.info("Datasource URL: {}", datasourceUrl);
            logger.info("Datasource Username: {}", datasourceUsername);
            if ("URL not found".equals(datasourceUrl) || "Username not found".equals(datasourceUsername)) {
                throw new IllegalStateException("Vault configuration is incorrect. Check the secrets and configuration.");
            }
            return ResponseEntity.ok("Vault configuration is correct.");
        } catch (Exception e) {
            logger.error("Error checking Vault configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Vault configuration error: " + e.getMessage());
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        logger.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
    }
}
