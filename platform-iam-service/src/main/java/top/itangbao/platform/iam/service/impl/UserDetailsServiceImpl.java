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

import java.util.Collections; // 暂时用空的权限集合
import java.util.List;
import java.util.stream.Collectors;

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

        // 构建用户的权限列表 (包括角色和直接权限)
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())) // 角色前缀通常是 ROLE_
                .collect(Collectors.toList());

        user.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .forEach(authorities::add);

        // 同时添加角色对应的权限
        user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .forEach(authorities::add);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities // 现在包含了用户的角色和权限
        );
    }
}
