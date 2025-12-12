package top.itangbao.platform.common.enums;

public enum FieldType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    DATE,
    DATETIME,
    ENUM,
    TEXT, // 长文本
    FILE, // 文件上传
    SELECT, REFERENCE // 引用其他模式的实体
}
