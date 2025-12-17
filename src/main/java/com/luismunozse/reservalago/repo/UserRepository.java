package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u.phone FROM User u WHERE u.phone IS NOT NULL AND u.phone <> '' AND u.enabled = true AND u.role IN ('ADMIN', 'MANAGER')")
    List<String> findAdminPhones();
}



