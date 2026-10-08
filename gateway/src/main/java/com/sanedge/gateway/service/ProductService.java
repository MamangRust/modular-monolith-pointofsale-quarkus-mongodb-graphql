package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ProductDto;
import io.smallrye.mutiny.Uni;

public interface ProductService {
    Uni<ProductDto.UploadImageResponse> uploadImage(String base64Data, String fileName);
    Uni<ProductDto.ApiResponsePaginationProduct> findAll(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByActive(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProductDeleteAt> findByTrashed(int page, int size, String search);
    Uni<ProductDto.ApiResponseProduct> findById(String id);
    Uni<ProductDto.ApiResponsePaginationProduct> findByMerchant(String merchantId, String search, String categoryId, int minPrice, int maxPrice, int page, int size);
    Uni<ProductDto.ApiResponsePaginationProduct> findByCategory(String categoryName, int page, int size, String search, int minPrice, int maxPrice);
    Uni<ProductDto.ApiResponseProduct> createProduct(ProductDto.CreateProductRequest body);
    Uni<ProductDto.ApiResponseProduct> updateProduct(String id, ProductDto.UpdateProductRequest body);
    Uni<ProductDto.ApiResponseProductDeleteAt> deleteProduct(String id);
    Uni<ProductDto.ApiResponseProductDeleteAt> restoreProduct(String id);
    Uni<ProductDto.ApiResponseProductDelete> deleteProductPermanent(String id);
    Uni<ProductDto.ApiResponseProductAll> restoreAllProducts();
    Uni<ProductDto.ApiResponseProductAll> deleteAllProductsPermanent();
}
