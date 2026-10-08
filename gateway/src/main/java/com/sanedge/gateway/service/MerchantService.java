package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDto;
import io.smallrye.mutiny.Uni;

public interface MerchantService {
    Uni<MerchantDto.ApiResponsePaginationMerchant> listMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> activeMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponsePaginationMerchantDeleteAt> trashedMerchants(int page, int size, String search);
    Uni<MerchantDto.ApiResponseMerchant> getMerchant(String id);
    Uni<MerchantDto.ApiResponseMerchant> getMerchantByApiKey(String apiKey);
    Uni<MerchantDto.ApiResponsesMerchant> getMerchantsByUserId(String userId);
    Uni<MerchantDto.ApiResponseMerchant> createMerchant(MerchantDto.CreateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchant> updateMerchant(String id, MerchantDto.UpdateMerchantRequest body);
    Uni<MerchantDto.ApiResponseMerchantDeleteAt> deleteMerchant(String id);
    Uni<MerchantDto.ApiResponseMerchantDeleteAt> restoreMerchant(String id);
    Uni<MerchantDto.ApiResponseMerchantDelete> deleteMerchantPermanent(String id);
    Uni<MerchantDto.ApiResponseMerchantAll> restoreAllMerchants();
    Uni<MerchantDto.ApiResponseMerchantAll> deleteAllMerchantsPermanent();
}
