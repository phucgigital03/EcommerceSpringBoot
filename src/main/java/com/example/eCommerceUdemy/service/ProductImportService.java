package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.ImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImportService {
    ImportResponse importProductsFromExcel(MultipartFile file);
}
