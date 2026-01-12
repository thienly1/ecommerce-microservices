package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSkuCode());

        if (productRepository.existsBySkuCode(request.getSkuCode())) {
            throw new IllegalArgumentException("SKU code already exists: " + request.getSkuCode());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .skuCode(request.getSkuCode())
                .status(Product.ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with id: {}", savedProduct.getId());

        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");

        return productRepository.findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);

        return productRepository.findByCategory(category)
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());

        // Update status based on stock
        if (request.getStockQuantity() == 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == Product.ProductStatus.OUT_OF_STOCK) {
            product.setStatus(Product.ProductStatus.ACTIVE);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully");

        return ProductResponse.fromEntity(updatedProduct);
    }

    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted successfully");
    }

    // Method for Order Service to reduce stock
    public ProductResponse reduceStock(Long productId, int quantity) {
        log.info("Reducing stock for product {}, quantity: {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, product.getStockQuantity());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);

        if (product.getStockQuantity() == 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Stock reduced successfully. New quantity: {}", updatedProduct.getStockQuantity());

        return ProductResponse.fromEntity(updatedProduct);
    }

    // Method to check if product is in stock
    @Transactional(readOnly = true)
    public boolean isInStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return product.getStockQuantity() >= quantity;
    }
}
