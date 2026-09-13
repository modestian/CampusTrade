-- ============================================================
-- 校园二手交易平台 数据库初始化脚本
-- 数据库：campus_trade（独立库，不影响其他项目数据）
-- 说明：MySQL 8.0
-- 账号：管理员 admin / 123456 ； 学生 20230001 / 123456
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_trade;

-- ------------------------------------------------------------
-- 1. 学生用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(32)  NOT NULL COMMENT '学号（登录账号）',
    password     VARCHAR(64)  NOT NULL COMMENT 'MD5加盐密码',
    nickname     VARCHAR(32)  NOT NULL COMMENT '昵称',
    avatar_color VARCHAR(16)  NOT NULL DEFAULT '#74B9FF' COMMENT '头像色块 hex',
    phone        VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    campus       VARCHAR(32)  DEFAULT NULL COMMENT '常用校区/楼栋',
    credit       INT          NOT NULL DEFAULT 100 COMMENT '信用分',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1封禁',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '学生用户表（买卖一体账号）';

-- ------------------------------------------------------------
-- 2. 管理员表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_admin;
CREATE TABLE tb_admin (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(32) NOT NULL COMMENT '登录账号',
    password     VARCHAR(64) NOT NULL COMMENT 'MD5加盐密码',
    name         VARCHAR(32) NOT NULL COMMENT '姓名',
    avatar_color VARCHAR(16) DEFAULT '#636E72' COMMENT '头像色块',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '管理员表';

-- ------------------------------------------------------------
-- 3. 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_category;
CREATE TABLE tb_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32) NOT NULL COMMENT '分类名称',
    icon_color  VARCHAR(16) NOT NULL DEFAULT '#DFE6E9' COMMENT '分类色块 hex',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT = '商品分类表';

-- ------------------------------------------------------------
-- 4. 商品表（核心，默认单件先到先得）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_product;
CREATE TABLE tb_product (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    seller_id       BIGINT        NOT NULL COMMENT '卖家用户ID',
    category_id     BIGINT        NOT NULL COMMENT '分类ID',
    title           VARCHAR(64)   NOT NULL COMMENT '商品标题',
    description     VARCHAR(512)  DEFAULT NULL COMMENT '商品描述',
    cover_color     VARCHAR(16)   NOT NULL COMMENT '封面色块 hex（替代图片）',
    price           DECIMAL(10,2) NOT NULL COMMENT '出售价',
    original_price  DECIMAL(10,2) DEFAULT NULL COMMENT '购入原价',
    condition_level TINYINT       NOT NULL DEFAULT 3 COMMENT '成色 1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5磨损明显',
    delivery_type   TINYINT       NOT NULL DEFAULT 1 COMMENT '交付方式 1校内面交 2快递代取',
    campus          VARCHAR(32)   DEFAULT NULL COMMENT '面交地点',
    view_count      INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
    collect_count   INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
    like_count      INT           NOT NULL DEFAULT 0 COMMENT '点赞数',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审核 1在售 2交易中 3已售出 4已下架 5已驳回',
    audit_remark    VARCHAR(128)  DEFAULT NULL COMMENT '审核意见（驳回原因）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_seller (seller_id),
    KEY idx_category (category_id),
    KEY idx_status (status),
    KEY idx_title (title)
) ENGINE = InnoDB COMMENT = '商品表';

