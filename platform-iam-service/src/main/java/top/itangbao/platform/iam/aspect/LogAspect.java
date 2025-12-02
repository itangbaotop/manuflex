package top.itangbao.platform.iam.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.itangbao.platform.common.annotation.Log;
import top.itangbao.platform.iam.domain.OperationLog;
import top.itangbao.platform.iam.repository.OperationLogRepository;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private OperationLogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper; // 用于序列化参数

    // 配置切入点：只要方法上加了 @Log 注解
    @Pointcut("@annotation(top.itangbao.platform.common.annotation.Log)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = null;
        OperationLog operLog = new OperationLog();

        try {
            // 执行目标方法
            result = point.proceed();
            operLog.setStatus("SUCCESS");
        } catch (Throwable e) {
            operLog.setStatus("FAIL");
            operLog.setErrorMsg(e.getMessage());
            throw e; // 异常要抛出去，不能吞掉
        } finally {
            long time = System.currentTimeMillis() - beginTime;
            operLog.setExecutionTime(time);

            try {
                saveLog(point, operLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operLog) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation != null) {
            operLog.setModule(logAnnotation.module());
            operLog.setAction(logAnnotation.action());
        }

        // 获取请求参数
        Object[] args = joinPoint.getArgs();
        try {
            // 简单序列化第一个参数作为描述 (通常是 RequestBody DTO)
            // 生产环境可能需要更精细的参数过滤，避免敏感信息泄露
            if (args.length > 0) {
                String params = objectMapper.writeValueAsString(args[0]);
                // 截断过长的参数
                operLog.setDescription(params.length() > 1000 ? params.substring(0, 1000) + "..." : params);
            }
        } catch (Exception e) {
            operLog.setDescription("Args serialization failed");
        }

        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            operLog.setUsername(authentication.getName());
        } else {
            operLog.setUsername("Anonymous");
        }

        // 获取 IP
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            operLog.setUserIp(request.getRemoteAddr());
        } catch (Exception e) {
            operLog.setUserIp("Unknown");
        }

        // 保存到数据库
        logRepository.save(operLog);
    }
}