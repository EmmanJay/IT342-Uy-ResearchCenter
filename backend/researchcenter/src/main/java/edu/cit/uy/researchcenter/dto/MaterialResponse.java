package edu.cit.uy.researchcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {
    private Long id;
    private Long repositoryId;
    private String repositoryName;
    private String title;
    private String description;
    private String materialType;
    private String fileUrl;
    private String url;
    private String authors;
    private String publisher;
    private String year;
    private String isbn;
    private String metadata;
    private Long uploaderId;
    private String uploaderName;
    private List<String> tags;
    private String status;
    private String myStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
