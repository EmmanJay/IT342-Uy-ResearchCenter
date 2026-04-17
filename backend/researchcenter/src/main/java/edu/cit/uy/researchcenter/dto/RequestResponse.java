package edu.cit.uy.researchcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private Long id;
    private Long repositoryId;
    private String repositoryName;
    private String title;
    private String description;
    private Long requesterId;
    private String requesterName;
    private String status;
    private Long fulfilledBy;
    private String fulfilledByName;
    private Instant fulfilledAt;
    private Long materialId;
    private String materialTitle;
    private Instant createdAt;
    private Instant updatedAt;
}
