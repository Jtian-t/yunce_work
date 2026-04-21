package com.recruit.platform.lookup;

import com.recruit.platform.common.enums.RoleType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @GetMapping("/departments")
    List<DepartmentLookupResponse> departments() {
        return lookupService.departments();
    }

    @GetMapping("/users")
    List<UserLookupResponse> users(@RequestParam RoleType role, @RequestParam(required = false) Long departmentId) {
        return lookupService.users(role, departmentId);
    }

    @GetMapping("/department-members")
    List<DepartmentMemberResponse> departmentMembers(@RequestParam Long departmentId) {
        return lookupService.departmentMembers(departmentId);
    }
}
