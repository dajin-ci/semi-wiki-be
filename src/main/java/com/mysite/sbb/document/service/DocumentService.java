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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    /** 작성자 권한 확인 (수정/삭제 공통) */
    private void assertAuthor(Document doc, SiteUser user, String action) {
        if (doc.getCreatedBy() == null || user == null ||
                !doc.getCreatedBy().getUsername().equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, action + " 권한이 없습니다.");
        }
    }

    /** 수정 + 리비전 */
    @Transactional
    public void update(String slug, String newTitle, String newSummary,
            List<SectionDto> sections, SiteUser editor) {

        // 🔹 섹션까지 함께 가져오기
        Document doc = docRepo.findBySlugWithSections(slug)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // 🔹 제목/요약 갱신
        doc.setTitle(newTitle);
        doc.setSummary(newSummary);
        doc.setUpdatedAt(LocalDateTime.now());

        // 🔹 이전 섹션 전량 제거 (orphanRemoval=true 이므로 DB에서 자동 삭제)
        doc.getSections().clear();

        // 🔹 새 섹션 삽입 (orderIndex 재부여)
        int i = 0;
        for (SectionDto s : sections) {
            DocumentSection sec = new DocumentSection();
            sec.setDocument(doc); // 관계 설정
            sec.setOrderIndex(i++);
            sec.setHeading(s.heading());
            sec.setContentMd(s.contentMd());
            sec.setCreatedAt(LocalDateTime.now());
            sec.setUpdatedAt(LocalDateTime.now());
            doc.getSections().add(sec); // 컬렉션에 추가(중요)
        }

        // 🔹 리비전 저장 (스냅샷은 doc.getSections() 기준이라 중복 없이 저장됩니다)
        saveRevision(doc, editor, snapshot(doc), nextVersion(doc));
    }

    /** 삭제 (섹션/리비전 정리 후 문서 삭제) */
    @Transactional
    public void delete(String slug, SiteUser requester) {
        Document doc = docRepo.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // ★ 작성자만 삭제 가능
        assertAuthor(doc, requester, "삭제");

        // 자식 먼저 정리(캐스케이드가 설정되어 있지 않은 경우)
        revisionRepo.deleteAll(revisionRepo.findByDocumentOrderByVersionDesc(doc));
        sectionRepo.deleteAll(sectionRepo.findByDocumentOrderByOrderIndexAsc(doc));
        // 댓글이 있다면: commentRepo.deleteAllByDocument(doc);

        docRepo.delete(doc);
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

    @Transactional
    public Document getOrCreateForQuestion(com.mysite.sbb.question.Question q,
            com.mysite.sbb.user.SiteUser authorIfCreate) {
        String slug = "q-" + q.getId();
        return docRepo.findBySlug(slug).orElseGet(() -> {
            var dto = new com.mysite.sbb.document.dto.DocumentCreateDto(
                    q.getSubject(), null, null,
                    java.util.List.of(new com.mysite.sbb.document.dto.SectionDto("개요", q.getContent())),
                    slug);
            // 질문 작성자를 fallback으로 사용
            SiteUser author = (authorIfCreate != null) ? authorIfCreate : q.getAuthor();
            return create(dto, author);
        });
    }

}
