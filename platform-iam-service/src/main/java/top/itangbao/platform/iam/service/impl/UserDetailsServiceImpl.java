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
import top.itangbao.platform.iam.security.CustomUserDetails;

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

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        user.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .forEach(authorities::add);

        user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .forEach(authorities::add);

        return new CustomUserDetails( // 返回 CustomUserDetails
                user.getId(), // 传入用户ID
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
