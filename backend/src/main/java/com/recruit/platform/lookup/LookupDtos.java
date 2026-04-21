package com.recruit.platform.lookup;

import java.util.List;

record DepartmentLookupResponse(
        Long id,
        String code,
        String name
) {
}

record UserLookupResponse(
        Long id,
        String username,
        String displayName,
        String email,
        Long departmentId,
        String departmentName,
        boolean canInterview,
        String employmentStatus,
        Integer displayOrder,
        List<String> roles
 ) {
}

record DepartmentMemberResponse(
        Long id,
        String username,
        String displayName,
        String email,
        Long departmentId,
        String departmentName,
        boolean canInterview,
        String employmentStatus,
        Integer displayOrder,
        List<String> roles
) {
}
