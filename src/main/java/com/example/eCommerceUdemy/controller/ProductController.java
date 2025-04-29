package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.config.AppConsants;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.CategoryResponse;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.payload.ProductResponse;
import com.example.eCommerceUdemy.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ProductController {
    @Autowired
    ProductService productService;

    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<ProductDTO> createProduct(
            @RequestBody ProductDTO productDTO,
            @PathVariable Long categoryId
    ) {
        ProductDTO savedproductDTO = productService.addProduct(categoryId, productDTO);
        return new ResponseEntity<>(savedproductDTO, HttpStatus.CREATED);
    }

    @PostMapping("/admin/categories/{categoryId}/productWithImage")
    public ResponseEntity<ProductDTO> createProductWithImage(
            @RequestPart(name = "productDTO") ProductDTO productDTO,
            @RequestPart(name = "imageFile") MultipartFile imageFile,
            @PathVariable Long categoryId
    ) {
        ProductDTO savedproductDTO = productService.addProductWithImage(categoryId, productDTO, imageFile);
        return new ResponseEntity<>(savedproductDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "pageNumber",defaultValue = AppConsants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConsants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy",defaultValue = AppConsants.SORT_PRODUCTID_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder",defaultValue = AppConsants.SORT_ORDER, required = false) String sortOrder
    ) {
        ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize,sortBy,sortOrder,keyword,category);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(
            @RequestBody ProductDTO productDTO,
            @PathVariable Long productId
    ){
        ProductDTO updatedProduct = productService.updateProduct(productId,productDTO);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @PutMapping("/products/{productId}/image")
    public ResponseEntity<ProductDTO> updateProductImage(
            @PathVariable Long productId,
            @RequestParam("image") MultipartFile image) throws IOException {
        ProductDTO productDTO = productService.updateProductImg(productId,image);
        return new ResponseEntity<>(productDTO, HttpStatus.OK);
    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long productId){
        ProductDTO productDTO = productService.deleteProduct(productId);
        return new ResponseEntity<>(productDTO, HttpStatus.OK);
    }

    @DeleteMapping("/admin/softProduct/{productId}")
    public ResponseEntity<?> softDeleteProduct(@PathVariable Long productId){
       String status = productService.softDeleteProduct(productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<ProductResponse> getProductsByKeyword(
            @PathVariable String keyword,
            @RequestParam(name = "pageNumber",defaultValue = AppConsants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConsants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy",defaultValue = AppConsants.SORT_PRODUCTID_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder",defaultValue = AppConsants.SORT_ORDER, required = false) String sortOrder
    ) {
        ProductResponse productResponse = productService.getProductsByKeyWord(keyword,pageNumber,pageSize, sortBy,sortOrder);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/categories/{categoryId}/product")
    public ResponseEntity<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(name = "pageNumber",defaultValue = AppConsants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConsants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy",defaultValue = AppConsants.SORT_PRODUCTID_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder",defaultValue = AppConsants.SORT_ORDER, required = false) String sortOrder
    ) {
        ProductResponse productResponse = productService.getProductsByCategory(categoryId,pageNumber,pageSize,sortBy,sortOrder);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

}
