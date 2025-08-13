package com.mysite.sbb.document.repository;

import com.mysite.sbb.document.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentSectionRepository extends JpaRepository<DocumentSection, Long> {
    List<DocumentSection> findByDocumentOrderByOrderIndexAsc(Document doc);
}
