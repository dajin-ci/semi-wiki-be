package com.mysite.sbb.document.web;

import com.mysite.sbb.document.domain.Document;
import com.mysite.sbb.document.dto.DocumentCreateDto;
import com.mysite.sbb.document.service.DocumentService;
import com.mysite.sbb.document.service.MarkdownService;
import com.mysite.sbb.document.repository.DocumentRevisionRepository;
import com.mysite.sbb.document.repository.DocumentRepository;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository docRepo;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final DocumentRevisionRepository revisionRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        PageRequest pr = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> docs = q.isBlank() ? docRepo.findAll(pr) : docRepo.findByTitleContainingIgnoreCase(q, pr);
        model.addAttribute("docs", docs);
        model.addAttribute("q", q);
        return "docs/list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new DocumentCreateForm());
        return "docs/new_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public String create(@Valid DocumentCreateForm form, Principal principal) {
        SiteUser me = userService.getUser(principal.getName());
        DocumentCreateDto dto = form.toDto();
        var saved = documentService.create(dto, me);
        return "redirect:/docs/" + saved.getSlug();
    }

    /** 상세에서 MD → HTML 변환한 섹션을 내려주기 */
    public record SectionView(String heading, String html) {
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        var doc = documentService.getBySlug(slug);
        var views = doc.getSections().stream()
                .map(s -> new SectionView(s.getHeading(), markdownService.toHtml(s.getContentMd())))
                .toList();
        model.addAttribute("doc", doc);
        model.addAttribute("sections", views); // HTML 포함
        return "docs/detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{slug}/edit")
    public String editForm(@PathVariable String slug, Model model) {
        var doc = documentService.getBySlug(slug);
        model.addAttribute("form", DocumentEditForm.from(doc));
        model.addAttribute("slug", slug);
        return "docs/edit_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{slug}/edit")
    public String edit(@PathVariable String slug, @Valid DocumentEditForm form, Principal principal) {
        var me = userService.getUser(principal.getName());
        documentService.update(slug, form.getTitle(), form.getSummary(), form.toSectionDtos(), me);
        return "redirect:/docs/" + slug;
    }

    @GetMapping("/{slug}/history")
    public String history(@PathVariable String slug, Model model) {
        var doc = documentService.getBySlug(slug);
        var revs = revisionRepo.findByDocumentOrderByVersionDesc(doc);
        model.addAttribute("doc", doc);
        model.addAttribute("revisions", revs);
        return "docs/history";
    }
}
