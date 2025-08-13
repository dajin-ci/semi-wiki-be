//이미지 업로드 API
//에디터에서는 이미지 업로드 후 응답의 url을 마크다운 ![](url)로 넣으면 됩니다.
//(드래그&드롭 자동삽입은 Step 3에서 JS로 붙이면 돼요.)
package com.mysite.sbb.document.web;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/docs")
public class DocumentUploadController {

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/{slug}/attachments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> upload(@PathVariable String slug,
            @RequestParam("file") MultipartFile file) throws Exception {
        String uploadsDir = "src/main/resources/static/uploads";
        Files.createDirectories(Path.of(uploadsDir));

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "-" + (original == null ? "file" : original);
        Path target = Path.of(uploadsDir, filename);
        file.transferTo(target.toFile());

        return Map.of("url", "/uploads/" + filename);
    }
}
