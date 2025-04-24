package com.example.eCommerceUdemy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService{

    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {
//      file names of current/original file
        String originalFilename = image.getOriginalFilename();

//      ***generate a unique file name
//      mat.jpg -> 124 -> 1234.jpg
//      originalFilename.lastIndexOf("."): Finds the last period (.) in image.jpg, which is at index 5.
//      originalFilename.substring(5): Extracts the file extension starting from index 5 (which is .jpg).
//      randomId.concat(".jpg"): Concatenates "abc123" with .jpg, resulting in "abc123.jpg".
//      File.separator generate slash "/" macos or "\\" windows
        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.concat(originalFilename.substring(originalFilename.lastIndexOf(".")));
        String filePath = path + File.separator + fileName;

//      check if path exist and create
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }

//      upload to server
        Files.copy(image.getInputStream(), Paths.get(filePath));

//      returning file name
        return fileName;
    }

    @Override
    public Map<String, String> saveFile(String path, MultipartFile imageFile) throws IOException {

        // === 1. Validate file ===
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or missing.");
        }

        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        // Validate file extension (you can change based on allowed types)
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        List<String> allowedExtensions = List.of(".jpg", ".jpeg", ".png", ".webp");

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file format: " + extension);
        }

        // Optional: Validate file size (e.g., max 2MB)
        long maxSizeInBytes = 2 * 1024 * 1024; // 2MB
        if (imageFile.getSize() > maxSizeInBytes) {
            throw new IllegalArgumentException("File size exceeds the 2MB limit.");
        }

        // 2.Generate unique file name
        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.concat(originalFilename.substring(originalFilename.lastIndexOf(".")));
        String filePath = path + File.separator + fileName;

        //3.check if path exist and create
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        //4.upload to server
        Files.copy(imageFile.getInputStream(), Paths.get(filePath));

        Map<String, String> result = new HashMap<>();
        result.put("fileName", fileName);
        result.put("filePath", filePath);

        return result;
    }
}
