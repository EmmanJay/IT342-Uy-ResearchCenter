package edu.cit.uy.researchcenter.features.request.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FulfillRequestDto {
    @NotNull
    private Long materialId;
}
