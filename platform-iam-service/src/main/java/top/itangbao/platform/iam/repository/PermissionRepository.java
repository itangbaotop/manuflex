package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.iam.domain.Permission;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限名称查找权限
     * @param name 权限名称 (例如: user:read, product:create)
     * @return 包含权限的 Optional 对象
     */
    Optional<Permission> findByName(String name);

    /**
     * 根据权限名称判断权限是否存在
     * @param name 权限名称
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByName(String name);
}
