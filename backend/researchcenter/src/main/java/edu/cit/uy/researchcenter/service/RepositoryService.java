package edu.cit.uy.researchcenter.service;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.*;
import edu.cit.uy.researchcenter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final ResearchRepositoryRepo repositoryRepo;
    private final RepositoryMemberRepo memberRepo;
    private final MaterialRepo materialRepo;
    private final UserRepository userRepository;  // from Phase 1

    // ── Create ────────────────────────────────────────────────────────────
    @Transactional
    public RepositoryResponse create(User owner, CreateRepositoryRequest req) {
        ResearchRepository repo = ResearchRepository.builder()
                .owner(owner)
                .name(req.getName())
                .description(req.getDescription())
                .build();
        repo = repositoryRepo.save(repo);

        // Add owner as OWNER member
        RepositoryMember ownerMember = RepositoryMember.builder()
                .repository(repo)
                .user(owner)
                .roleInRepo("OWNER")
                .build();
        memberRepo.save(ownerMember);

        return toResponse(repo, owner.getId());
    }

    // ── Get all repos for user (owned + member) ───────────────────────────
    public List<RepositoryResponse> getAllForUser(Long userId) {
        List<RepositoryMember> memberships = memberRepo.findAllByUserId(userId);
        return memberships.stream()
                .map(m -> toResponse(m.getRepository(), userId))
                .collect(Collectors.toList());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────
    public RepositoryResponse getById(Long repoId, Long callerId) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertMember(repoId, callerId);
        return toDetailResponse(repo, callerId);
    }

    // ── Update ────────────────────────────────────────────────────────────
    @Transactional
    public RepositoryResponse update(Long repoId, Long callerId, CreateRepositoryRequest req) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertOwner(repo, callerId);
        repo.setName(req.getName());
        if (req.getDescription() != null) repo.setDescription(req.getDescription());
        return toResponse(repositoryRepo.save(repo), callerId);
    }

    // ── Delete ────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long repoId, Long callerId) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertOwner(repo, callerId);
        repositoryRepo.delete(repo);
    }

    // ── Invite member ─────────────────────────────────────────────────────
    @Transactional
    public void inviteMember(Long repoId, Long callerId, String email) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertOwner(repo, callerId);

        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with that email not found"));

        if (memberRepo.existsByRepositoryIdAndUserId(repoId, invitee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }

        RepositoryMember member = RepositoryMember.builder()
                .repository(repo)
                .user(invitee)
                .roleInRepo("MEMBER")
                .build();
        memberRepo.save(member);
    }

    // ── Get members ───────────────────────────────────────────────────────
    public List<RepositoryResponse.MemberDto> getMembers(Long repoId, Long callerId) {
        assertMember(repoId, callerId);
        return memberRepo.findByRepositoryId(repoId).stream()
                .map(m -> RepositoryResponse.MemberDto.builder()
                        .userId(m.getUser().getId())
                        .name(buildOwnerName(m.getUser()))
                        .email(m.getUser().getEmail())
                        .role(m.getRoleInRepo())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Remove member ─────────────────────────────────────────────────────
    @Transactional
    public void removeMember(Long repoId, Long callerId, Long userId) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertOwner(repo, callerId);
        if (userId.equals(repo.getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the owner");
        }
        memberRepo.deleteByRepositoryIdAndUserId(repoId, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void assertOwner(ResearchRepository repo, Long callerId) {
        if (!repo.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can perform this action");
        }
    }

    private void assertMember(Long repoId, Long callerId) {
        if (!memberRepo.existsByRepositoryIdAndUserId(repoId, callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }
    }

    private RepositoryResponse toResponse(ResearchRepository repo, Long callerId) {
        String role = repo.getOwner().getId().equals(callerId) ? "OWNER" : "MEMBER";
        int matCount = materialRepo.findByRepositoryId(repo.getId()).size();
        int memCount = memberRepo.findByRepositoryId(repo.getId()).size();
        String ownerName = buildOwnerName(repo.getOwner());
        return RepositoryResponse.builder()
                .id(repo.getId())
                .name(repo.getName())
                .description(repo.getDescription())
                .ownerId(repo.getOwner().getId())
                .ownerName(ownerName)
                .role(role)
                .materialCount(matCount)
                .memberCount(memCount)
                .createdAt(repo.getCreatedAt())
                .updatedAt(repo.getUpdatedAt())
                .build();
    }

    private String buildOwnerName(User owner) {
        String firstName = owner.getFirstName() != null ? owner.getFirstName() : "";
        String lastName = owner.getLastName() != null ? owner.getLastName() : "";
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? owner.getEmail() : name;
    }

    private RepositoryResponse toDetailResponse(ResearchRepository repo, Long callerId) {
        RepositoryResponse base = toResponse(repo, callerId);
        List<RepositoryResponse.MemberDto> members = memberRepo.findByRepositoryId(repo.getId()).stream()
                .map(m -> RepositoryResponse.MemberDto.builder()
                        .userId(m.getUser().getId())
                        .name(buildOwnerName(m.getUser()))
                        .email(m.getUser().getEmail())
                        .role(m.getRoleInRepo())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
        base.setMembers(members);
        return base;
    }
}
