package edu.cit.uy.researchcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRepositoryRequest {
    @NotBlank(message = "Repository name is required")
    private String name;
    private String description;
}
