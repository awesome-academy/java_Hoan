package com.fooddrinks.repository;

import com.fooddrinks.entity.Suggestion;
import com.fooddrinks.entity.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    List<Suggestion> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Admin: list all suggestions with user eager-loaded (avoids N+1 on admin suggestion list).
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Suggestion> findAll(Pageable pageable);

    long countByStatus(SuggestionStatus status);
}
