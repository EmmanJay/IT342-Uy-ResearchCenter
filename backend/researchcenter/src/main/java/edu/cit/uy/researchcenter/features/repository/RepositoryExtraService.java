package edu.cit.uy.researchcenter.features.repository;

import edu.cit.uy.researchcenter.features.repository.model.PrivateNote;
import edu.cit.uy.researchcenter.features.repository.model.RepositoryBookmark;
import edu.cit.uy.researchcenter.features.repository.model.RepositoryUpdate;
import edu.cit.uy.researchcenter.features.repository.repository.PrivateNoteRepository;
import edu.cit.uy.researchcenter.features.repository.repository.RepositoryBookmarkRepository;
import edu.cit.uy.researchcenter.features.repository.repository.RepositoryUpdateRepository;
import edu.cit.uy.researchcenter.features.repository.repository.ResearchRepositoryRepo;
import edu.cit.uy.researchcenter.features.repository.model.ResearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepositoryExtraService {
    private final RepositoryBookmarkRepository bookmarkRepository;
    private final PrivateNoteRepository noteRepository;
    private final RepositoryUpdateRepository updateRepository;
    private final ResearchRepositoryRepo researchRepositoryRepo;

    @org.springframework.transaction.annotation.Transactional
    public boolean toggleBookmark(Long userId, Long repositoryId) {
        if (bookmarkRepository.existsByUserIdAndRepositoryId(userId, repositoryId)) {
            bookmarkRepository.deleteByUserIdAndRepositoryId(userId, repositoryId);
            return false;
        } else {
            RepositoryBookmark bookmark = new RepositoryBookmark();
            bookmark.setUserId(userId);
            bookmark.setRepositoryId(repositoryId);
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    public boolean isBookmarked(Long userId, Long repositoryId) {
        return bookmarkRepository.existsByUserIdAndRepositoryId(userId, repositoryId);
    }

    public PrivateNote getPrivateNote(Long userId, Long repositoryId) {
        return noteRepository.findByUserIdAndRepositoryId(userId, repositoryId)
                .orElse(null);
    }

    public PrivateNote savePrivateNote(Long userId, Long repositoryId, String content) {
        PrivateNote note = noteRepository.findByUserIdAndRepositoryId(userId, repositoryId)
                .orElse(new PrivateNote());
        note.setUserId(userId);
        note.setRepositoryId(repositoryId);
        note.setContent(content);
        return noteRepository.save(note);
    }

    public Page<RepositoryUpdate> getUpdates(Long repositoryId, int page, int size) {
        return updateRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId, PageRequest.of(page, size));
    }

    public RepositoryUpdate addUpdate(Long repositoryId, Long authorId, String authorName, String content) {
        RepositoryUpdate update = new RepositoryUpdate();
        update.setRepositoryId(repositoryId);
        update.setAuthorId(authorId);
        update.setAuthorName(authorName);
        update.setContent(content);
        return updateRepository.save(update);
    }

    public RepositoryUpdate editUpdate(Long updateId, Long userId, String content) {
        RepositoryUpdate update = updateRepository.findById(updateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Update not found"));
        if (!update.getAuthorId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own comments");
        }
        update.setContent(content);
        return updateRepository.save(update);
    }

    public void deleteUpdate(Long updateId, Long userId, Long repositoryId) {
        RepositoryUpdate update = updateRepository.findById(updateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Update not found"));
        // Check if user is author OR repo owner
        ResearchRepository repo = researchRepositoryRepo.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
        boolean isAuthor = update.getAuthorId().equals(userId);
        boolean isOwner = repo.getOwner().getId().equals(userId);
        if (!isAuthor && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }
        updateRepository.delete(update);
    }
}
