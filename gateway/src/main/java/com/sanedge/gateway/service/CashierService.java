package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CashierDto;
import io.smallrye.mutiny.Uni;

public interface CashierService {
    Uni<CashierDto.ApiResponsePaginationCashier> listCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getActiveCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getTrashedCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponseCashier> getCashier(String id);
    Uni<CashierDto.ApiResponsePaginationCashier> getCashiersByMerchant(String merchantId, int page, int size, String search);
    Uni<CashierDto.ApiResponseCashier> createCashier(CashierDto.CreateCashierRequest body);
    Uni<CashierDto.ApiResponseCashier> updateCashier(String id, CashierDto.UpdateCashierRequest body);
    Uni<CashierDto.ApiResponseCashierDeleteAt> deleteCashier(String id);
    Uni<CashierDto.ApiResponseCashierDeleteAt> restoreCashier(String id);
    Uni<CashierDto.ApiResponseCashierDelete> deleteCashierPermanent(String id);
    Uni<CashierDto.ApiResponseCashierAll> restoreAllCashier();
    Uni<CashierDto.ApiResponseCashierAll> deleteAllCashierPermanent();

    // Statistics
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSales(int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSales(int year);
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesById(String cashierId, int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesById(String cashierId, int year);
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesByMerchant(String merchantId, int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesByMerchant(String merchantId, int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSales(int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSales(int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesByMerchant(String merchantId, int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesByMerchant(String merchantId, int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesById(String cashierId, int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesById(String cashierId, int year);
}
