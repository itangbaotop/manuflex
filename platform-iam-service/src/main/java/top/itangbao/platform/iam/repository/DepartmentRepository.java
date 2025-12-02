package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.itangbao.platform.iam.domain.Department;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // 根据租户ID查询所有部门，并按排序号排序
    List<Department> findAllByTenantIdOrderBySortOrderAsc(String tenantId);

    // 检查是否存在子部门
    boolean existsByParentId(Long parentId);
}