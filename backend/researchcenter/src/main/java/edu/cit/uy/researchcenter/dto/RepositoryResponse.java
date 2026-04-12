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
public class RepositoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private String role;           // caller's role in this repo
    private int memberCount;
    private int materialCount;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MemberDto> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private Long userId;
        private String name;
        private String email;
        private String role;
        private Instant joinedAt;
    }
}
