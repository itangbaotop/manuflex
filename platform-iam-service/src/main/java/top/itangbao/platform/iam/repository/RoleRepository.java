package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.iam.domain.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色名称查找角色
     * @param name 角色名称 (例如: ADMIN, USER)
     * @return 包含角色的 Optional 对象
     */
    Optional<Role> findByName(String name);

    /**
     * 根据角色名称判断角色是否存在
     * @param name 角色名称
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByName(String name);
}
