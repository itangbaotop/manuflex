package top.itangbao.platform.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 模块名称 (例如：用户管理)
     */
    String module() default "";

    /**
     * 功能名称 (例如：新增用户)
     */
    String action() default "";
}