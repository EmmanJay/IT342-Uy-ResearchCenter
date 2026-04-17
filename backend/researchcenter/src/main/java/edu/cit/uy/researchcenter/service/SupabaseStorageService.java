package edu.cit.uy.researchcenter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final WebClient webClient;

    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String filename = userId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + filename;

        try {
            webClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", "application/octet-stream")
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to Supabase");
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + filename;
    }

    public void deleteFile(String fileUrl) {
        try {
            String path = fileUrl.replace(supabaseUrl + "/storage/v1/object/public/" + bucket + "/", "");
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

            webClient.delete()
                    .uri(deleteUrl)
                    .header("Authorization", "Bearer " + serviceKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            // Log but don't fail if deletion fails
            System.err.println("Failed to delete file from Supabase: " + e.getMessage());
        }
    }
}
