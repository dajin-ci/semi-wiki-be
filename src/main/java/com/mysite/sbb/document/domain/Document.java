package com.mysite.sbb.document.domain;

import com.mysite.sbb.user.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // H2/MySQL 공통
    private Long id;

    /** 문서 URL용 키 (예: ragnar-lothbrok, q-123) */
    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    /** 문서 제목 */
    @Column(nullable = false)
    private String title;

    /** 요약(선택) */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 표지 이미지(선택) */
    @Column(length = 512)
    private String coverImageUrl;

    /** 생성자(작성자) */
    @ManyToOne(fetch = FetchType.LAZY)
    private SiteUser createdBy;

    /** 생성/수정 시각 */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 섹션들 (개요/업적/사건 등) */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DocumentSection> sections = new ArrayList<>();
}
