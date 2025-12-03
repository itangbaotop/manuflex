package top.itangbao.platform.iam.service;

import top.itangbao.platform.iam.domain.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> getDepartmentTree(String tenantId);
    Department createDepartment(Department department);
    Department updateDepartment(Long id, Department department);
    void deleteDepartment(Long id);

    List<Long> getChildDepartmentIds(Long parentId, String tenantId);
}