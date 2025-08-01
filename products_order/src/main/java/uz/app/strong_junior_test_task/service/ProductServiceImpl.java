package uz.app.strong_junior_test_task.service;

import uz.app.strong_junior_test_task.dto.ProductCreateRequest;
import uz.app.strong_junior_test_task.dto.ProductResponse;
import uz.app.strong_junior_test_task.dto.ProductUpdateRequest;
import uz.app.strong_junior_test_task.entity.Product;
import uz.app.strong_junior_test_task.exception.ProductNotFoundException;
import uz.app.strong_junior_test_task.repository.ProductRepository;
import uz.app.strong_junior_test_task.util.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> getAll() {
        List<Product> activeProducts = productRepository.findAllByIsActiveTrue();

        return activeProducts.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .isActive(request.getIsActive())
                .createdAt(LocalDateTime.now())
                .build();

        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setIsActive(request.getIsActive());

        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductResponse> searchProducts(String name, String category) {
        return productRepository.findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(name, category)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

}
