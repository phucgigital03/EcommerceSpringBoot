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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
//      convert DTO->Entity
        Product product = modelMapper.map(productDTO, Product.class);

        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setPrice(product.getPrice());
        double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() * 0.01);
        productFromDB.setSpecialPrice(specialPrice);
//        productFromDB.setCategory(product.getCategory());
//        productFromDB.setSpecialPrice(product.getSpecialPrice());
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
}
