package edu.cit.uy.researchcenter.features.repository;

import edu.cit.uy.researchcenter.features.repository.dto.*;
import edu.cit.uy.researchcenter.features.repository.model.*;
import edu.cit.uy.researchcenter.features.repository.repository.*;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.auth.repository.UserRepository;
import edu.cit.uy.researchcenter.features.material.repository.MaterialRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepositoryService {

    private final ResearchRepositoryRepo repositoryRepo;
    private final RepositoryMemberRepo memberRepo;
    private final MaterialRepo materialRepo;
    private final UserRepository userRepository;  // from Phase 1
    private final edu.cit.uy.researchcenter.shared.service.EmailService emailService;
    private final edu.cit.uy.researchcenter.features.activity.service.ActivityService activityService;

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
                .status("ACCEPTED")
                .build();
        memberRepo.save(ownerMember);

        activityService.logActivity(owner, "created a repository", "REPOSITORY", repo.getId(), repo.getName(), repo, null, null);

        return toResponse(repo, owner.getId());
    }

    // ── Get all repos for user (owned + member) ───────────────────────────
    public List<RepositoryResponse> getAllForUser(Long userId) {
        List<RepositoryMember> memberships = memberRepo.findAllByUserId(userId);
        return memberships.stream()
                .filter(m -> "ACCEPTED".equalsIgnoreCase(m.getStatus()) || "OWNER".equalsIgnoreCase(m.getRoleInRepo()))
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
        
        // Delete all repository members first to avoid foreign key constraint violations
        memberRepo.deleteByRepositoryId(repoId);
        
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
                .status("PENDING")
                .inviteToken(UUID.randomUUID().toString())
                .build();
        memberRepo.save(member);

        User inviter = userRepository.findById(callerId).orElse(null);
        String inviterName = inviter != null ? (inviter.getFirstName() + " " + inviter.getLastName()) : "Someone";
        emailService.sendRepositoryInviteEmail(email, repo.getName(), inviterName, member.getInviteToken());

        activityService.logActivity(inviter, "invited a member", "MEMBER", invitee.getId(), invitee.getFirstName() + " " + invitee.getLastName(), repo, invitee.getId(), null);
    }

    public Map<String, Object> getInvitation(String inviteToken, Long callerId) {
        RepositoryMember member = memberRepo.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!member.getUser().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please log in using the invited account");
        }
        return Map.of(
                "repositoryId", member.getRepository().getId(),
                "repositoryName", member.getRepository().getName(),
                "status", member.getStatus(),
                "email", member.getUser().getEmail()
        );
    }

    @Transactional
    public Map<String, Object> acceptInvitation(String inviteToken, Long callerId) {
        RepositoryMember member = memberRepo.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!member.getUser().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please log in using the invited account");
        }
        member.setStatus("ACCEPTED");
        member.setInviteToken(null);
        member.setJoinedAt(Instant.now());
        memberRepo.save(member);

        return Map.of(
                "message", "Invitation accepted",
                "repositoryId", member.getRepository().getId(),
                "repositoryName", member.getRepository().getName()
        );
    }

    @Transactional
    public void rejectInvitation(String inviteToken, Long callerId) {
        RepositoryMember member = memberRepo.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (!member.getUser().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please log in using the invited account");
        }
        memberRepo.delete(member);
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
                        .status(m.getStatus())
                        .joinedAt(m.getJoinedAt())
                        .profilePicture(m.getUser().getProfilePicture())
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

    @Transactional
    public void leaveRepository(Long repoId, Long callerId) {
        ResearchRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        assertMember(repoId, callerId);
        if (repo.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot leave their own repository. Transfer ownership or delete it instead.");
        }
        memberRepo.deleteByRepositoryIdAndUserId(repoId, callerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void assertOwner(ResearchRepository repo, Long callerId) {
        if (!repo.getOwner().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can perform this action");
        }
    }

    private void assertMember(Long repoId, Long callerId) {
        RepositoryMember member = memberRepo.findByRepositoryIdAndUserId(repoId, callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member"));
        boolean isLegacyOwner = "OWNER".equalsIgnoreCase(member.getRoleInRepo());
        if (!"ACCEPTED".equalsIgnoreCase(member.getStatus()) && !isLegacyOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invitation pending. Please accept the invitation first.");
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
                        .status(m.getStatus())
                        .joinedAt(m.getJoinedAt())
                        .profilePicture(m.getUser().getProfilePicture())
                        .build())
                .collect(Collectors.toList());
        base.setMembers(members);
        return base;
    }
}
