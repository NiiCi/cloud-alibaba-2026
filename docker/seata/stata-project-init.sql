CREATE DATABASE IF NOT EXISTS `storage_db`;
USE `storage_db`;
DROP TABLE IF EXISTS `storage_tbl`;
CREATE TABLE `storage_tbl`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `commodity_code` varchar(255) DEFAULT NULL,
    `count`          int(11) DEFAULT 0,
    `frozen_count`   int(11) DEFAULT 0 COMMENT 'TCC Try阶段冻结的库存数量',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`commodity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO storage_tbl (commodity_code, count) VALUES
('P0001', 100),
('B1234', 10);

-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20) NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11) NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- tcc_fence_log（2.x 新增）
CREATE TABLE IF NOT EXISTS `tcc_fence_log` (
    `xid`           VARCHAR(128)  NOT NULL COMMENT '全局事务 xid',
    `branch_id`     BIGINT(20)    NOT NULL COMMENT '分支事务 id',
    `action_name`   VARCHAR(64)   NOT NULL COMMENT 'TCC 资源名称',
    `status`        TINYINT(4)    NOT NULL COMMENT '状态: 0-try 1-confirm 2-cancel',
    `gmt_create`    DATETIME(3)   NOT NULL COMMENT '创建时间',
    `gmt_modified`  DATETIME(3)   NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`xid`, `branch_id`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_tx_action_name` (`action_name`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TCC 栅栏日志表';

CREATE DATABASE IF NOT EXISTS `order_db`;
USE `order_db`;
DROP TABLE IF EXISTS `order_tbl`;
CREATE TABLE `order_tbl`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `user_id`        varchar(255) DEFAULT NULL,
    `commodity_code` varchar(255) DEFAULT NULL,
    `count`          int(11) DEFAULT 0,
    `money`          int(11) DEFAULT 0,
    `status`         int(11) DEFAULT 0 COMMENT '订单状态: 0-TCC Try预创建 1-Confirm已确认 2-Cancel已取消',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20) NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11) NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- tcc_fence_log（2.x 新增）
CREATE TABLE IF NOT EXISTS `tcc_fence_log` (
    `xid`           VARCHAR(128)  NOT NULL COMMENT '全局事务 xid',
    `branch_id`     BIGINT(20)    NOT NULL COMMENT '分支事务 id',
    `action_name`   VARCHAR(64)   NOT NULL COMMENT 'TCC 资源名称',
    `status`        TINYINT(4)    NOT NULL COMMENT '状态: 0-try 1-confirm 2-cancel',
    `gmt_create`    DATETIME(3)   NOT NULL COMMENT '创建时间',
    `gmt_modified`  DATETIME(3)   NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`xid`, `branch_id`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_tx_action_name` (`action_name`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TCC 栅栏日志表';

CREATE DATABASE IF NOT EXISTS `account_db`;
USE `account_db`;
DROP TABLE IF EXISTS `account_tbl`;
CREATE TABLE `account_tbl`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `user_id`      varchar(255) DEFAULT NULL,
    `money`        int(11) DEFAULT 0,
    `frozen_money` int(11) DEFAULT 0 COMMENT 'TCC Try阶段冻结的余额',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO account_tbl (user_id, money) VALUES ('1', 10000);

-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20) NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11) NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- tcc_fence_log（2.x 新增）
CREATE TABLE IF NOT EXISTS `tcc_fence_log` (
    `xid`           VARCHAR(128)  NOT NULL COMMENT '全局事务 xid',
    `branch_id`     BIGINT(20)    NOT NULL COMMENT '分支事务 id',
    `action_name`   VARCHAR(64)   NOT NULL COMMENT 'TCC 资源名称',
    `status`        TINYINT(4)    NOT NULL COMMENT '状态: 0-try 1-confirm 2-cancel',
    `gmt_create`    DATETIME(3)   NOT NULL COMMENT '创建时间',
    `gmt_modified`  DATETIME(3)   NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`xid`, `branch_id`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_tx_action_name` (`action_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TCC 栅栏日志表';