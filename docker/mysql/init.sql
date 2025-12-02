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

--  新增：插入一些权限数据
INSERT IGNORE INTO `mf_permissions` (`id`, `name`, `description`) VALUES
                                                                      (1, 'user:read', 'Read user information'),
                                                                      (2, 'user:write', 'Create or update user information'),
                                                                      (3, 'user:delete', 'Delete user information'),
                                                                      (4, 'lims:sample:create', 'Create LIMS sample'),
                                                                      (5, 'lims:sample:read', 'Read LIMS sample'),
                                                                      (6, 'lims:sample:update', 'Update LIMS sample'),
                                                                      (7, 'lims:sample:delete', 'Delete LIMS sample'),
                                                                      (8, 'metadata:schema:create', 'Create metadata schema'),
                                                                      (9, 'metadata:schema:read', 'Read metadata schema'),
                                                                      (10, 'metadata:schema:update', 'Update metadata schema'),
                                                                      (11, 'metadata:schema:delete', 'Delete metadata schema'),
                                                                      (12, 'workflow:process:deploy', 'Deploy workflow process'),
                                                                      (13, 'workflow:process:start', 'Start workflow process instance'),
                                                                      (14, 'workflow:task:read', 'Read workflow task'),
                                                                      (15, 'workflow:task:claim', 'Claim workflow task'),
                                                                      (16, 'workflow:task:complete', 'Complete workflow task'),
                                                                      (17, 'workflow:decision:deploy', 'Deploy DMN decision'),
                                                                      (18, 'workflow:decision:evaluate', 'Evaluate DMN decision');

--  新增：为 ADMIN 角色分配所有权限
INSERT IGNORE INTO `mf_role_permissions` (`role_id`, `permission_id`) VALUES
                                                                          (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18);

--  新增：为 TENANT_ADMIN 角色分配一些权限
INSERT IGNORE INTO `mf_role_permissions` (`role_id`, `permission_id`) VALUES
                                                                          (2, 1), (2, 2), (2, 4), (2, 5), (2, 6), (2, 8), (2, 9), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16), (2, 17), (2, 18);

--  新增：为 USER 角色分配一些权限
INSERT IGNORE INTO `mf_role_permissions` (`role_id`, `permission_id`) VALUES
                                                                          (3, 1), (3, 5), (3, 14), (3, 15), (3, 16), (3, 18);

-- 1. 系统管理 (目录)
INSERT INTO iam_menus (parent_id, name, path, icon, permission, sort_order, type, created_at, updated_at)
VALUES (0, '系统管理', 'system', 'SettingOutlined', 'ROLE_ADMIN', 1, 0, NOW(), NOW());

-- 获取刚才插入的ID，假设是 1
-- 1.1 用户管理
INSERT INTO iam_menus (parent_id, name, path, icon, permission, sort_order, type, created_at, updated_at)
VALUES (1, '用户管理', '/system/users', 'UserOutlined', 'user:read', 1, 1, NOW(), NOW());

-- 1.2 角色权限
INSERT INTO iam_menus (parent_id, name, path, icon, permission, sort_order, type, created_at, updated_at)
VALUES (1, '角色权限', '/system/roles', 'SafetyCertificateOutlined', 'role:read', 2, 1, NOW(), NOW());

-- 1.3 菜单管理 (新功能)
INSERT INTO iam_menus (parent_id, name, path, icon, permission, sort_order, type, created_at, updated_at)
VALUES (1, '菜单管理', '/system/menus', 'MenuOutlined', 'role:read', 3, 1, NOW(), NOW());

-- 1.4 模型设计
INSERT INTO iam_menus (parent_id, name, path, icon, permission, sort_order, type, created_at, updated_at)
VALUES (1, '模型设计', '/system/metadata', 'DatabaseOutlined', 'schema:read', 4, 1, NOW(), NOW());


-- ==========================================
-- 修正版：初始化系统权限列表
-- 表名：mf_permissions
-- 字段：name, description (无时间字段)
-- ==========================================

-- === 用户管理 ===
INSERT INTO mf_permissions (code, name, description) VALUES
                                                         ('user:read', '查看用户', '允许查看用户列表'),
                                                         ('user:create', '新建用户', '允许创建新用户'),
                                                         ('user:write', '编辑用户', '允许编辑用户信息'),
                                                         ('user:delete', '删除用户', '允许删除用户');

-- === 角色管理 ===
INSERT INTO mf_permissions (code, name, description) VALUES
                                                         ('role:read', '查看角色', '允许查看角色列表'),
                                                         ('role:write', '编辑角色', '允许创建或编辑角色'),
                                                         ('role:delete', '删除角色', '允许删除角色'),
                                                         ('role:assign_permission', '分配权限', '允许给角色分配权限');

-- === 菜单管理 ===
INSERT INTO mf_permissions (code, name, description) VALUES
                                                         ('menu:read', '查看菜单', '允许查看菜单配置'),
                                                         ('menu:write', '管理菜单', '允许增删改菜单结构');

-- === 业务模型 (Metadata) ===
INSERT INTO mf_permissions (code, name, description) VALUES
                                                         ('schema:read', '查看模型', '允许查看业务模型定义'),
                                                         ('schema:write', '设计模型', '允许新建或修改业务模型');

-- === 业务数据 (Data) ===
INSERT INTO mf_permissions (code, name, description) VALUES
                                                         ('data:read_all', '查看数据', '允许查看所有业务数据'),
                                                         ('data:create', '新增数据', '允许新增业务数据'),
                                                         ('data:update', '修改数据', '允许修改业务数据'),
                                                         ('data:delete', '删除数据', '允许删除业务数据'),
                                                         ('data:import', '导入数据', '允许导入Excel/CSV数据'),
                                                         ('data:export', '导出数据', '允许导出数据');

-- 创建 Camunda 数据库
CREATE DATABASE IF NOT EXISTS camunda_bpm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON camunda_bpm.* TO 'manuflex_user'@'%';
FLUSH PRIVILEGES;