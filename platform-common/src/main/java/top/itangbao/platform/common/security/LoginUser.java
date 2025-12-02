package top.itangbao.platform.common.security;

import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class LoginUser {
    private String username;
    private String tenantId;
    private Long deptId;
    private List<String> dataScopes;
}