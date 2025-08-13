package com.mysite.sbb.document.repository;

import com.mysite.sbb.document.domain.Document;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Document> findByTitleContainingIgnoreCase(String q, Pageable pageable);
}
