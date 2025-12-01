package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.iam.domain.Menu;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    // 根据排序号获取所有菜单
    List<Menu> findAllByOrderBySortOrderAsc();
}