package top.itangbao.platform.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.iam.domain.Department;
import top.itangbao.platform.iam.repository.DepartmentRepository;
import top.itangbao.platform.iam.service.DepartmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public List<Department> getDepartmentTree(String tenantId) {
        // 1. 获取该租户下所有部门
        List<Department> allDepts = departmentRepository.findAllByTenantIdOrderBySortOrderAsc(tenantId);

        // 2. 构建树形结构
        return buildTree(allDepts);
    }

    private List<Department> buildTree(List<Department> depts) {
        Map<Long, Department> deptMap = new HashMap<>();
        List<Department> roots = new ArrayList<>();

        for (Department d : depts) {
            d.setChildren(new ArrayList<>());
            deptMap.put(d.getId(), d);
        }

        for (Department d : depts) {
            if (d.getParentId() == null || d.getParentId() == 0) {
                roots.add(d);
            } else {
                Department parent = deptMap.get(d.getParentId());
                if (parent != null) {
                    parent.getChildren().add(d);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public Department createDepartment(Department dept) {
        if (dept.getParentId() == null) dept.setParentId(0L);
        return departmentRepository.save(dept);
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, Department details) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        dept.setName(details.getName());
        dept.setParentId(details.getParentId());
        dept.setSortOrder(details.getSortOrder());
        dept.setLeader(details.getLeader());
        dept.setPhone(details.getPhone());
        dept.setEmail(details.getEmail());
        dept.setStatus(details.getStatus());

        return departmentRepository.save(dept);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (departmentRepository.existsByParentId(id)) {
            throw new IllegalStateException("Cannot delete department with children. Please delete sub-departments first.");
        }
        departmentRepository.deleteById(id);
    }

    @Override
    public List<Long> getChildDepartmentIds(Long parentId, String tenantId) {
        // 1. 获取该租户下所有部门 (数据量通常不大，一次查出比递归查库性能更好)
        List<Department> allDepts = departmentRepository.findAllByTenantIdOrderBySortOrderAsc(tenantId);

        // 2. 递归收集所有子孙ID
        List<Long> resultIds = new ArrayList<>();
        // 先把自己加进去 (因为"部门及以下"包含本部门)
        resultIds.add(parentId);

        collectChildIds(parentId, allDepts, resultIds);

        return resultIds;
    }

    // 递归辅助方法
    private void collectChildIds(Long parentId, List<Department> allDepts, List<Long> resultIds) {
        for (Department dept : allDepts) {
            // 如果当前部门的父亲是 parentId
            if (dept.getParentId() != null && dept.getParentId().equals(parentId)) {
                resultIds.add(dept.getId());
                // 继续递归找它的孩子
                collectChildIds(dept.getId(), allDepts, resultIds);
            }
        }
    }
}