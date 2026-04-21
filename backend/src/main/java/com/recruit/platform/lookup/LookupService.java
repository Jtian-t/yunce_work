package com.recruit.platform.lookup;

import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.enums.EmploymentStatus;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
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
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return departmentRepository.findAll().stream()
                    .sorted(Comparator.comparing(department -> department.getName().toLowerCase()))
                    .map(department -> new DepartmentLookupResponse(
                            department.getId(),
                            department.getCode(),
                            department.getName()
                    ))
                    .toList();
        }

        currentUserService.requireAnyRole(RoleType.DEPARTMENT_LEAD);
        Department department = currentUserService.getRequiredUser().getDepartment();
        if (department == null) {
            return List.of();
        }
        return List.of(new DepartmentLookupResponse(department.getId(), department.getCode(), department.getName()));
    }

    public List<UserLookupResponse> users(RoleType role, Long departmentId) {
        User actor = currentUserService.getRequiredUser();
        Long scopedDepartmentId = enforceDepartmentScope(actor, departmentId);
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(user -> user.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .filter(user -> user.getRoles().contains(role))
                .filter(user -> scopedDepartmentId == null || (user.getDepartment() != null && user.getDepartment().getId().equals(scopedDepartmentId)))
                .filter(user -> role != RoleType.INTERVIEWER || user.isCanInterview())
                .sorted(Comparator
                        .comparing(User::getDisplayOrder)
                        .thenComparing(user -> user.getDisplayName().toLowerCase()))
                .map(this::toUserLookup)
                .toList();
    }

    public List<DepartmentMemberResponse> departmentMembers(Long departmentId) {
        User actor = currentUserService.getRequiredUser();
        Long scopedDepartmentId = enforceDepartmentScope(actor, departmentId);
        if (scopedDepartmentId == null) {
            throw new ForbiddenException("Department scope is required");
        }

        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(user -> user.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .filter(user -> user.getDepartment() != null && user.getDepartment().getId().equals(scopedDepartmentId))
                .sorted(Comparator
                        .comparing(User::getDisplayOrder)
                        .thenComparing(user -> user.getDisplayName().toLowerCase()))
                .map(user -> new DepartmentMemberResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getDepartment().getId(),
                        user.getDepartment().getName(),
                        user.isCanInterview(),
                        user.getEmploymentStatus().name(),
                        user.getDisplayOrder(),
                        user.getRoles().stream().map(Enum::name).sorted().toList()
                ))
                .toList();
    }

    private Long enforceDepartmentScope(User actor, Long requestedDepartmentId) {
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return requestedDepartmentId;
        }

        currentUserService.requireAnyRole(RoleType.DEPARTMENT_LEAD);
        Department actorDepartment = actor.getDepartment();
        if (actorDepartment == null) {
            throw new ForbiddenException("Current user is not bound to a department");
        }
        if (requestedDepartmentId != null && !actorDepartment.getId().equals(requestedDepartmentId)) {
            throw new ForbiddenException("Cannot query another department");
        }
        return actorDepartment.getId();
    }

    private UserLookupResponse toUserLookup(User user) {
        return new UserLookupResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.isCanInterview(),
                user.getEmploymentStatus().name(),
                user.getDisplayOrder(),
                user.getRoles().stream().map(Enum::name).sorted().toList()
        );
    }
}
