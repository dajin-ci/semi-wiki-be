package com.mysite.sbb.document.web;

import com.mysite.sbb.document.dto.DocumentCreateDto;
import com.mysite.sbb.document.dto.SectionDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DocumentCreateForm {

    @NotBlank
    private String title;

    private String summary;

    /** 간단 버전: 섹션 1개를 기본 제공 */
    private List<SectionInner> sections = new ArrayList<>();

    public DocumentCreateForm() {
        sections.add(new SectionInner()); // 섹션 1개 기본 생성
    }

    @Getter
    @Setter
    public static class SectionInner {
        private String heading; // 예: "개요"
        private String contentMd; // 마크다운 본문
    }

    /** 폼 → DTO 변환 */
    public DocumentCreateDto toDto() {
        List<SectionDto> list = new ArrayList<>();
        for (SectionInner s : sections) {
            if ((s.getHeading() != null && !s.getHeading().isBlank())
                    || (s.getContentMd() != null && !s.getContentMd().isBlank())) {
                list.add(new SectionDto(
                        s.getHeading() == null ? "" : s.getHeading(),
                        s.getContentMd() == null ? "" : s.getContentMd()));
            }
        }
        return new DocumentCreateDto(title, summary, null, list, null);
    }
}
