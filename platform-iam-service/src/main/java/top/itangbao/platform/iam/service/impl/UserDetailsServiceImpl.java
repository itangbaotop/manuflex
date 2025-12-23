package top.itangbao.platform.iam.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.itangbao.platform.iam.domain.User;
import top.itangbao.platform.iam.repository.UserRepository;
import top.itangbao.platform.common.security.CustomUserDetails;
import top.itangbao.platform.iam.service.DepartmentService;

import java.util.HashSet;
import java.util.Set;

/**
 * 实现 Spring Security 的 UserDetailsService 接口，用于从数据库加载用户详情
 */
@Slf4j
@Service // 标记为 Spring 组件
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource // 自动注入 UserRepository
    private UserRepository userRepository;

    @Resource
    private DepartmentService departmentService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier)));

        Set<GrantedAuthority> authorities = new HashSet<>();
        Set<String> dataScopes = new HashSet<>();

        // 添加角色权限
        user.getRoles().forEach(role -> {
            String roleName = role.getName();
            authorities.add(new SimpleGrantedAuthority(roleName));
            role.getPermissions().stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                    .forEach(authorities::add);
            dataScopes.add(role.getDataScope());
        });

        // 添加直接分配给用户的权限
        user.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .forEach(authorities::add);

        Set<Long> accessibleDeptIds = new HashSet<>();
        // 如果权限是“本部门及以下”，立即调用 DepartmentService 计算
        if (user.getRoles().stream().anyMatch(r -> "DEPT_AND_CHILD".equals(r.getDataScope()))) {
            // 调用我们已经实现的递归方法
            accessibleDeptIds.addAll(departmentService.getChildDepartmentIds(user.getDeptId(), user.getTenantId()));
        } else {
            accessibleDeptIds.add(user.getDeptId());
        }

        return new CustomUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getDeptId(),
                dataScopes,
                accessibleDeptIds,
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                authorities
        );
    }
}
