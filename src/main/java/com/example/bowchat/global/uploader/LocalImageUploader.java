package com.example.bowchat.global.uploader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalImageUploader implements ImageUploader{


    private final String uploadDir = "/Library/Study/uploads";

    @Override
    public String uploadImage(File file, String originalFilename) {
        try {
            String newFilename = System.currentTimeMillis() + "_" + originalFilename;
            Path targetPath = Paths.get(uploadDir, newFilename);

            Files.createDirectories(targetPath.getParent());
            // 파일을 지정된 경로로 복사
            Files.copy(file.toPath(), targetPath);

            // 클라이언트에는 상대경로 URL을 응답
            return "/uploads/" + newFilename;

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }
}
