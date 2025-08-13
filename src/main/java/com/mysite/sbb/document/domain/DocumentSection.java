package com.mysite.sbb.document.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "document_sections")
public class DocumentSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private String heading;

    @Column(columnDefinition = "TEXT")
    private String contentMd;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}