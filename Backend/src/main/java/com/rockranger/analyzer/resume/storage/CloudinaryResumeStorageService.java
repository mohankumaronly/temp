package com.rockranger.analyzer.resume.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class CloudinaryResumeStorageService implements ResumeStorageService {

    private final Cloudinary cloudinary;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CloudinaryResumeStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String upload(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty.");
        }

        try {

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "folder", "resume-analyzer/resumes",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload resume to Cloudinary.",
                    e
            );
        }
    }

    @Override
    public byte[] download(String cloudinaryUrl) {

        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Cloudinary URL cannot be empty."
            );
        }

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cloudinaryUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Failed to download resume from Cloudinary. HTTP status: "
                                + response.statusCode()
                );
            }

            return response.body();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Resume download was interrupted.",
                    e
            );

        } catch (IOException | IllegalArgumentException e) {

            throw new RuntimeException(
                    "Failed to download resume from Cloudinary.",
                    e
            );
        }
    }
}