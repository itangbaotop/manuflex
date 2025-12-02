package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.itangbao.platform.iam.domain.Permission;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // 根据唯一标识查找
    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);
}