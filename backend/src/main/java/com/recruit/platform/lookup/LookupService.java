package com.recruit.platform.lookup;

import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public List<DepartmentLookupResponse> departments() {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(department -> department.getName().toLowerCase()))
                .map(department -> new DepartmentLookupResponse(
                        department.getId(),
                        department.getCode(),
                        department.getName()
                ))
                .toList();
    }

    public List<UserLookupResponse> users(RoleType role) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(role))
                .sorted(Comparator.comparing(user -> user.getDisplayName().toLowerCase()))
                .map(user -> new UserLookupResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getDepartment() == null ? null : user.getDepartment().getId(),
                        user.getDepartment() == null ? null : user.getDepartment().getName()
                ))
                .toList();
    }
}
