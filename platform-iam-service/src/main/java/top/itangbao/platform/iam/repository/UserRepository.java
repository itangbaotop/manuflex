package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.iam.domain.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 包含用户的 Optional 对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 包含用户的 Optional 对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名判断用户是否存在
     * @param username 用户名
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByUsername(String username);

    /**
     * 根据邮箱判断用户是否存在
     * @param email 邮箱
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByEmail(String email);
}
