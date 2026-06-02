package edu.cit.uy.researchcenter.features.activity.controller;

import edu.cit.uy.researchcenter.features.activity.model.Activity;
import edu.cit.uy.researchcenter.features.activity.repository.ActivityRepository;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import edu.cit.uy.researchcenter.features.auth.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityRepository activityRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    private void populateProfilePicture(Activity activity) {
        if (activity != null && activity.getUserId() != null) {
            userRepository.findById(activity.getUserId())
                    .ifPresent(u -> activity.setActorProfilePicture(u.getProfilePicture()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getActivities(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long repositoryId) {
        
        User user = userService.findByEmail(principal.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<Activity> activities;
        
        if (repositoryId != null) {
            // Repo activity (exclude private notes)
            activities = activityRepository.findByRepositoryIdAndTargetTypeNotOrderByCreatedAtDesc(repositoryId, "PRIVATE_NOTE", pageable);
        } else {
            // Personal activity: always show the current user's own actions.
            activities = activityRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        activities.getContent().forEach(this::populateProfilePicture);

        return ResponseEntity.ok(wrap(true, activities.getContent(), null));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Activity> activities = activityRepository.findAllByOrderByCreatedAtDesc(pageable);
        activities.getContent().forEach(this::populateProfilePicture);
        return ResponseEntity.ok(wrap(true, activities.getContent(), null));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userService.findByEmail(principal.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Activity> notifications;
        if ("ADMIN".equals(user.getRole())) {
            notifications = activityRepository.findAllAdminNotifications(user.getId(), pageable);
        } else {
            notifications = activityRepository.findNotificationsForUser(user.getId(), pageable);
        }

        notifications.getContent().forEach(this::populateProfilePicture);

        return ResponseEntity.ok(wrap(true, notifications.getContent(), null));
    }

    private Map<String, Object> wrap(boolean success, Object data, Object error) {
        return Map.of("success", success, "data", data != null ? data : Map.of(),
                "error", error != null ? error : Map.of(), "timestamp", Instant.now().toString());
    }
}
