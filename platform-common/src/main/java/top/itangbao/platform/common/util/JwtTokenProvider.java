package top.itangbao.platform.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID; // 引入 UUID
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs}") // 新增 Refresh Token 过期时间
    private int jwtRefreshExpirationMs;

    public Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * 生成 JWT Access Token
     * @param authentication 认证信息
     * @return JWT Access Token 字符串
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 生成 JWT Access Token，包含权限信息 (新方法)
     * @param authentication 认证信息
     * @return JWT Access Token 字符串
     */
    public String generateAccessTokenWithPermissions(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // 提取角色
        List<String> roles = userPrincipal.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_")) // 过滤出角色
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 提取权限 (所有 GrantedAuthority，去除 ROLE_ 前缀)
        List<String> permissions = userPrincipal.getAuthorities().stream()
                .filter(a -> !a.getAuthority().startsWith("ROLE_")) // 过滤出权限
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("roles", String.join(",", roles)) // 角色仍然以逗号分隔字符串存储
                .claim("permissions", permissions) // ⬅️ 权限以 List<String> 存储
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }


    /**
     * 生成 JWT Refresh Token
     * Refresh Token 不包含业务数据，通常是一个随机字符串，用于验证用户是否可以获取新的 Access Token
     * @return JWT Refresh Token 字符串
     */
    public String generateRefreshToken() {
        // Refresh Token 可以是随机的 UUID，然后存储在数据库中进行校验
        // 也可以是一个带有过期时间的 JWT，但通常不包含敏感信息
        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // 使用 UUID 作为 Refresh Token 的 ID
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从 JWT Token 中获取用户名 (适用于 Access Token)
     * @param token JWT Token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * 验证 JWT Token (适用于 Access Token 和 Refresh Token)
     * @param authToken JWT Token 字符串
     * @return 如果 Token 有效返回 true，否则返回 false
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 JWT Token 中获取所有 Claims
     * @param token JWT Token 字符串
     * @return Claims 对象
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }
}
