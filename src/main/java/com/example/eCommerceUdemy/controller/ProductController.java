package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.config.AppConsants;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.CategoryResponse;
import com.example.eCommerceUdemy.payload.ImportResponse;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.payload.ProductResponse;
import com.example.eCommerceUdemy.service.ProductImportService;
import com.example.eCommerceUdemy.service.ProductSalesService;
import com.example.eCommerceUdemy.service.ProductService;
import com.example.eCommerceUdemy.util.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {
    @Autowired
    ProductService productService;
    @Autowired
    private ProductSalesService productSalesService;
    @Autowired
    private ProductImportService productImportService;

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

    @GetMapping("/public/products/count")
    public ResponseEntity<?> getProductsCount() {
        Long productCount = productService.getAllProductsCount();
        Map<String, Long> map = new HashMap<>();
        map.put("productCount", productCount);
        return new ResponseEntity<>(map, HttpStatus.OK);
    }


    @GetMapping("/products/top-products")
    public ResponseEntity<?> getTopNProductsSelling(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Map<String, Object>> data = productSalesService.getTopSellingProductsByDateRange(startDate, endDate);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @PostMapping("/products/import")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Please upload an Excel file!");
        }

        try {
            ImportResponse importResponse = productImportService.importProductsFromExcel(file);
            String summaryMessage = String.format(
                    "Import completed: %d of %d products imported successfully, %d failed.",
                    importResponse.getSuccessCount(),
                    importResponse.getTotalItems(),
                    importResponse.getFailureCount()
            );
            // Can return summaryMessage or importResponse
            return ResponseEntity.status(HttpStatus.OK).body(importResponse);
        } catch (Exception e) {
            String message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
        }
    }

    @GetMapping("/products/export")
    public ResponseEntity<Resource> getFile() {
        String filename = "products.xlsx";
        List<Product> products = productService.findByDeletedFalse();

        InputStreamResource file = new InputStreamResource(ExcelHelper.productsToExcel(products));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

}
