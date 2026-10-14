package com.example.profile.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class AzureBlobStorageService {

    private final BlobContainerClient containerClient;

    // Client now comes from AzureConfiguration's bean - single source of
    // truth for the connection, instead of building it again here.
    public AzureBlobStorageService(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    public String upload(String blobPath, MultipartFile file) {
        BlobClient blobClient = containerClient.getBlobClient(blobPath);
        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
            blobClient.setHttpHeaders(
                new BlobHttpHeaders().setContentType(file.getContentType())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload blob: " + blobPath, e);
        }
        // Build the URL ourselves rather than using blobClient.getBlobUrl() -
        // that method over-encodes "/" in the blob name as "%2F", which then
        // fails to resolve back to the real blob when we parse it later.
        return containerClient.getBlobContainerUrl() + "/" + blobPath;
    }

    /**
     * No CDN in front of this - Spring Boot streams the bytes itself.
     * Fine for moderate traffic; if read volume grows, put a CDN back
     * in front of the blob container rather than scaling this endpoint.
     *
     * Takes the full blob URL (as stored in the DB) and resolves it back
     * to a BlobClient via Managed Identity - the URL itself carries no
     * credentials, so storing it in the DB is safe as long as the
     * container stays private and reads go through this app.
     */

    public byte[] download(String blobUrl) {
        String blobName = blobNameFromUrl(blobUrl);
        log.info("Looking for blob name: '{}' (from stored URL: {})", blobName, blobUrl);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        boolean exists = blobClient.exists();
        log.info("Blob exists check result: {}", exists);
        if (!exists) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return outputStream.toByteArray();
    }

    private String blobNameFromUrl(String blobUrl) {
        String containerName = containerClient.getBlobContainerName();
        String marker = "/" + containerName + "/";
        int idx = blobUrl.indexOf(marker);
        if (idx < 0) {
            throw new IllegalArgumentException("URL does not belong to container " + containerName + ": " + blobUrl);
        }
        String rawName = blobUrl.substring(idx + marker.length());
        // Defensive decode - handles both correctly-formed URLs and any
        // that end up with "/" over-encoded as "%2F" (e.g. from older
        // stored rows, or any SDK/browser link that encoded it that way).
        return java.net.URLDecoder.decode(rawName, java.nio.charset.StandardCharsets.UTF_8);
    }
}