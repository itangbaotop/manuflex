package top.itangbao.platform.common.util;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import top.itangbao.platform.common.security.CustomUserDetails;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.privateKey:}")
    private String privateKeyStr;

    @Value("${app.jwt.publicKey}")
    private String publicKeyStr;

    @Value("${app.jwt.expirationMs}")
    private int jwtExpirationMs;

    @Value("${app.jwt.refreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 加载公钥 (所有服务通用)
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);
        this.publicKey = keyFactory.generatePublic(publicSpec);

        // 加载私钥 (仅 IAM 服务有值)
        if (privateKeyStr != null && !privateKeyStr.isEmpty()) {
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
            this.privateKey = keyFactory.generatePrivate(privateSpec);
        }
    }

    /**
     * IAM 使用：私钥签名
     */
    public String generateAccessTokenWithPermissions(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<String> permissions = userPrincipal.getAuthorities().stream()
                .filter(a -> !a.getAuthority().startsWith("ROLE_"))
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("roles", String.join(",", roles))
                .claim("permissions", permissions)
                .claim("tenantId", userPrincipal.getTenantId())
                .claim("deptId", userPrincipal.getDeptId())
                .claim("dataScopes", userPrincipal.getDataScopes())
                .claim("accessibleDeptIds", userPrincipal.getAccessibleDeptIds())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.RS256) // 核心：切换为 RS256
                .compact();
    }

    /**
     * 生成 JWT Refresh Token (使用私钥签名)
     */
    public String generateRefreshToken() {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 从 Token 中获取用户名 (使用公钥解析)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 验证 Token 有效性 (使用公钥验证)
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 验证失败: {}", e.getMessage());
        }
        return false;
    }
    

    /**
     * 网关/资源服务使用：公钥验证
     */
    public Claims validateAndParse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey) // 使用公钥验证
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}