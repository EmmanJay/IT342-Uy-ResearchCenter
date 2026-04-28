package edu.cit.uy.researchcenter.features.auth.repository;

import edu.cit.uy.researchcenter.features.auth.model.RefreshToken;
import edu.cit.uy.researchcenter.features.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isValid = false WHERE rt.user = :user")
    void invalidateAllByUser(User user);
}
