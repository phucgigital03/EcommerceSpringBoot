package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.payload.ProductResponse;
import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    ProductDTO addProduct(Long categoryId, ProductDTO productDTO);

    ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category);

    ProductResponse getProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductResponse getProductsByKeyWord(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductDTO updateProduct(Long productId, ProductDTO productDTO);

    ProductDTO deleteProduct(Long productId);

    ProductDTO updateProductImg(Long productId, MultipartFile image) throws IOException;

    @Transactional
    ProductDTO addProductWithImage(Long categoryId, ProductDTO productDTO, MultipartFile imageFile);

    String softDeleteProduct(Long productId);

    Long getAllProductsCount();

    List<Product> findByDeletedFalse();
}
