package com.mysite.sbb.document.web;

import com.mysite.sbb.document.domain.Document;
import com.mysite.sbb.document.domain.DocumentSection;
import com.mysite.sbb.document.dto.SectionDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DocumentEditForm {
    @NotBlank
    private String title;
    private String summary;

    private List<SectionInner> sections = new ArrayList<>();

    @Getter
    @Setter
    public static class SectionInner {
        private String heading;
        private String contentMd;
    }

    public static DocumentEditForm from(Document doc) {
        DocumentEditForm f = new DocumentEditForm();
        f.setTitle(doc.getTitle());
        f.setSummary(doc.getSummary());
        for (DocumentSection s : doc.getSections()) {
            SectionInner in = new SectionInner();
            in.setHeading(s.getHeading());
            in.setContentMd(s.getContentMd());
            f.getSections().add(in);
        }
        if (f.getSections().isEmpty())
            f.getSections().add(new SectionInner());
        return f;
    }

    public List<SectionDto> toSectionDtos() {
        List<SectionDto> list = new ArrayList<>();
        for (SectionInner s : sections) {
            if ((s.getHeading() != null && !s.getHeading().isBlank())
                    || (s.getContentMd() != null && !s.getContentMd().isBlank())) {
                list.add(new SectionDto(
                        s.getHeading() == null ? "" : s.getHeading(),
                        s.getContentMd() == null ? "" : s.getContentMd()));
            }
        }
        return list;
    }
}
