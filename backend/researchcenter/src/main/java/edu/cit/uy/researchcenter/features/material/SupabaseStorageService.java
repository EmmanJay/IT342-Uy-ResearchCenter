package edu.cit.uy.researchcenter.features.material;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-key:}")
    private String supabaseServiceKey;

    @Value("${supabase.bucket:research-materials}")
    private String bucketName;

    /**
     * Delete a file from Supabase Storage
     * @param fileUrl The public URL or file path to delete
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            logger.warn("Attempted to delete null or empty file URL");
            return;
        }

        try {
            // Extract filename from URL
            // URL format: https://[project].supabase.co/storage/v1/object/public/[bucket]/[path]
            String filename = extractFilenameFromUrl(fileUrl);
            
            if (filename == null || filename.isBlank()) {
                logger.warn("Could not extract filename from URL: " + fileUrl);
                return;
            }

            logger.info("File deletion from Supabase initiated for: " + filename);
            // TODO: Implement actual Supabase API call to delete file
            // This is a placeholder implementation
        } catch (Exception e) {
            logger.error("Error deleting file from Supabase: " + fileUrl, e);
            throw new RuntimeException("Failed to delete file from Supabase", e);
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (!fileUrl.contains("/")) {
            return fileUrl;
        }
        // Extract the path portion after the bucket name
        int lastSlashIndex = fileUrl.lastIndexOf("/");
        if (lastSlashIndex >= 0 && lastSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }
}
