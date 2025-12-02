package top.itangbao.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScope {
    ALL("全部数据"),
    DEPT_AND_CHILD("本部门及以下数据"),
    DEPT("本部门数据"),
    SELF("仅本人数据"),
    CUSTOM("自定义数据");

    private final String description;
}