package com.fooddrinks.repository;

import com.fooddrinks.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    List<Suggestion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
