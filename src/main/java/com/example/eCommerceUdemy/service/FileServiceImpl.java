package com.example.eCommerceUdemy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
}
