package edu.cit.uy.researchcenter.features.auth.repository;

import edu.cit.uy.researchcenter.features.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailContainingIgnoreCase(String email);

    boolean existsByEmail(String email);
}
