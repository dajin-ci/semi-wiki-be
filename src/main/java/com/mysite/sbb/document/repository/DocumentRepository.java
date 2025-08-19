package com.mysite.sbb.document.repository;

import com.mysite.sbb.document.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 기본 조회
    Optional<Document> findBySlug(String slug);

    // 슬러그 중복 체크
    boolean existsBySlug(String slug);

    // 제목 검색(대소문자 무시)
    Page<Document> findByTitleContainingIgnoreCase(String q, Pageable pageable);

    // 상세에서 섹션까지 즉시 로딩
    @Query("select d from Document d left join fetch d.sections where d.slug = :slug")
    Optional<Document> findBySlugWithSections(@Param("slug") String slug);
}
