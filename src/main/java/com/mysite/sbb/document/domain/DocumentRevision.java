package com.mysite.sbb.document.domain;

import com.mysite.sbb.user.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "document_revisions")
public class DocumentRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String snapshotJson; // 수정 시점의 전체 스냅샷(JSON)

    @ManyToOne(fetch = FetchType.LAZY)
    private SiteUser editor;

    private LocalDateTime createdAt;
}