-- ------------------------------------------------------------
-- 5. 订单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_order;
CREATE TABLE tb_order (
    id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no             VARCHAR(32)   NOT NULL COMMENT '订单号',
    product_id           BIGINT        NOT NULL COMMENT '商品ID',
    buyer_id             BIGINT        NOT NULL COMMENT '买家ID',
    seller_id            BIGINT        NOT NULL COMMENT '卖家ID',
    amount               DECIMAL(10,2) NOT NULL COMMENT '成交金额',
    status               TINYINT       NOT NULL DEFAULT 1 COMMENT '1待支付 2待发货 3待收货 4已完成 5已取消 6退款中 7已退款',
    delivery_type        TINYINT       NOT NULL DEFAULT 1 COMMENT '交付方式快照',
    remark               VARCHAR(128)  DEFAULT NULL COMMENT '买家留言',
    cancel_reason        VARCHAR(128)  DEFAULT NULL COMMENT '取消原因',
    refund_reason        VARCHAR(128)  DEFAULT NULL COMMENT '退款申请原因',
    refund_reject_reason VARCHAR(128)  DEFAULT NULL COMMENT '卖家拒绝退款理由',
    pay_time             DATETIME      DEFAULT NULL COMMENT '支付时间',
    deliver_time         DATETIME      DEFAULT NULL COMMENT '发货时间',
    receive_time         DATETIME      DEFAULT NULL COMMENT '收货时间',
    cancel_time          DATETIME      DEFAULT NULL COMMENT '取消时间',
    create_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_buyer (buyer_id),
    KEY idx_seller (seller_id),
    KEY idx_status (status),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT = '订单表';

-- ------------------------------------------------------------
-- 6. 收藏表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_collect;
CREATE TABLE tb_collect (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT = '收藏表';

-- ------------------------------------------------------------
-- 7. 商品点赞表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_product_like;
CREATE TABLE tb_product_like (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE = InnoDB COMMENT = '商品点赞表';

-- ------------------------------------------------------------
-- 8. 关注表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_follow;
CREATE TABLE tb_follow (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT   NOT NULL COMMENT '关注者（粉丝）ID',
    follow_user_id BIGINT   NOT NULL COMMENT '被关注者ID',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_follow (user_id, follow_user_id),
    KEY idx_follow_user (follow_user_id)
) ENGINE = InnoDB COMMENT = '关注表';

-- ------------------------------------------------------------
-- 9. 评价表（一单一评）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_review;
CREATE TABLE tb_review (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT       NOT NULL COMMENT '订单ID',
    product_id   BIGINT       NOT NULL COMMENT '商品ID',
    reviewer_id  BIGINT       NOT NULL COMMENT '评价人（买家）ID',
    seller_id    BIGINT       NOT NULL COMMENT '被评价卖家ID',
    score        TINYINT      NOT NULL COMMENT '评分1~5',
    content      VARCHAR(256) DEFAULT NULL COMMENT '评价内容',
    is_anonymous TINYINT      NOT NULL DEFAULT 0 COMMENT '0实名 1匿名',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order (order_id),
    KEY idx_product (product_id),
    KEY idx_seller (seller_id)
) ENGINE = InnoDB COMMENT = '订单评价表';

-- ------------------------------------------------------------
-- 10. 举报表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_report;
CREATE TABLE tb_report (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    reporter_id   BIGINT       NOT NULL COMMENT '举报人ID',
    product_id    BIGINT       NOT NULL COMMENT '被举报商品ID',
    reason        VARCHAR(64)  NOT NULL COMMENT '举报原因',
    description   VARCHAR(256) DEFAULT NULL COMMENT '补充说明',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理(下架) 2已驳回',
    handle_remark VARCHAR(128) DEFAULT NULL COMMENT '处理备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT = '举报表';

-- ------------------------------------------------------------
-- 11. 公告表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_notice;
CREATE TABLE tb_notice (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    admin_id    BIGINT       NOT NULL COMMENT '发布管理员ID',
    title       VARCHAR(64)  NOT NULL COMMENT '公告标题',
    content     VARCHAR(512) NOT NULL COMMENT '公告内容',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1展示 0隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '公告表';

-- ------------------------------------------------------------
-- 12. 私聊消息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tb_chat_message;
CREATE TABLE tb_chat_message (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    from_user_id BIGINT       NOT NULL COMMENT '发送方',
    to_user_id   BIGINT       NOT NULL COMMENT '接收方',
    content      VARCHAR(512) NOT NULL COMMENT '消息内容',
    is_read      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_from_to (from_user_id, to_user_id),
    KEY idx_to_read (to_user_id, is_read)
) ENGINE = InnoDB COMMENT = '私聊消息表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 管理员（admin / 123456，MD5加盐）
INSERT INTO tb_admin (username, password, name, avatar_color, status) VALUES
('admin', '84eaa8e526af55c7c63bc0b665413449', '平台管理员', '#636E72', 1);

-- 学生（密码均为 123456）
INSERT INTO tb_user (username, password, nickname, avatar_color, phone, campus, credit, status, create_time, update_time) VALUES
('20230001', '84eaa8e526af55c7c63bc0b665413449', '测试同学', '#74B9FF', '13800000001', '梅园1栋', 100, 0, NOW() - INTERVAL 6 DAY, NOW()),
('20230002', '84eaa8e526af55c7c63bc0b665413449', '卖家小张', '#FF7675', '13800000002', '竹园3栋', 100, 0, NOW() - INTERVAL 30 DAY, NOW()),
('20230003', '84eaa8e526af55c7c63bc0b665413449', '毕业学长', '#55EFC4', '13800000003', '桂园5栋', 98, 0, NOW() - INTERVAL 60 DAY, NOW());

-- 分类
INSERT INTO tb_category (name, icon_color, sort, status) VALUES
('教材书籍', '#FF7675', 1, 1),
('数码电子', '#74B9FF', 2, 1),
('生活用品', '#55EFC4', 3, 1),
('服饰鞋包', '#A29BFE', 4, 1),
('运动户外', '#FAB1A0', 5, 1),
('美妆个护', '#FFEAA7', 6, 1),
('乐器文具', '#81ECEC', 7, 1),
('其他', '#DFE6E9', 8, 1);

-- 公告
INSERT INTO tb_notice (admin_id, title, content, status) VALUES
(1, '欢迎来到校园二手交易平台', '交易请优先选择校内面交，注意人身与财产安全；遇到违规商品请及时举报。', 1),
(1, '毕业季清仓活动预告', '平台即将上线毕业季限时抢购专区，敬请期待！', 1);

-- 商品（覆盖：在售 / 交易中 / 待审核 / 已售出）
INSERT INTO tb_product (seller_id, category_id, title, description, cover_color, price, original_price, condition_level, delivery_type, campus, view_count, collect_count, like_count, status, create_time, update_time) VALUES
(2, 1, '《数据结构（C语言版）》教材', '大二用书，几乎全新，无笔记无划线，期末复习必备。', '#FF7675', 15.00, 45.00, 2, 1, '三号教学楼门口', 36, 2, 3, 1, NOW() - INTERVAL 2 DAY, NOW()),
(3, 5, '捷安特山地车 24速', '毕业出，骑了一年多，刹车变速正常，送车锁和打气筒。', '#55EFC4', 320.00, 1200.00, 3, 1, '南大门快递驿站', 128, 9, 15, 1, NOW() - INTERVAL 3 DAY, NOW()),
(2, 3, '小米智能台灯', '用了一学期，支持无级调光，宿舍学习护眼神器。', '#FDCB6E', 25.00, 79.00, 2, 2, '竹园3栋楼下', 58, 4, 6, 2, NOW() - INTERVAL 1 DAY, NOW()),
(3, 2, 'QCY蓝牙耳机 T13', '音质不错，续航给力，换了新的出闲置，带原装充电盒。', '#74B9FF', 45.00, 129.00, 3, 1, '桂园5栋门口', 12, 1, 2, 0, NOW() - INTERVAL 6 HOUR, NOW()),
(2, 1, '考研英语黄皮书真题', '考研上岸出，真题册有少量笔记，介意勿拍。', '#A29BFE', 30.00, 68.00, 3, 1, '图书馆一楼大厅', 95, 12, 18, 3, NOW() - INTERVAL 10 DAY, NOW());

-- 示例订单（订单1：已完成；订单2：待发货）
INSERT INTO tb_order (order_no, product_id, buyer_id, seller_id, amount, status, delivery_type, remark, pay_time, deliver_time, receive_time, create_time, update_time) VALUES
('20260903000000000001', 5, 1, 2, 30.00, 4, 1, '学长好，周五下午图书馆交易可以吗？',
 NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY),
('20260913000000000001', 3, 1, 2, 25.00, 2, 2, '麻烦傍晚送到竹园3栋，谢谢！',
 NOW() - INTERVAL 5 HOUR, NULL, NULL, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 5 HOUR);

-- 示例评价（对应订单1）
INSERT INTO tb_review (order_id, product_id, reviewer_id, seller_id, score, content, is_anonymous, create_time) VALUES
(1, 5, 1, 2, 5, '书很新，学长人很好，还送了笔记，强烈推荐！', 0, NOW() - INTERVAL 8 DAY);

-- 示例关注 / 收藏 / 点赞
INSERT INTO tb_follow (user_id, follow_user_id, create_time) VALUES
(1, 2, NOW() - INTERVAL 5 DAY),
(1, 3, NOW() - INTERVAL 4 DAY);

INSERT INTO tb_collect (user_id, product_id, create_time) VALUES
(1, 2, NOW() - INTERVAL 2 DAY),
(1, 3, NOW() - INTERVAL 1 DAY);

INSERT INTO tb_product_like (user_id, product_id, create_time) VALUES
(1, 2, NOW() - INTERVAL 2 DAY),
(3, 2, NOW() - INTERVAL 1 DAY),
(1, 5, NOW() - INTERVAL 8 DAY);

-- 示例私聊
INSERT INTO tb_chat_message (from_user_id, to_user_id, content, is_read, create_time) VALUES
(1, 2, '同学你好，台灯还在吗？', 1, NOW() - INTERVAL 7 HOUR),
(2, 1, '在的，25 元，傍晚可以送到竹园。', 1, NOW() - INTERVAL 6 HOUR),
(1, 2, '好的，那我先下单啦~', 1, NOW() - INTERVAL 6 HOUR);
