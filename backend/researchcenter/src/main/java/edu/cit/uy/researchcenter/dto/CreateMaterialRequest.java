package edu.cit.uy.researchcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateMaterialRequest {
    @NotNull
    private Long repositoryId;
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String materialType;   // PDF | LINK | REFERENCE
    private String fileUrl;        // for PDF (pre-uploaded to Supabase)
    private String url;            // for LINK
    private String authors;        // for REFERENCE
    private String publisher;
    private String year;
    private String isbn;
    private String metadata;
    private List<String> tags;
    private String status;         // TO_READ | IN_PROGRESS | COMPLETED
}
