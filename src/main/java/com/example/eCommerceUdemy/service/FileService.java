package com.example.eCommerceUdemy.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface FileService {
    String uploadImage(String path, MultipartFile image) throws IOException;

    Map<String, String> saveFile(String path, MultipartFile file) throws IOException;
}
