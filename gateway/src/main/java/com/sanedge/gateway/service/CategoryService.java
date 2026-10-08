package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CategoryDto;
import io.smallrye.mutiny.Uni;

public interface CategoryService {
    Uni<CategoryDto.ApiResponsePaginationCategory> listCategories(int page, int size, String search);
    Uni<CategoryDto.ApiResponseCategory> getCategory(String id);
    Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getActiveCategories(int page, int size, String search);
    Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getTrashedCategories(int page, int size, String search);
    Uni<CategoryDto.ApiResponseCategory> createCategory(CategoryDto.CreateCategoryRequest body);
    Uni<CategoryDto.ApiResponseCategory> updateCategory(String id, CategoryDto.UpdateCategoryRequest body);
    Uni<CategoryDto.ApiResponseCategoryDeleteAt> deleteCategory(String id);
    Uni<CategoryDto.ApiResponseCategoryDeleteAt> restoreCategory(String id);
    Uni<CategoryDto.ApiResponseCategoryDelete> deleteCategoryPermanent(String id);
    Uni<CategoryDto.ApiResponseCategoryAll> restoreAllCategory();
    Uni<CategoryDto.ApiResponseCategoryAll> deleteAllCategoryPermanent();

    // Statistics
    Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPrices(int year, int month);
    Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPrices(int year);
    Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesById(String categoryId, int year, int month);
    Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesById(String categoryId, int year);
    Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesByMerchant(String merchantId, int year, int month);
    Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesByMerchant(String merchantId, int year);
    Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPrices(int year);
    Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPrices(int year);
    Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesByMerchant(String merchantId, int year);
    Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesByMerchant(String merchantId, int year);
    Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesById(String categoryId, int year);
    Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesById(String categoryId, int year);
}
