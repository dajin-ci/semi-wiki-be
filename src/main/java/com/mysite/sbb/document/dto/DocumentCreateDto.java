package com.mysite.sbb.document.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record DocumentCreateDto(
        @NotBlank String title,
        String summary,
        String coverImageUrl,
        List<SectionDto> sections,
        String slug // 옵션: 직접 입력 허용(없으면 생성)
) {
}