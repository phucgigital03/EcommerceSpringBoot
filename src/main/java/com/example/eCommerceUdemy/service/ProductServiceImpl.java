package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.Cart;
import com.example.eCommerceUdemy.model.Category;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.CartDTO;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.payload.ProductResponse;
import com.example.eCommerceUdemy.repository.CartRepository;
import com.example.eCommerceUdemy.repository.CategoryRepository;
import com.example.eCommerceUdemy.repository.ProductRepository;
import com.example.eCommerceUdemy.util.ConstructImageUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    FileService fileService;
    @Value("${project.image}")
    private String path;
    @Autowired
    private CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ConstructImageUtil constructImageUtil;


    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

//      check product is presenting with name
        boolean isProductNotPresent = true;
        List<Product> products = category.getProducts();
        for(int i = 0; i < products.size(); i++){
            if(products.get(i).getProductName().equals(productDTO.getProductName())){
                isProductNotPresent = false;
                break;
            }
        }

        if(isProductNotPresent){
//          convert DTO->Entity
            Product product = modelMapper.map(productDTO, Product.class);
            product.setProductId(null);
            product.setCategory(category);
            product.setImage("default.png");
            double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);
        }else{
            throw new APIException("Product already exists");
        }
    }

    @Override
    @Transactional
    public ProductDTO addProductWithImage(Long categoryId, ProductDTO productDTO, MultipartFile imageFile) {
        // check category exist
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // check product exist
        boolean isProductNotPresent = true;
        List<Product> products = category.getProducts();
        for(Product product : products){
//            System.out.println(product);
            if(product.getProductName().equals(productDTO.getProductName())){
                isProductNotPresent = false;
                break;
            }
        }

        if(isProductNotPresent){
            String fileName = "default.png";
            String filePath = null;
            try {
                //save fileImage and get fileName
                Map<String, String> fileNameAndFilePath = fileService.saveFile(path, imageFile);
                filePath = fileNameAndFilePath.get("filePath");
                fileName = fileNameAndFilePath.get("fileName");

                //convert DTO->Entity
                Product product = modelMapper.map(productDTO, Product.class);
                product.setProductId(null);
                product.setCategory(category);
                double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
                product.setSpecialPrice(specialPrice);

                product.setImage(fileName);
                Product savedProduct = productRepository.save(product);
                return modelMapper.map(savedProduct, ProductDTO.class);

            }catch (IllegalArgumentException e){
                System.out.println("IllegalArgumentException: " + e.getMessage());
                // Case 1: One file upload (I am at case 1)
                // Case 2: Multiple files uploads, you can
                // see addProductWithImages and addProductWithImagesHandleErrors
                throw new APIException(HttpStatus.BAD_REQUEST,"Failed to save image: " + e.getMessage());
            } catch (Exception e){
                // If DB save fails, delete the uploaded image
                if (filePath != null && Files.exists(Paths.get(filePath))) {
                    try {
                        Files.delete(Paths.get(filePath));
                        System.out.println("Rolled back image file: " + filePath);
                    } catch (IOException ex) {
                        System.err.println("Failed to delete uploaded image: " + ex.getMessage());
                    }
                }
                throw new APIException(HttpStatus.BAD_REQUEST,"Failed to save product or image");
            }
        }else{
            throw new APIException("Product already exists");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
//      sort
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize,sortByAndOrder);
//      Specification is part Spring Data JPA
        Specification<Product> spec = Specification.where(null);
        if(keyword != null && !keyword.isEmpty()){
            spec = spec.and(((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("productName")),
                            "%" + keyword.toLowerCase() + "%"
                    )));
        }
        if(category != null && !category.isEmpty()){
            spec = spec.and(((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            root.get("category").get("categoryName"),
                            category
                    )));
        }
        Page<Product> productPage = productRepository.findAll(spec,pageable);
//      get products
        List<Product> products = productPage.getContent();
        if(products.isEmpty()){
            throw new APIException("No products found");
        }
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUtil.constructImage(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getNumberOfElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
//      sort and find Products By Category
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize,sortByAndOrder);
        Page<Product> productPage = productRepository.findProductsByCategoryId(category.getCategoryId(),pageable);
//      get products
        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getNumberOfElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByKeyWord(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
//      sort and Products By KeyWord
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize,sortByAndOrder);
        Page<Product> productPage = productRepository.findProductsByLikeProductName(keyword,pageable);
//      get products
        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getNumberOfElements());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        Category categoryDB = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDTO.getCategoryId()));
//      convert DTO->Entity
        Product product = modelMapper.map(productDTO, Product.class);

        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setPrice(product.getPrice());
        double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
        productFromDB.setSpecialPrice(specialPrice);
        productFromDB.setCategory(categoryDB);
