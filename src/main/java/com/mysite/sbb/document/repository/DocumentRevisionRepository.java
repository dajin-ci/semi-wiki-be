package com.mysite.sbb.document.repository;

import com.mysite.sbb.document.domain.Document;
import com.mysite.sbb.document.domain.DocumentRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRevisionRepository extends JpaRepository<DocumentRevision, Long> {
    List<DocumentRevision> findByDocumentOrderByVersionDesc(Document document);
}
