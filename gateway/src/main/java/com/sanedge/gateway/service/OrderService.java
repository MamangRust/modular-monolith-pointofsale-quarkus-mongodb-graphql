package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.OrderDto;
import io.smallrye.mutiny.Uni;

public interface OrderService {
    Uni<OrderDto.ApiResponsePaginationOrder> listOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getActiveOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getTrashedOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponseOrder> getOrder(String id);
    Uni<OrderDto.ApiResponsePaginationOrder> getOrdersByMerchant(String merchantId, int page, int size, String search);
    Uni<OrderDto.ApiResponseOrder> createOrder(OrderDto.CreateOrderRequest body);
    Uni<OrderDto.ApiResponseOrder> updateOrder(String id, OrderDto.UpdateOrderRequest body);
    Uni<OrderDto.ApiResponseOrderDeleteAt> deleteOrder(String id);
    Uni<OrderDto.ApiResponseOrderDeleteAt> restoreOrder(String id);
    Uni<OrderDto.ApiResponseOrderDelete> deleteOrderPermanent(String id);
    Uni<OrderDto.ApiResponseOrderAll> restoreAllOrders();
    Uni<OrderDto.ApiResponseOrderAll> deleteAllOrders();

    // Statistics
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenues(int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenues(int year);
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenuesByMerchant(String merchantId, int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenuesByMerchant(String merchantId, int year);
    Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenues(int year);
    Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenues(int year);
    Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenuesByMerchant(String merchantId, int year);
    Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenuesByMerchant(String merchantId, int year);
}
