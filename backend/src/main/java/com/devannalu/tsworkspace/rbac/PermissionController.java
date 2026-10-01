package com.devannalu.tsworkspace.rbac;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionController {
    private final PermissionRepository permissions;
    public PermissionController(PermissionRepository permissions) { this.permissions = permissions; }

    @GetMapping("/api/v1/permissions")
    @PreAuthorize("@permissionGuard.has(authentication, 'permissions.view')")
    public List<PermissionResponse> list() {
        return permissions.findAllByOrderByKeyAsc().stream().map(p -> new PermissionResponse(p.getKey(), p.getName())).toList();
    }

    public record PermissionResponse(String key, String name) { }
}
