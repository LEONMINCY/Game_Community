/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : localhost:3306
 Source Schema         : gaming_db

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 15/06/2026 19:02:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_account_sequence
-- ----------------------------
DROP TABLE IF EXISTS `t_account_sequence`;
CREATE TABLE `t_account_sequence`  (
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1014 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_account_sequence
-- ----------------------------

-- ----------------------------
-- Table structure for t_admin_user
-- ----------------------------
DROP TABLE IF EXISTS `t_admin_user`;
CREATE TABLE `t_admin_user`  (
  `id` int NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role_id` bigint NOT NULL,
  `enable_flag` tinyint(1) NULL DEFAULT 1,
  `ban_end_time` datetime NULL DEFAULT NULL,
  `last_login` datetime NULL DEFAULT NULL,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_admin_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_t_admin_user_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_t_admin_user_role`(`role_id` ASC) USING BTREE,
  INDEX `idx_admin_user_created`(`created_time` ASC) USING BTREE,
  INDEX `idx_admin_user_last_login`(`last_login` ASC) USING BTREE,
  INDEX `idx_admin_user_deleted_username`(`deleted` ASC, `username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_admin_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_conversation`;
CREATE TABLE `t_ai_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_conversation_user_update`(`user_id` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_ai_conversation
-- ----------------------------

-- ----------------------------
-- Table structure for t_ai_message
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_message`;
CREATE TABLE `t_ai_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `image_urls` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI消息图片URL JSON',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_message_conversation_time`(`conversation_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_message_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_ai_message
-- ----------------------------

-- ----------------------------
-- Table structure for t_browse_history
-- ----------------------------
DROP TABLE IF EXISTS `t_browse_history`;
CREATE TABLE `t_browse_history`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '浏览用户id',
  `target_id` int NOT NULL COMMENT '浏览目标 ID（game 或 news 的唯一标识)',
  `target_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '目标类型（game 或 news）',
  `browse_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '浏览时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_browse_user_type_time`(`user_id` ASC, `target_type` ASC, `browse_time` ASC) USING BTREE,
  INDEX `idx_browse_target_type_time`(`target_id` ASC, `target_type` ASC, `browse_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2576 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_browse_history
-- ----------------------------

-- ----------------------------
-- Table structure for t_comment
-- ----------------------------
DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '评论唯一标识',
  `post_id` bigint NOT NULL COMMENT '所属帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `parent_id` int NULL DEFAULT NULL COMMENT '父评论ID',
  `reply_user_id` int NULL DEFAULT NULL COMMENT '回复的目标用户ID',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评论图片',
  `likes_user` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评论点赞用户ID集合',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comment_post_deleted_time`(`post_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_comment_parent_deleted_time`(`parent_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_comment_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_comment_post_parent_deleted_time`(`post_id` ASC, `parent_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 123 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_comment
-- ----------------------------

-- ----------------------------
-- Table structure for t_game
-- ----------------------------
DROP TABLE IF EXISTS `t_game`;
CREATE TABLE `t_game`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '游戏唯一标识',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '游戏名称',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏icon',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏类型',
  `developer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开发商名称',
  `price` int NULL DEFAULT NULL COMMENT '游戏价格',
  `rating` decimal(3, 1) NULL DEFAULT 0.0 COMMENT '玩家评分',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '游戏简介',
  `screenshots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '游戏截图URL（JSON数组）',
  `video` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏演示视频URL',
  `system_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统配置要求',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `discount` int NULL DEFAULT 0 COMMENT '折扣百分比',
  `price_mark` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '价格标记：historical_low/tie_historical_low',
  `release_date` date NULL DEFAULT NULL COMMENT '发售日期',
  `platforms` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'supported game platforms',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_game_deleted_name`(`deleted` ASC, `name` ASC) USING BTREE,
  INDEX `idx_game_deleted_type`(`deleted` ASC, `type` ASC) USING BTREE,
  INDEX `idx_game_deleted_developer`(`deleted` ASC, `developer` ASC) USING BTREE,
  INDEX `idx_game_discount_deleted`(`discount` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_game
-- ----------------------------

-- ----------------------------
-- Table structure for t_game_cart
-- ----------------------------
DROP TABLE IF EXISTS `t_game_cart`;
CREATE TABLE `t_game_cart`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `game_id` int NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cart_user_game`(`user_id` ASC, `game_id` ASC) USING BTREE,
  INDEX `idx_cart_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_cart_user_game_deleted`(`user_id` ASC, `game_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_game_cart
-- ----------------------------

-- ----------------------------
-- Table structure for t_game_rating
-- ----------------------------
DROP TABLE IF EXISTS `t_game_rating`;
CREATE TABLE `t_game_rating`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `game_id` int NOT NULL,
  `user_id` int NOT NULL,
  `rating` int NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评价内容',
  `recommend` tinyint(1) NULL DEFAULT NULL COMMENT '是否好评',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_game_user`(`game_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_rating_game_time`(`game_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_rating_game_recommend_time`(`game_id` ASC, `recommend` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_rating_user_game`(`user_id` ASC, `game_id` ASC) USING BTREE,
  CONSTRAINT `t_game_rating_chk_1` CHECK ((`rating` >= 1) and (`rating` <= 10))
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_game_rating
-- ----------------------------

-- ----------------------------
-- Table structure for t_game_wishlist
-- ----------------------------
DROP TABLE IF EXISTS `t_game_wishlist`;
CREATE TABLE `t_game_wishlist`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `game_id` int NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wishlist_user_game`(`user_id` ASC, `game_id` ASC) USING BTREE,
  INDEX `idx_wishlist_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_wishlist_user_game_deleted`(`user_id` ASC, `game_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_game_wishlist
-- ----------------------------

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `index` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` int NULL DEFAULT 0,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_menu_index`(`index` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_menu
-- ----------------------------

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息唯一标识',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `read_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_sender_receiver_time`(`sender_id` ASC, `receiver_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_message_receiver_time`(`receiver_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_message_receiver_read_time`(`receiver_id` ASC, `read_flag` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_message_receiver_read`(`receiver_id` ASC, `read_flag` ASC, `sender_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 232 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_message
-- ----------------------------

-- ----------------------------
-- Table structure for t_moderator_user
-- ----------------------------
DROP TABLE IF EXISTS `t_moderator_user`;
CREATE TABLE `t_moderator_user`  (
  `id` int NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role_id` bigint NOT NULL,
  `enable_flag` tinyint(1) NULL DEFAULT 1,
  `ban_end_time` datetime NULL DEFAULT NULL,
  `last_login` datetime NULL DEFAULT NULL,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_moderator_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_t_moderator_user_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_t_moderator_user_role`(`role_id` ASC) USING BTREE,
  INDEX `idx_moderator_user_created`(`created_time` ASC) USING BTREE,
  INDEX `idx_moderator_user_last_login`(`last_login` ASC) USING BTREE,
  INDEX `idx_moderator_user_deleted_username`(`deleted` ASC, `username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_moderator_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_news
-- ----------------------------
DROP TABLE IF EXISTS `t_news`;
CREATE TABLE `t_news`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '新闻id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '新闻标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '新闻内容',
  `media_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '帖子媒体内容（JSON数组，图片）',
  `brow_count` int NULL DEFAULT 0 COMMENT '浏览量',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `likes_user` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '新闻点赞用户',
  `favorites_user` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '新闻收藏用户',
  `share_count` int NULL DEFAULT 0 COMMENT '新闻转发次数',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_news_deleted_time`(`deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_news_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区帖子表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_news
-- ----------------------------

-- ----------------------------
-- Table structure for t_news_comment
-- ----------------------------
DROP TABLE IF EXISTS `t_news_comment`;
CREATE TABLE `t_news_comment`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `news_id` int NOT NULL,
  `user_id` int NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parent_id` int NULL DEFAULT NULL,
  `reply_user_id` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_news_comment_news`(`news_id` ASC, `deleted` ASC, `parent_id` ASC) USING BTREE,
  INDEX `idx_news_comment_parent`(`parent_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_news_comment_news_parent_time`(`news_id` ASC, `deleted` ASC, `parent_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_news_comment_parent_time`(`parent_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_news_comment
-- ----------------------------

-- ----------------------------
-- Table structure for t_notification
-- ----------------------------
DROP TABLE IF EXISTS `t_notification`;
CREATE TABLE `t_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `actor_id` int NULL DEFAULT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `target_id` int NULL DEFAULT NULL,
  `target_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `read_flag` tinyint(1) NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_user_read`(`user_id` ASC, `read_flag` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_notification_target`(`target_type` ASC, `target_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_notification
-- ----------------------------

-- ----------------------------
-- Table structure for t_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role_id` int NULL DEFAULT NULL,
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `module_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operation_name` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `controller_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `method_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `success` tinyint(1) NOT NULL DEFAULT 1,
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cost_time` bigint NULL DEFAULT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_log_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_user`(`user_id` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_role`(`role_id` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_success`(`success` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_deleted_time`(`deleted` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_username_time`(`username` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_role_time`(`role_id` ASC, `created_time` ASC) USING BTREE,
  INDEX `idx_operation_log_success_time`(`success` ASC, `created_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60141 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单唯一标识',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `game_id` bigint NOT NULL COMMENT '游戏ID',
  `order_no` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '订单状态',
  `total_price` decimal(10, 2) NOT NULL COMMENT '总价格',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `refund_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NONE' COMMENT '退款状态',
  `refund_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款原因',
  `refund_reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款审核回复',
  `refund_apply_time` datetime NULL DEFAULT NULL COMMENT '退款申请时间',
  `refund_review_time` datetime NULL DEFAULT NULL COMMENT '退款审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_order_user_status_time`(`user_id` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_order_user_game_status`(`user_id` ASC, `game_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_order_game_status_time`(`game_id` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_order_refund_status_time`(`refund_status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_order_user_deleted_time`(`user_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_order_deleted_time`(`deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_order_status_time`(`status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 239 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_order
-- ----------------------------

-- ----------------------------
-- Table structure for t_player_user
-- ----------------------------
DROP TABLE IF EXISTS `t_player_user`;
CREATE TABLE `t_player_user`  (
  `id` int NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role_id` bigint NOT NULL,
  `enable_flag` tinyint(1) NULL DEFAULT 1,
  `ban_end_time` datetime NULL DEFAULT NULL,
  `last_login` datetime NULL DEFAULT NULL,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_player_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_t_player_user_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_t_player_user_role`(`role_id` ASC) USING BTREE,
  INDEX `idx_player_user_created`(`created_time` ASC) USING BTREE,
  INDEX `idx_player_user_last_login`(`last_login` ASC) USING BTREE,
  INDEX `idx_player_user_deleted_username`(`deleted` ASC, `username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_player_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_post
-- ----------------------------
DROP TABLE IF EXISTS `t_post`;
CREATE TABLE `t_post`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '帖子唯一标识',
  `user_id` int NOT NULL COMMENT '发帖用户ID',
  `game_id` int NULL DEFAULT NULL COMMENT '关联游戏id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子内容',
  `media` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '帖子媒体内容（JSON数组，图文/视频）',
  `likes_user` varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '点赞用户',
  `favorites_user` varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '收藏用户',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'approved' COMMENT '审核状态',
  `audit_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核拒绝原因',
  `appeal_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '申诉内容',
  `appeal_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'none' COMMENT '申诉状态',
  `appeal_reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申诉处理回复',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `appeal_time` datetime NULL DEFAULT NULL COMMENT '申诉时间',
  `share_count` int NULL DEFAULT 0 COMMENT '转发次数',
  `topics` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '帖子话题标签',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_deleted_audit_game_time`(`deleted` ASC, `audit_status` ASC, `game_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_post_deleted_game_time`(`deleted` ASC, `game_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_post_user_deleted_time`(`user_id` ASC, `deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_post_audit_appeal_time`(`audit_status` ASC, `appeal_status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_post_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区帖子表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_post
-- ----------------------------

-- ----------------------------
-- Table structure for t_report
-- ----------------------------
DROP TABLE IF EXISTS `t_report`;
CREATE TABLE `t_report`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '举报唯一标识',
  `reported_id` bigint NOT NULL COMMENT '被举报对象ID',
  `report_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型（POST/COMMENT/USER）',
  `user_id` bigint NOT NULL COMMENT '举报用户ID',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '举报理由',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '举报状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  `reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '举报处理回复',
  `update_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `evidence_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '举报文字证据',
  `evidence_images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '举报图片证据',
  `appeal_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '被举报人申诉内容',
  `appeal_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '被举报人申诉状态',
  `appeal_reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '举报申诉处理回复',
  `appeal_time` datetime NULL DEFAULT NULL COMMENT '被举报人申诉时间',
  `appeal_review_time` datetime NULL DEFAULT NULL COMMENT '举报申诉审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_status_type_time`(`status` ASC, `report_type` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_report_reported_type`(`reported_id` ASC, `report_type` ASC) USING BTREE,
  INDEX `idx_report_deleted_time`(`deleted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_report_deleted_status_type_time`(`deleted` ASC, `status` ASC, `report_type` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '举报表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_report
-- ----------------------------

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_role
-- ----------------------------

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_t_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码',
  `role_id` int NOT NULL COMMENT '角色',
  `enable_flag` tinyint(1) NULL DEFAULT 1 COMMENT '启用状态',
  `last_login` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `ban_end_time` datetime NULL DEFAULT NULL COMMENT '封禁截止时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1013 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user
-- ----------------------------

-- ----------------------------
-- Table structure for t_user_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `t_user_blacklist`;
CREATE TABLE `t_user_blacklist`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `blocked_user_id` int NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_blacklist_pair`(`user_id` ASC, `blocked_user_id` ASC) USING BTREE,
  INDEX `idx_user_blacklist_user`(`user_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_blacklist
-- ----------------------------

-- ----------------------------
-- Table structure for t_user_daily_task
-- ----------------------------
DROP TABLE IF EXISTS `t_user_daily_task`;
CREATE TABLE `t_user_daily_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `task_date` date NOT NULL,
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `action_count` int NULL DEFAULT 0,
  `exp_gained` int NULL DEFAULT 0,
  `created_time` datetime NULL DEFAULT NULL,
  `updated_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_daily_task_user_type`(`user_id` ASC, `task_date` ASC, `task_type` ASC) USING BTREE,
  INDEX `idx_daily_task_user_date`(`user_id` ASC, `task_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user_daily_task
-- ----------------------------

-- ----------------------------
-- Table structure for t_user_info
-- ----------------------------
DROP TABLE IF EXISTS `t_user_info`;
CREATE TABLE `t_user_info`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户id',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `gender` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `phone` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `ex1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拓展1',
  `ex2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拓展2',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `signature` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个性签名',
  `privacy_profile` tinyint(1) NULL DEFAULT 0 COMMENT '隐藏个人资料',
  `privacy_follow` tinyint(1) NULL DEFAULT 0 COMMENT '隐藏关注列表',
  `privacy_fans` tinyint(1) NULL DEFAULT 0 COMMENT '隐藏粉丝列表',
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定邮箱',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_info_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_info_deleted_nickname`(`deleted` ASC, `nickname` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1014 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user_info
-- ----------------------------

-- ----------------------------
-- Table structure for t_user_relation
-- ----------------------------
DROP TABLE IF EXISTS `t_user_relation`;
CREATE TABLE `t_user_relation`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关系唯一标识',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `follow_id` bigint NOT NULL COMMENT '关注用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标识（0-正常，1-删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_relation_user_deleted`(`user_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_relation_follow_deleted`(`follow_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 221 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user_relation
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
