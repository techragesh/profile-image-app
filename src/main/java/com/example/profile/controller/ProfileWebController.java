package com.example.profile.controller;

import com.example.profile.service.ProfileImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileWebController {

    private final ProfileImageService profileImageService;

    public ProfileWebController(ProfileImageService profileImageService) {
        this.profileImageService = profileImageService;
    }

    // GET /profile/{userId} -> renders the card + upload form + list of all profiles
    @GetMapping("/{userId}")
    public String showProfile(@PathVariable Long userId, Model model) {
        boolean hasImage = profileImageService.hasProfileImage(userId);
        model.addAttribute("userId", userId);
        model.addAttribute("hasImage", hasImage);
        model.addAttribute("profiles", profileImageService.getAllProfileSummaries());
        return "profile";
    }

    // POST /profile/{userId}/upload -> handles the multipart upload
    @PostMapping("/{userId}/upload")
    public String upload(@PathVariable Long userId,
                          @RequestParam("file") MultipartFile file,
                          RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please choose a file first");
            return "redirect:/profile/" + userId;
        }
        profileImageService.uploadProfileImage(userId, file);
        redirectAttributes.addFlashAttribute("message", "Profile picture updated");
        return "redirect:/profile/" + userId;
    }

    // GET /profile/{userId}/image -> streams raw bytes, used as the <img> src
    @GetMapping("/{userId}/image")
    @ResponseBody
    public ResponseEntity<byte[]> viewImage(@PathVariable Long userId) {
        byte[] data = profileImageService.getProfileImageBytes(userId).orElse(null);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = profileImageService.getContentType(userId).orElse("image/jpeg");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // No CDN, so no long-lived caching - revalidate each time
                .cacheControl(CacheControl.noCache())
                .body(data);
    }
}
