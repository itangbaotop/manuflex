package top.itangbao.platform.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Set;

/**
 * 自定义 UserDetails 实现，用于存储用户的 ID
 */
public class CustomUserDetails extends User {
    private Long id;
    private String tenantId;
    private Long deptId;
    private Set<String> dataScopes;

    public CustomUserDetails(Long id, String tenantId, Long deptId, Set<String> dataScopes,
                             String username, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.id = id;
        this.tenantId = tenantId;
        this.deptId = deptId;
        this.dataScopes = dataScopes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Set<String> getDataScopes() {
        return dataScopes;
    }

    public void setDataScopes(Set<String> dataScopes) {
        this.dataScopes = dataScopes;
    }
}
