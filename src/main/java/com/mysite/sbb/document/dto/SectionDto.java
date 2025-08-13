package com.mysite.sbb.document.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SectionDto(
        @NotBlank String heading,
        @NotBlank String contentMd) {
}
