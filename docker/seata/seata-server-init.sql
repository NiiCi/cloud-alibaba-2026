-- 创建 seata 库
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS `global_table` (
                                              `xid` VARCHAR(128) NOT NULL,
    `transaction_id` BIGINT NULL DEFAULT NULL,
    `status` TINYINT NOT NULL,
    `application_id` VARCHAR(32) NULL DEFAULT NULL,
    `transaction_service_group` VARCHAR(32) NULL DEFAULT NULL,
    `transaction_name` VARCHAR(128) NULL DEFAULT NULL,
    `timeout` INT NULL DEFAULT NULL,
    `begin_time` BIGINT NULL DEFAULT NULL,
    `application_data` VARCHAR(2000) NULL DEFAULT NULL,
    `gmt_create` DATETIME NULL DEFAULT NULL,
    `gmt_modified` DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (`xid`),
    INDEX `idx_status_gmt_modified` (`status`, `gmt_modified`),
    INDEX `idx_transaction_id` (`transaction_id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table` (
                                              `branch_id` BIGINT NOT NULL,
                                              `xid` VARCHAR(128) NOT NULL,
    `transaction_id` BIGINT NULL DEFAULT NULL,
    `resource_group_id` VARCHAR(32) NULL DEFAULT NULL,
    `resource_id` VARCHAR(256) NULL DEFAULT NULL,
    `branch_type` VARCHAR(8) NULL DEFAULT NULL,
    `status` TINYINT NULL DEFAULT NULL,
    `client_id` VARCHAR(64) NULL DEFAULT NULL,
    `application_data` VARCHAR(2000) NULL DEFAULT NULL,
    `gmt_create` DATETIME(6) NULL DEFAULT NULL,
    `gmt_modified` DATETIME(6) NULL DEFAULT NULL,
    PRIMARY KEY (`branch_id`),
    INDEX `idx_xid` (`xid`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 锁表
CREATE TABLE IF NOT EXISTS `lock_table` (
                                            `row_key` VARCHAR(128) NOT NULL,
    `xid` VARCHAR(128) NULL DEFAULT NULL,
    `transaction_id` BIGINT NULL DEFAULT NULL,
    `branch_id` BIGINT NOT NULL,
    `resource_id` VARCHAR(256) NULL DEFAULT NULL,
    `table_name` VARCHAR(32) NULL DEFAULT NULL,
    `pk` VARCHAR(36) NULL DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0:locked,1:rollbacking',
    `gmt_create` DATETIME NULL DEFAULT NULL,
    `gmt_modified` DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (`row_key`),
    INDEX `idx_status` (`status`),
    INDEX `idx_branch_id` (`branch_id`),
    INDEX `idx_xid` (`xid`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;