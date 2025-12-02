package top.itangbao.platform.iam.init;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.iam.domain.Permission;
import top.itangbao.platform.iam.repository.PermissionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 系统权限初始化器
 * 每次服务启动时运行，确保数据库中包含代码定义的所有权限
 */
@Component
@Slf4j
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionInitializer(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * 这里定义系统所有的权限清单
     * 开发新功能时，只需在这里追加即可
     */
    @Getter
    @AllArgsConstructor
    public enum SysPermission {
        // === 用户管理 ===
        USER_READ("user:read", "查看用户"),
        USER_CREATE("user:create", "新建用户"),
        USER_WRITE("user:write", "编辑用户"),
        USER_DELETE("user:delete", "删除用户"),

        // === 角色管理 ===
        ROLE_READ("role:read", "查看角色"),
        ROLE_WRITE("role:write", "编辑角色"),
        ROLE_DELETE("role:delete", "删除角色"),
        ROLE_ASSIGN("role:assign_permission", "分配权限"),

        // === 菜单管理 ===
        MENU_READ("menu:read", "查看菜单"),
        MENU_WRITE("menu:write", "管理菜单"),

        // === 业务模型 ===
        SCHEMA_READ("schema:read", "查看模型"),
        SCHEMA_WRITE("schema:write", "设计模型"),

        // === 业务数据 ===
        DATA_READ("data:read_all", "查看数据"),
        DATA_CREATE("data:create", "新增数据"),
        DATA_UPDATE("data:update", "修改数据"),
        DATA_DELETE("data:delete", "删除数据"),
        DATA_IMPORT("data:import", "导入数据"),
        DATA_EXPORT("data:export", "导出数据");

        private final String code;
        private final String name;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("开始检查并同步系统权限...");

        for (SysPermission sysPerm : SysPermission.values()) {
            initPermission(sysPerm.getCode(), sysPerm.getName());
        }

        log.info("系统权限同步完成。");
    }

    private void initPermission(String code, String name) {
        // 检查数据库是否存在该权限 (根据 code)
        // 注意：这里假设您的 Repository 有 findByCode 或类似方法
        // 如果没有，可以使用 findByName(code) (假设您的 Permission 实体里字段叫 name 还是 code)
        // 根据之前的沟通，您的实体是: code (唯一标识), name (显示名称)

        Optional<Permission> exist = permissionRepository.findByCode(code);

        if (exist.isEmpty()) {
            Permission permission = new Permission();
            permission.setCode(code);
            permission.setName(name);
            permission.setDescription("系统自动初始化");
            permissionRepository.save(permission);
            log.info("初始化新增权限: {} - {}", code, name);
        } else {
            // 可选：如果想强制更新名称，可以在这里 update
            // Permission p = exist.get();
            // if (!p.getName().equals(name)) {
            //     p.setName(name);
            //     permissionRepository.save(p);
            // }
        }
    }
}