-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS manuflex_paas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权 (如果不存在)
CREATE USER IF NOT EXISTS 'manuflex_user'@'%' IDENTIFIED BY 'manuflex_password';
GRANT ALL PRIVILEGES ON manuflex_paas.* TO 'manuflex_user'@'%';
FLUSH PRIVILEGES;

-- 切换到新创建的数据库
USE manuflex_paas;

-- 示例：创建一张用户表 (IAM 服务会用到)
CREATE TABLE IF NOT EXISTS `mf_users` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) UNIQUE,
    `tenant_id` VARCHAR(50) NOT NULL, -- 用于多租户
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 示例：创建一张角色表
CREATE TABLE IF NOT EXISTS `mf_roles` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 示例：创建用户-角色关联表
CREATE TABLE IF NOT EXISTS `mf_user_roles` (
                                               `user_id` BIGINT NOT NULL,
                                               `role_id` BIGINT NOT NULL,
                                               PRIMARY KEY (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `mf_users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `mf_roles`(`id`) ON DELETE CASCADE
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 插入一些初始数据 (可选)
INSERT IGNORE INTO `mf_roles` (`id`, `name`, `description`) VALUES
(1, 'ADMIN', 'Administrator Role'),
(2, 'TENANT_ADMIN', 'Tenant Administrator Role'),
(3, 'USER', 'Standard User Role');
