package edu.cit.uy.researchcenter.service;

import edu.cit.uy.researchcenter.dto.*;
import edu.cit.uy.researchcenter.model.*;
import edu.cit.uy.researchcenter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final MaterialRequestRepo requestRepo;
    private final ResearchRepositoryRepo repositoryRepo;
    private final RepositoryMemberRepo memberRepo;
    private final MaterialRepo materialRepo;
    private final UserRepository userRepository;

    // ── Create request ────────────────────────────────────────────────────
    @Transactional
    public RequestResponse create(User requester, CreateRequestDto dto) {
        ResearchRepository repo = repositoryRepo.findById(dto.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        if (!memberRepo.existsByRepositoryIdAndUserId(repo.getId(), requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }

        MaterialRequest req = MaterialRequest.builder()
                .repository(repo)
                .requester(requester)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build();

        return toResponse(requestRepo.save(req));
    }

    // ── Get by repo ───────────────────────────────────────────────────────
    public List<RequestResponse> getByRepo(Long repoId, Long callerId) {
        if (!memberRepo.existsByRepositoryIdAndUserId(repoId, callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }
        return requestRepo.findByRepositoryId(repoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────
    public RequestResponse getById(Long id, Long callerId) {
        MaterialRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!memberRepo.existsByRepositoryIdAndUserId(req.getRepository().getId(), callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }
        return toResponse(req);
    }

    // ── Fulfill request ───────────────────────────────────────────────────
    @Transactional
    public RequestResponse fulfill(Long requestId, Long callerId, FulfillRequestDto dto) {
        MaterialRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!"OPEN".equals(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is already fulfilled or cancelled");
        }
        if (!memberRepo.existsByRepositoryIdAndUserId(req.getRepository().getId(), callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a repository member");
        }

        Material material = materialRepo.findById(dto.getMaterialId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        // Verify material belongs to same repo
        if (!material.getRepository().getId().equals(req.getRepository().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material does not belong to this repository");
        }

        // Load the fulfiller user
        User fulfiller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        req.setStatus("FULFILLED");
        req.setFulfilledAt(Instant.now());
        req.setMaterial(material);
        req.setFulfilledBy(fulfiller);

        return toResponse(requestRepo.save(req));
    }

    // ── Delete request ────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, Long callerId) {
        MaterialRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // Only the requester (who created the request) can delete it
        if (!req.getRequester().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the request creator can delete this request");
        }

        requestRepo.deleteById(id);
    }

    // ── Update fulfilled request's material ────────────────────────────────
    @Transactional
    public RequestResponse updateMaterial(Long requestId, Long callerId, FulfillRequestDto dto) {
        MaterialRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // Check status first
        if (!"FULFILLED".equals(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only fulfilled requests can be updated");
        }

        // Check if fulfiller exists and is the caller
        if (req.getFulfilledBy() == null || !req.getFulfilledBy().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the fulfiller can update this request");
        }

        Material material = materialRepo.findById(dto.getMaterialId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        // Verify material belongs to same repo
        if (!material.getRepository().getId().equals(req.getRepository().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material does not belong to this repository");
        }

        req.setMaterial(material);
        return toResponse(requestRepo.save(req));
    }

    // ── Mapper ────────────────────────────────────────────────────────────
    private RequestResponse toResponse(MaterialRequest r) {
        return RequestResponse.builder()
                .id(r.getId())
                .repositoryId(r.getRepository().getId())
                .repositoryName(r.getRepository().getName())
                .title(r.getTitle())
                .description(r.getDescription())
                .requesterId(r.getRequester().getId())
                .requesterName(r.getRequester().getFirstName() + " " + r.getRequester().getLastName())
                .status(r.getStatus())
                .fulfilledBy(r.getFulfilledBy() != null ? r.getFulfilledBy().getId() : null)
                .fulfilledByName(r.getFulfilledBy() != null
                        ? r.getFulfilledBy().getFirstName() + " " + r.getFulfilledBy().getLastName() : null)
                .fulfilledAt(r.getFulfilledAt())
                .materialId(r.getMaterial() != null ? r.getMaterial().getId() : null)
                .materialTitle(r.getMaterial() != null ? r.getMaterial().getTitle() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
