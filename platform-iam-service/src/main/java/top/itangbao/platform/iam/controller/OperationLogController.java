package top.itangbao.platform.iam.controller;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.iam.domain.OperationLog;
import top.itangbao.platform.iam.repository.OperationLogRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/iam/logs")
public class OperationLogController {

    @Autowired
    private OperationLogRepository logRepository;

    /**
     * 分页查询审计日志
     * 支持过滤条件：username, module, status
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 只有管理员能看日志
    public ResponseEntity<Page<OperationLog>> getLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (module != null && !module.isEmpty()) {
                predicates.add(cb.like(root.get("module"), "%" + module + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ResponseEntity.ok(logRepository.findAll(spec, pageable));
    }
}