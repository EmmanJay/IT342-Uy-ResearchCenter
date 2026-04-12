package edu.cit.uy.researchcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMaterialStatusRequest {
    @NotBlank
    private String status;
}
