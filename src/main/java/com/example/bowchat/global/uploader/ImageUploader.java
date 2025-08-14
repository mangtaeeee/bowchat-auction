package com.example.bowchat.global.uploader;

import java.io.File;

public interface ImageUploader {
    String uploadImage(File file, String originalFileName);
}
