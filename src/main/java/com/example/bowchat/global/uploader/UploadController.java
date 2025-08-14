package com.example.bowchat.global.uploader;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final LocalImageUploader imageUploader;

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile multipartFile) {
        try {
            log.info("업로드 요청 파일: {}", multipartFile.getOriginalFilename());
            log.info("파일 크기: {}", multipartFile.getSize());
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);
            log.info("성공");
            String url = imageUploader.uploadImage(tempFile, multipartFile.getOriginalFilename());
            return ResponseEntity.ok().body(new UploadResponse(url));

        } catch (Exception e) {
            log.info("실패");
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }
}