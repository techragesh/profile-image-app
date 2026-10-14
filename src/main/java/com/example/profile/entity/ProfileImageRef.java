package com.example.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "profile_image_ref")
public class ProfileImageRef {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProfileImageRef() {
        // required by JPA
    }

    public ProfileImageRef(Long userId, String imageUrl, Integer version, String contentType) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.version = version;
        this.contentType = contentType;
        this.updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