//        productFromDB.setImage(product.getImage());

        Product updatedProduct = productRepository.save(productFromDB);

//      update cartItem and cart
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(p -> {
                        ProductDTO pDTO =  modelMapper.map(p.getProduct(), ProductDTO.class);
                        pDTO.setQuantity(p.getQuantity());
                        return pDTO;
                    }).collect(Collectors.toList());
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();
        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    @Override
    @Transactional
    public ProductDTO deleteProduct(Long productId) {
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Delete product from cart first
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> {
            cartService.deleteProductFromCart(cart.getCartId(), productId);
        });

        productRepository.deleteProductById(productId);
        return modelMapper.map(productFromDB, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImg(Long productId, MultipartFile image) throws IOException {
//      get product from DB
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
//      upload image to server
//      get the file name of uploaded image
        String fileName = fileService.uploadImage(path,image);
//      Updating the new file name to the product
        productFromDB.setImage(fileName);
//      save updated product
        Product updatedProduct = productRepository.save(productFromDB);
//      return DTO after mapping product to DTO
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }


    // handle with multiple images that saved
    public ProductDTO addProductWithImages(Long categoryId, ProductDTO productDTO, List<MultipartFile> imageFiles) {
        List<String> savedFilePaths = new ArrayList<>();
        List<String> savedFileNames = new ArrayList<>();

        try {
            // Upload all images
            for (MultipartFile image : imageFiles) {
                Map<String, String> fileResult = fileService.saveFile(path, image);
                savedFilePaths.add(fileResult.get("filePath"));
                savedFileNames.add(fileResult.get("fileName"));
            }

            // Save product
            Product product = modelMapper.map(productDTO, Product.class);
            product.setProductId(null);
            product.setCategory(categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)));

            product.setImage(String.join(",", savedFileNames));
            double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
            product.setSpecialPrice(specialPrice);

            Product savedProduct = productRepository.save(product);

            return modelMapper.map(savedProduct, ProductDTO.class);

        } catch (Exception e) {
            // Rollback any saved images
            for (String filePath : savedFilePaths) {
                try {
                    Files.deleteIfExists(Paths.get(filePath));
                    System.out.println("Rolled back image: " + filePath);
                } catch (IOException ex) {
                    System.err.println("Failed to delete file: " + filePath + " | " + ex.getMessage());
                }
            }
            throw new APIException("Failed to save product or image(s): " + e.getMessage());
        }
    }


    // handle with multiple images that saved
    // handle multiple errors
    public ProductDTO addProductWithImagesHandleErrors(Long categoryId, ProductDTO productDTO, List<MultipartFile> imageFiles) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        boolean isProductNotPresent = category.getProducts().stream()
                .noneMatch(product -> product.getProductName().equals(productDTO.getProductName()));

        if (!isProductNotPresent) {
            throw new APIException("Product already exists");
        }

        List<String> savedFilePaths = new ArrayList<>();
        List<String> savedFileNames = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();

        for (MultipartFile imageFile : imageFiles) {
            try {
                // Validate manually
                if (imageFile.getOriginalFilename() == null || imageFile.getOriginalFilename().isBlank()) {
                    validationErrors.add("Invalid filename for one of the images.");
                    continue;
                }

                if (imageFile.getSize() > (2 * 1024 * 1024)) { // e.g. 2MB limit
                    validationErrors.add("File '" + imageFile.getOriginalFilename() + "' exceeds size limit.");
                    continue;
                }

                Map<String, String> result = fileService.saveFile(path, imageFile);
                savedFilePaths.add(result.get("filePath"));
                savedFileNames.add(result.get("fileName"));

            } catch (Exception e) {
                validationErrors.add("Error processing file '" + imageFile.getOriginalFilename() + "': " + e.getMessage());
            }
        }

        if (!validationErrors.isEmpty()) {
            // Rollback any saved files
            for (String filePath : savedFilePaths) {
                try {
                    Files.deleteIfExists(Paths.get(filePath));
                } catch (IOException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw new APIException(HttpStatus.BAD_REQUEST, String.join(" | ", validationErrors));
        }

        // Save product if all files are valid
        Product product = modelMapper.map(productDTO, Product.class);
        product.setProductId(null);
        product.setCategory(category);
        product.setSpecialPrice(product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01));
        product.setImage(savedFileNames.get(0)); // Main image
        Product savedProduct = productRepository.save(product);

        ProductDTO responseDTO = modelMapper.map(savedProduct, ProductDTO.class);
        responseDTO.setImage(savedFileNames.get(0));
        return responseDTO;
    }

}


