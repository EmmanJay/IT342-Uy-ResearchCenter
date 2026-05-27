package edu.cit.uy.researchcenter.features.activity.service;

import edu.cit.uy.researchcenter.features.activity.model.Activity;
import edu.cit.uy.researchcenter.features.activity.repository.ActivityRepository;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.repository.model.ResearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(User actor, String action, String targetType, Long targetId, String targetName, ResearchRepository repo, Long targetUserId, String description) {
        String actorName = actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : "System";
        if (actorName.isEmpty() && actor != null) {
            actorName = actor.getEmail();
        }
        Activity activity = Activity.builder()
                .userId(actor != null ? actor.getId() : null)
                .actorName(actorName)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .repositoryId(repo != null ? repo.getId() : null)
                .repositoryName(repo != null ? repo.getName() : null)
                .targetUserId(targetUserId)
                .description(description)
                .build();
        activityRepository.save(activity);
    }
}
