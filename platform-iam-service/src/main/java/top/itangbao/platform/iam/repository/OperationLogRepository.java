package top.itangbao.platform.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import top.itangbao.platform.iam.domain.OperationLog;

// 加上 JpaSpecificationExecutor 以支持复杂查询（后续做日志搜索用）
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {
}