package top.itangbao.platform.iam.service.impl;

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

import java.util.HashSet;
import java.util.Set;

/**
 * 实现 Spring Security 的 UserDetailsService 接口，用于从数据库加载用户详情
 */
@Slf4j
@Service // 标记为 Spring 组件
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired // 自动注入 UserRepository
    private UserRepository userRepository;

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
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
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


        return new CustomUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getDeptId(),
                dataScopes,
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                authorities
        );
    }
}
