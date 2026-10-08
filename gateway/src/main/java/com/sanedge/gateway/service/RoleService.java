package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.RoleDto;
import io.smallrye.mutiny.Uni;

public interface RoleService {
    Uni<RoleDto.ApiResponsePaginationRole> listRoles(int page, int size, String search);
    Uni<RoleDto.ApiResponseRole> getRole(String id);
    Uni<RoleDto.ApiResponseRole> getRoleByName(String name);
    Uni<RoleDto.ApiResponsesRole> getRolesByUserId(String userId);
    Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> activeRoles(int page, int size, String search);
    Uni<RoleDto.ApiResponsePaginationRoleDeleteAt> trashedRoles(int page, int size, String search);
    Uni<RoleDto.ApiResponseRole> createRole(RoleDto.CreateRoleRequest body);
    Uni<RoleDto.ApiResponseRole> updateRole(String id, RoleDto.UpdateRoleRequest body);
    Uni<RoleDto.ApiResponseRoleDeleteAt> deleteRole(String id);
    Uni<RoleDto.ApiResponseRoleDeleteAt> restoreRole(String id);
    Uni<RoleDto.ApiResponseRoleDelete> deleteRolePermanent(String id);
    Uni<RoleDto.ApiResponseRoleAll> restoreAllRoles();
    Uni<RoleDto.ApiResponseRoleAll> deleteAllRolesPermanent();
    Uni<RoleDto.ApiResponseUserRole> assignRoleToUser(RoleDto.AssignRoleToUserRequest body);
    Uni<Boolean> removeRoleFromUser(RoleDto.AssignRoleToUserRequest body);
}
