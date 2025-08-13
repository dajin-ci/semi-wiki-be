package com.mysite.sbb.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysite.sbb.document.domain.Document;
import com.mysite.sbb.document.domain.DocumentRevision;
import com.mysite.sbb.document.domain.DocumentSection;
import com.mysite.sbb.document.dto.DocumentCreateDto;
import com.mysite.sbb.document.dto.SectionDto;
import com.mysite.sbb.document.repository.DocumentRepository;
import com.mysite.sbb.document.repository.DocumentRevisionRepository;
import com.mysite.sbb.document.repository.DocumentSectionRepository;
import com.mysite.sbb.document.util.SlugGenerator;
import com.mysite.sbb.user.SiteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository docRepo;
    private final DocumentSectionRepository sectionRepo;
    private final DocumentRevisionRepository revisionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 생성 */
    @Transactional
    public Document create(DocumentCreateDto dto, SiteUser author) {
        Document doc = new Document();

        // 사용자가 직접 입력한 슬러그가 있으면 그걸, 없으면 제목 기반으로
        String base = (dto.slug() != null && !dto.slug().isBlank())
                ? SlugGenerator.toSlug(dto.slug())
                : SlugGenerator.toSlug(dto.title());

        String slug = ensureUniqueSlug(base); // 중복이면 -2, -3 … 붙임

        doc.setSlug(slug);
        doc.setTitle(dto.title());
        doc.setSummary(dto.summary());
        doc.setCoverImageUrl(dto.coverImageUrl());
        doc.setCreatedBy(author);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        Document saved = docRepo.save(doc);

        int i = 0;
        if (dto.sections() != null) {
            for (SectionDto s : dto.sections()) {
                DocumentSection sec = new DocumentSection();
                sec.setDocument(saved);
                sec.setOrderIndex(i++);
                sec.setHeading(s.heading());
                sec.setContentMd(s.contentMd());
                sec.setCreatedAt(LocalDateTime.now());
                sec.setUpdatedAt(LocalDateTime.now());
                sectionRepo.save(sec);
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Document getBySlug(String slug) {
        return docRepo.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));
    }

    // 리비전 버전 계산
    private int nextVersion(Document doc) {
        return revisionRepo.findByDocumentOrderByVersionDesc(doc).stream()
                .map(DocumentRevision::getVersion)
                .findFirst().orElse(0) + 1;
    }

    // 문서 스냅샷(JSON)
    private String snapshot(Document doc) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("title", doc.getTitle());
            root.put("summary", doc.getSummary());

            ArrayNode arr = root.putArray("sections");
            for (DocumentSection s : doc.getSections()) {
                ObjectNode sec = objectMapper.createObjectNode();
                sec.put("heading", s.getHeading());
                sec.put("contentMd", s.getContentMd());
                arr.add(sec);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    // 리비전 저장
    private void saveRevision(Document doc, SiteUser editor, String snap, int version) {
        DocumentRevision r = new DocumentRevision();
        r.setDocument(doc);
        r.setVersion(version);
        r.setSnapshotJson(snap);
        r.setEditor(editor);
        r.setCreatedAt(LocalDateTime.now());
        revisionRepo.save(r);
    }

    /** 수정 + 리비전 */
    @Transactional
    public void update(String slug, String newTitle, String newSummary, List<SectionDto> sections, SiteUser editor) {
        Document doc = docRepo.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // 기존 섹션 삭제 후 재삽입(MVP)
        sectionRepo.deleteAll(sectionRepo.findByDocumentOrderByOrderIndexAsc(doc));

        doc.setTitle(newTitle);
        doc.setSummary(newSummary);

        int i = 0;
        for (SectionDto s : sections) {
            DocumentSection sec = new DocumentSection();
            sec.setDocument(doc);
            sec.setOrderIndex(i++);
            sec.setHeading(s.heading());
            sec.setContentMd(s.contentMd());
            sec.setCreatedAt(LocalDateTime.now());
            sec.setUpdatedAt(LocalDateTime.now());
            sectionRepo.save(sec);
        }
        doc.setUpdatedAt(LocalDateTime.now());

        saveRevision(doc, editor, snapshot(doc), nextVersion(doc));
    }

    /** 슬러그 유니크 보장 */
    private String ensureUniqueSlug(String base) {
        String candidate = base;
        int n = 2;
        while (docRepo.existsBySlug(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }
}
