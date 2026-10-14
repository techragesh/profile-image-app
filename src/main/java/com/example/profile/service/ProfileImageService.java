package com.example.profile.service;

import com.example.profile.dto.ProfileSummary;
import com.example.profile.entity.ProfileImageRef;
import com.example.profile.repository.ProfileImageRefRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfileImageService {

    private final ProfileImageRefRepository repository;
    private final AzureBlobStorageService blobStorageService;

    public ProfileImageService(
            ProfileImageRefRepository repository,
            AzureBlobStorageService blobStorageService) {
        this.repository = repository;
        this.blobStorageService = blobStorageService;
    }

    public void uploadProfileImage(Long userId, MultipartFile file) {
        Optional<ProfileImageRef> existing = repository.findById(userId);
        int nextVersion = existing.map(ref -> ref.getVersion() + 1).orElse(1);

        String extension = extensionFor(file.getContentType());
        String blobPath = "profile-pics/" + userId + "/v" + nextVersion + "-full." + extension;

        String imageUrl = blobStorageService.upload(blobPath, file);

        ProfileImageRef ref = existing.orElse(
                new ProfileImageRef(userId, imageUrl, nextVersion, file.getContentType())
        );
        ref.setImageUrl(imageUrl);
        ref.setVersion(nextVersion);
        ref.setContentType(file.getContentType());
        ref.setUpdatedAt(Instant.now());
        repository.save(ref);
    }

    public boolean hasProfileImage(Long userId) {
        return repository.existsById(userId);
    }

    public Optional<byte[]> getProfileImageBytes(Long userId) {
        return repository.findById(userId)
                .map(ref -> blobStorageService.download(ref.getImageUrl()));
    }

    public Optional<String> getImageUrl(Long userId) {
        return repository.findById(userId).map(ProfileImageRef::getImageUrl);
    }

    public Optional<String> getContentType(Long userId) {
        return repository.findById(userId).map(ProfileImageRef::getContentType);
    }

    // All profiles that have a picture uploaded - for the list view.
    // Account number here is a static, deterministic placeholder (not a
    // real stored value) - same userId always produces the same number.
    public List<ProfileSummary> getAllProfileSummaries() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ProfileImageRef::getUserId))
                .map(ref -> new ProfileSummary(ref.getUserId(), generateAccountNumber(ref.getUserId())))
                .collect(Collectors.toList());
    }

    private String generateAccountNumber(Long userId) {
        return String.format("ACC-%08d", userId);
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
