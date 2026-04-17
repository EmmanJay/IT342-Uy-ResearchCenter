package edu.cit.uy.researchcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRequestDto {
    @NotNull
    private Long repositoryId;
    @NotBlank
    private String title;
    private String description;
}
