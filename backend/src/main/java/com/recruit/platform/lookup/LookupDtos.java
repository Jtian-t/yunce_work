package com.recruit.platform.lookup;

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
        String departmentName
) {
}
