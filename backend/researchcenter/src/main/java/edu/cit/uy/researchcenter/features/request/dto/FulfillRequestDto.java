package edu.cit.uy.researchcenter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FulfillRequestDto {
    @NotNull
    private Long materialId;
}
