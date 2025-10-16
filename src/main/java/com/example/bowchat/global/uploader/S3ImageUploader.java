package com.example.bowchat.global.uploader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3ImageUploader implements ImageUploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    @Override
    public String uploadImage(File file, String originalFileName) {
        String fileName = "uploads/" + UUID.randomUUID() + "-" + originalFileName;

        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(getContentType(originalFileName));

            amazonS3.putObject(bucketName, fileName, fileInputStream, metadata);
            String fileUrl = amazonS3.getUrl(bucketName, fileName).toString();

            log.info("S3 업로드 완료: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패: " + originalFileName, e);
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".png")) return "image/png";
        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        else if (fileName.endsWith(".gif")) return "image/gif";
        else return "application/octet-stream";
    }
}
