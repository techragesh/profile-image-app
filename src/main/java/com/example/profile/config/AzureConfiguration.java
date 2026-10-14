package com.example.profile.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AzureConfiguration.class);

    @Value("${azure.storage.account-url}")
    private String accountUrl;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Bean
    public DefaultAzureCredential azureCredential() {
        // tryLogging = true makes each credential in the chain log why it
        // succeeded/failed, at INFO level, same style as what you've already
        // been seeing in the console.
        return new DefaultAzureCredentialBuilder().build();
    }

    @Bean
    public BlobServiceClient blobServiceClient(DefaultAzureCredential credential) {
        log.info("=== Azure Blob connection ===");
        log.info("Configured account-url : {}", accountUrl);
        log.info("Configured container   : {}", containerName);

        BlobServiceClient client = new BlobServiceClientBuilder()
                .endpoint(accountUrl)
                .credential(credential)
                .buildClient();

        log.info("Resolved account URL from client: {}", client.getAccountUrl());
        return client;
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // This call itself requires a successful token + authorized request,
        // so if your RBAC/identity setup is wrong, you'll see it fail HERE,
        // at startup, instead of only when you try to upload from the browser.
        boolean exists = containerClient.exists();
        log.info("Container '{}' reachable at startup: {}", containerName, exists);

        return containerClient;
    }

    @PostConstruct
    public void logEnvironmentHints() {
        // These three override AzureCliCredential's tenant/account resolution
        // if set - if any of these are present and don't match what you used
        // for `az login` + the CLI test that worked, that's almost certainly
        // your mismatch.
        log.info("AZURE_TENANT_ID env var       : {}", System.getenv("AZURE_TENANT_ID"));
        log.info("AZURE_CLIENT_ID env var       : {}", System.getenv("AZURE_CLIENT_ID"));
        log.info("AZURE_SUBSCRIPTION_ID env var : {}", System.getenv("AZURE_SUBSCRIPTION_ID"));
    }
}
