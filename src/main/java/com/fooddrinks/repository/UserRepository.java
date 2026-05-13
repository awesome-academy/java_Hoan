package com.fooddrinks.repository;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByIsActive(Boolean isActive);

    /**
     * Looks up a user by their OAuth2 provider and provider-assigned ID.
     * Used for Apple Sign In subsequent logins where email is not returned in the id_token.
     */
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
