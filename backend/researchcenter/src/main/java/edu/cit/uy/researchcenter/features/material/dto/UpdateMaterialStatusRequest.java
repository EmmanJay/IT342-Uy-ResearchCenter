package edu.cit.uy.researchcenter.features.material.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMaterialStatusRequest {
    @NotBlank
    private String status;
}
