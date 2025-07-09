USE `hoj`;

/*
* 2021.08.07 修改OI题目得分在OI排行榜新计分字段 分数计算为：OI题目总得分*0.1+2*题目难度
*/
DROP PROCEDURE
IF EXISTS judge_Add_oi_rank_score;
DELIMITER $$

CREATE PROCEDURE judge_Add_oi_rank_score ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'oi_rank'
) THEN
	ALTER TABLE judge ADD COLUMN oi_rank INT(11) NULL COMMENT '该题在OI排行榜的分数';
END
IF ; END$$

DELIMITER ;
CALL judge_Add_oi_rank_score ;

DROP PROCEDURE judge_Add_oi_rank_score;

/*
* 2021.08.08 增加vjudge_submit_id在vjudge判题获取提交id后存储，当等待结果超时，下次重判时可用该提交id直接获取结果。
			 同时vjudge_username、vjudge_password分别记录提交账号密码
*/
DROP PROCEDURE
IF EXISTS judge_Add_vjudge_submit_id;
DELIMITER $$

CREATE PROCEDURE judge_Add_vjudge_submit_id ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'vjudge_submit_id'
) THEN
	ALTER TABLE judge ADD COLUMN vjudge_submit_id BIGINT UNSIGNED NULL  COMMENT 'vjudge判题在其它oj的提交id';
	ALTER TABLE judge ADD COLUMN vjudge_username VARCHAR(255) NULL  COMMENT 'vjudge判题在其它oj的提交用户名';
	ALTER TABLE judge ADD COLUMN vjudge_password VARCHAR(255) NULL  COMMENT 'vjudge判题在其它oj的提交账号密码';
END
IF ; END$$

DELIMITER ;
CALL judge_Add_vjudge_submit_id ;

DROP PROCEDURE judge_Add_vjudge_submit_id;


/*
* 2021.09.21 比赛增加打印、账号限制的功能，增大真实姓名长度
*/

DROP PROCEDURE
IF EXISTS contest_Add_print_and_limit;
DELIMITER $$

CREATE PROCEDURE contest_Add_print_and_limit ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'open_print'
) THEN
	ALTER TABLE contest ADD COLUMN open_print tinyint(1) DEFAULT '0' COMMENT '是否打开打印功能';
    ALTER TABLE contest ADD COLUMN open_account_limit tinyint(1) DEFAULT '0' COMMENT '是否开启账号限制';
    ALTER TABLE contest ADD COLUMN account_limit_rule mediumtext COMMENT '账号限制规则';
	ALTER TABLE `hoj`.`user_info` CHANGE `realname` `realname` VARCHAR(100) CHARSET utf8 COLLATE utf8_general_ci NULL  COMMENT '真实姓名';
END
IF ; END$$

DELIMITER ;
CALL contest_Add_print_and_limit ;

DROP PROCEDURE contest_Add_print_and_limit;



DROP PROCEDURE
IF EXISTS Add_contest_print;
DELIMITER $$

CREATE PROCEDURE Add_contest_print ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_print'
) THEN
	CREATE TABLE `contest_print` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `username` varchar(100) DEFAULT NULL,
	  `realname` varchar(100) DEFAULT NULL,
	  `cid` bigint(20) unsigned DEFAULT NULL,
	  `content` longtext NOT NULL,
	  `status` int(11) DEFAULT '0',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `cid` (`cid`),
	  KEY `username` (`username`),
	  CONSTRAINT `contest_print_ibfk_1` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `contest_print_ibfk_2` FOREIGN KEY (`username`) REFERENCES `user_info` (`username`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL Add_contest_print ;

DROP PROCEDURE Add_contest_print;


/*
* 2021.10.04 增加站内消息系统，包括评论我的、收到的赞、回复我的、系统通知、我的消息五个模块
*/

DROP PROCEDURE
IF EXISTS Add_msg_table;
DELIMITER $$

CREATE PROCEDURE Add_msg_table ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'msg_remind'
) THEN
	CREATE TABLE `admin_sys_notice` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `title` varchar(255) DEFAULT NULL COMMENT '标题',
	  `content` longtext COMMENT '内容',
	  `type` varchar(255) DEFAULT NULL COMMENT '发给哪些用户类型',
	  `state` tinyint(1) DEFAULT '0' COMMENT '是否已拉取给用户',
	  `recipient_id` varchar(32) DEFAULT NULL COMMENT '接受通知的用户id',
	  `admin_id` varchar(32) DEFAULT NULL COMMENT '发送通知的管理员id',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
	  PRIMARY KEY (`id`),
	  KEY `recipient_id` (`recipient_id`),
	  KEY `admin_id` (`admin_id`),
	  CONSTRAINT `admin_sys_notice_ibfk_1` FOREIGN KEY (`recipient_id`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `admin_sys_notice_ibfk_2` FOREIGN KEY (`admin_id`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	CREATE TABLE `msg_remind` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `action` varchar(255) NOT NULL COMMENT '动作类型，如点赞讨论帖Like_Post、点赞评论Like_Discuss、评论Discuss、回复Reply等',
	  `source_id` int(10) unsigned DEFAULT NULL COMMENT '消息来源id，讨论id或比赛id',
	  `source_type` varchar(255) DEFAULT NULL COMMENT '事件源类型：''Discussion''、''Contest''等',
	  `source_content` varchar(255) DEFAULT NULL COMMENT '事件源的内容，比如回复的内容，评论的帖子标题等等',
	  `quote_id` int(10) unsigned DEFAULT NULL COMMENT '事件引用上一级评论或回复id',
	  `quote_type` varchar(255) DEFAULT NULL COMMENT '事件引用上一级的类型：Comment、Reply',
	  `url` varchar(255) DEFAULT NULL COMMENT '事件所发生的地点链接 url',
	  `state` tinyint(1) DEFAULT '0' COMMENT '是否已读',
	  `sender_id` varchar(32) DEFAULT NULL COMMENT '操作者的id',
	  `recipient_id` varchar(32) DEFAULT NULL COMMENT '接受消息的用户id',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
	  PRIMARY KEY (`id`),
	  KEY `sender_id` (`sender_id`),
	  KEY `recipient_id` (`recipient_id`),
	  CONSTRAINT `msg_remind_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `msg_remind_ibfk_2` FOREIGN KEY (`recipient_id`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	CREATE TABLE `user_sys_notice` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `sys_notice_id` bigint(20) unsigned DEFAULT NULL COMMENT '系统通知的id',
	  `recipient_id` varchar(32) DEFAULT NULL COMMENT '接受通知的用户id',
	  `type` varchar(255) DEFAULT NULL COMMENT '消息类型，系统通知sys、我的信息mine',
	  `state` tinyint(1) DEFAULT '0' COMMENT '是否已读',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `sys_notice_id` (`sys_notice_id`),
	  KEY `recipient_id` (`recipient_id`),
	  CONSTRAINT `user_sys_notice_ibfk_1` FOREIGN KEY (`sys_notice_id`) REFERENCES `admin_sys_notice` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `user_sys_notice_ibfk_2` FOREIGN KEY (`recipient_id`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL Add_msg_table;

DROP PROCEDURE Add_msg_table;




/*
* 2021.10.06 user_info增加性别列gender 比赛榜单用户名称显示可选

*/
DROP PROCEDURE
IF EXISTS user_info_Add_gender;
DELIMITER $$

CREATE PROCEDURE user_info_Add_gender ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_info'
	AND column_name = 'gender'
) THEN
	ALTER TABLE user_info ADD COLUMN gender varchar(20) DEFAULT 'secrecy'  NOT NULL COMMENT '性别';
END
IF ; END$$

DELIMITER ;
CALL user_info_Add_gender ;

DROP PROCEDURE user_info_Add_gender;


DROP PROCEDURE
IF EXISTS contest_Add_rank_show_name;
DELIMITER $$

CREATE PROCEDURE contest_Add_rank_show_name ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'rank_show_name'
) THEN
	ALTER TABLE contest ADD COLUMN rank_show_name varchar(20) DEFAULT 'username' COMMENT '排行榜显示（username、nickname、realname）';
END
IF ; END$$

DELIMITER ;
CALL contest_Add_rank_show_name ;

DROP PROCEDURE contest_Add_rank_show_name;

/*
* 2021.10.08 user_info增加性别列gender 比赛榜单用户名称显示可选

*/
DROP PROCEDURE
IF EXISTS contest_problem_Add_color;
DELIMITER $$

CREATE PROCEDURE contest_problem_Add_color ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_problem'
	AND column_name = 'color'
) THEN
	ALTER TABLE contest_problem ADD COLUMN `color` VARCHAR(255) NULL   COMMENT '气球颜色';
	ALTER TABLE user_info ADD COLUMN `title_name` VARCHAR(255) NULL   COMMENT '头衔、称号';
	ALTER TABLE user_info ADD COLUMN `title_color` VARCHAR(255) NULL   COMMENT '头衔、称号的颜色';
END
IF ; END$$

DELIMITER ;
CALL contest_problem_Add_color ;

DROP PROCEDURE contest_problem_Add_color;


/*
* 2021.11.17 judge_server增加cf_submittable控制单台判题机只能一个账号提交CF

*/
DROP PROCEDURE
IF EXISTS judge_server_Add_cf_submittable;
DELIMITER $$

CREATE PROCEDURE judge_serverm_Add_cf_submittable ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge_server'
	AND column_name = 'cf_submittable'
) THEN
	ALTER TABLE `hoj`.`judge_server`  ADD COLUMN `cf_submittable` BOOLEAN DEFAULT 1  NULL  COMMENT '是否可提交CF';
END
IF ; END$$

DELIMITER ;
CALL judge_serverm_Add_cf_submittable ;

DROP PROCEDURE judge_serverm_Add_cf_submittable;



/*
* 2021.11.29 增加训练模块
*/

DROP PROCEDURE
IF EXISTS Add_training_table;
DELIMITER $$

CREATE PROCEDURE Add_training_table ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'training'
) THEN

	CREATE TABLE `training` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `title` varchar(255) DEFAULT NULL COMMENT '训练题单名称',
	  `description` longtext COMMENT '训练题单简介',
	  `author` varchar(255) NOT NULL COMMENT '训练题单创建者用户名',
	  `auth` varchar(255) NOT NULL COMMENT '训练题单权限类型：Public、Private',
	  `private_pwd` varchar(255) DEFAULT NULL COMMENT '训练题单权限为Private时的密码',
	  `rank` int DEFAULT '0' COMMENT '编号，升序',
	  `status` tinyint(1) DEFAULT '1' COMMENT '是否可用',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


	CREATE TABLE `training_category` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `name` varchar(255) DEFAULT NULL,
	  `color` varchar(255) DEFAULT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	CREATE TABLE `training_problem` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL COMMENT '训练id',
	  `pid` bigint unsigned NOT NULL COMMENT '题目id',
	  `rank` int DEFAULT '0',
	  `display_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `pid` (`pid`),
	  KEY `display_id` (`display_id`),
	  CONSTRAINT `training_problem_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_problem_ibfk_2` FOREIGN KEY (`pid`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_problem_ibfk_3` FOREIGN KEY (`display_id`) REFERENCES `problem` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	CREATE TABLE `training_record` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL,
	  `tpid` bigint unsigned NOT NULL,
	  `pid` bigint unsigned NOT NULL,
	  `uid` varchar(255) NOT NULL,
	  `submit_id` bigint unsigned NOT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `tpid` (`tpid`),
	  KEY `pid` (`pid`),
	  KEY `uid` (`uid`),
	  KEY `submit_id` (`submit_id`),
	  CONSTRAINT `training_record_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_2` FOREIGN KEY (`tpid`) REFERENCES `training_problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_3` FOREIGN KEY (`pid`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_4` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_5` FOREIGN KEY (`submit_id`) REFERENCES `judge` (`submit_id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


	CREATE TABLE `training_register` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL COMMENT '训练id',
	  `uid` varchar(255) NOT NULL COMMENT '用户id',
	  `status` tinyint(1) DEFAULT '1' COMMENT '是否可用',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `uid` (`uid`),
	  CONSTRAINT `training_register_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_register_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;


	CREATE TABLE `mapping_training_category` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL,
	  `cid` bigint unsigned NOT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `cid` (`cid`),
	  CONSTRAINT `mapping_training_category_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `mapping_training_category_ibfk_2` FOREIGN KEY (`cid`) REFERENCES `training_category` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	ALTER TABLE `hoj`.`judge` ADD COLUMN `tid` BIGINT UNSIGNED NULL AFTER `cpid`,
	ADD FOREIGN KEY (`tid`) REFERENCES `hoj`.`training`(`id`) ON UPDATE CASCADE ON DELETE CASCADE;
END
IF ; END$$

DELIMITER ;
CALL Add_training_table;

DROP PROCEDURE Add_training_table;


/*
* 2021.12.05 contest增加auto_real_rank比赛结束是否自动解除封榜,自动转换成真实榜单

*/
DROP PROCEDURE
IF EXISTS contest_Add_auto_real_rank;
DELIMITER $$

CREATE PROCEDURE contest_Add_auto_real_rank()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'auto_real_rank'
) THEN
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `auto_real_rank` BOOLEAN DEFAULT 1  NULL  COMMENT '比赛结束是否自动解除封榜,自动转换成真实榜单';
	DROP TABLE `hoj`.`training_problem`;
	DROP TABLE `hoj`.`training_record`;
	CREATE TABLE `training_problem` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL COMMENT '训练id',
	  `pid` bigint unsigned NOT NULL COMMENT '题目id',
	  `rank` int DEFAULT '0',
	  `display_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `pid` (`pid`),
	  KEY `display_id` (`display_id`),
	  CONSTRAINT `training_problem_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_problem_ibfk_2` FOREIGN KEY (`pid`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_problem_ibfk_3` FOREIGN KEY (`display_id`) REFERENCES `problem` (`problem_id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	CREATE TABLE `training_record` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `tid` bigint unsigned NOT NULL,
	  `tpid` bigint unsigned NOT NULL,
	  `pid` bigint unsigned NOT NULL,
	  `uid` varchar(255) NOT NULL,
	  `submit_id` bigint unsigned NOT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `tid` (`tid`),
	  KEY `tpid` (`tpid`),
	  KEY `pid` (`pid`),
	  KEY `uid` (`uid`),
	  KEY `submit_id` (`submit_id`),
	  CONSTRAINT `training_record_ibfk_1` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_2` FOREIGN KEY (`tpid`) REFERENCES `training_problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_3` FOREIGN KEY (`pid`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_4` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `training_record_ibfk_5` FOREIGN KEY (`submit_id`) REFERENCES `judge` (`submit_id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL contest_Add_auto_real_rank;

DROP PROCEDURE contest_Add_auto_real_rank;




/*
* 2021.12.07 contest增加打星账号列表、是否开放榜单

*/
DROP PROCEDURE
IF EXISTS contest_Add_star_account_And_open_rank;
DELIMITER $$

CREATE PROCEDURE contest_Add_star_account_And_open_rank ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'star_account'
) THEN
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `star_account` mediumtext COMMENT '打星用户列表';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `open_rank` BOOLEAN DEFAULT 0 NULL  COMMENT '是否开放赛外榜单';
END
IF ; END$$

DELIMITER ;
CALL contest_Add_star_account_And_open_rank ;

DROP PROCEDURE contest_Add_star_account_And_open_rank;



/*
* 2021.12.19 judge表删除tid

*/
DROP PROCEDURE
IF EXISTS judge_Delete_tid;
DELIMITER $$

CREATE PROCEDURE judge_Delete_tid ()
BEGIN

IF EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'tid'
) THEN
	ALTER TABLE `hoj`.`judge` DROP foreign key `judge_ibfk_4`;
	ALTER TABLE `hoj`.`judge` DROP COLUMN `tid`;
END
IF ; END$$

DELIMITER ;
CALL judge_Delete_tid ;

DROP PROCEDURE judge_Delete_tid;


/*
* 2022.01.03 problem表增加mode，user_extra_file，judge_extra_file用于区别普通判题、特殊判题、交互判题

*/
DROP PROCEDURE
IF EXISTS problem_Add_judge_mode;
DELIMITER $$

CREATE PROCEDURE problem_Add_judge_mode ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'judge_mode'
) THEN
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `judge_mode` varchar(255) DEFAULT 'default' COMMENT '题目评测模式,default、spj、interactive';
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `user_extra_file` mediumtext DEFAULT NULL COMMENT '题目评测时用户程序的额外额外文件 json key:name value:content';
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `judge_extra_file` mediumtext DEFAULT NULL COMMENT '题目评测时交互或特殊程序的额外额外文件 json key:name value:content';
END
IF ; END$$

DELIMITER ;
CALL problem_Add_judge_mode ;

DROP PROCEDURE problem_Add_judge_mode;


/*
* 2022.03.02 contest表增加oi_rank_score_type

*/
DROP PROCEDURE
IF EXISTS contest_Add_oi_rank_score_type;
DELIMITER $$

CREATE PROCEDURE contest_Add_oi_rank_score_type ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'oi_rank_score_type'
) THEN
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `oi_rank_score_type` varchar(255) DEFAULT 'Recent' COMMENT 'oi排行榜得分方式，Recent、Highest';
END
IF ; END$$

DELIMITER ;
CALL contest_Add_oi_rank_score_type ;

DROP PROCEDURE contest_Add_oi_rank_score_type;


/*
* 2022.03.28 增加团队模块

*/
DROP PROCEDURE
IF EXISTS add_group;
DELIMITER $$

CREATE PROCEDURE add_group ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'group'
) THEN
	CREATE TABLE `group` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
	  `name` varchar(25) DEFAULT NULL COMMENT '团队名称',
	  `short_name` varchar(10) DEFAULT NULL COMMENT '团队简称',
	  `brief` varchar(50) COMMENT '团队简介',
	  `description` longtext COMMENT '团队介绍',
	  `owner` varchar(255) NOT NULL COMMENT '团队拥有者用户名',
	  `uid` varchar(32) NOT NULL COMMENT '团队拥有者用户id',
	  `auth` int(11) NOT NULL COMMENT '0为Public，1为Protected，2为Private',
	  `visible` tinyint(1) DEFAULT '1' COMMENT '是否可见',
	  `status` tinyint(1) DEFAULT '0' COMMENT '是否封禁',
	  `code` varchar(6) DEFAULT NULL COMMENT '邀请码',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  UNIQUE KEY `NAME_UNIQUE` (`name`),
	  UNIQUE KEY `short_name` (`short_name`),
	  CONSTRAINT `group_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8;

	CREATE TABLE `group_member` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `gid` bigint unsigned NOT NULL COMMENT '团队id',
	  `uid` varchar(32) NOT NULL COMMENT '用户id',
	  `auth` int(11) DEFAULT '1' COMMENT '1未审批，2拒绝，3普通成员，4团队管理员，5团队拥有者',
	  `reason` varchar(100) DEFAULT NULL COMMENT '申请理由',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  UNIQUE KEY `gid_uid_unique` (`gid`, `uid`),
	  KEY `gid` (`gid`),
	  KEY `uid` (`uid`),
	  CONSTRAINT `group_member_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  CONSTRAINT `group_member_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

	ALTER TABLE `hoj`.`announcement`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`announcement` ADD CONSTRAINT `announcement_ibfk_2` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;


	ALTER TABLE `hoj`.`contest`  ADD COLUMN `is_group` tinyint(1) DEFAULT '0';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`contest` ADD CONSTRAINT `contest_ibfk_2` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	ALTER TABLE `hoj`.`judge`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`judge` ADD CONSTRAINT `judge_ibfk_4` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;


	ALTER TABLE `hoj`.`discussion`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`discussion` ADD CONSTRAINT `discussion_ibfk_3` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	ALTER TABLE `hoj`.`file`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`file` ADD  CONSTRAINT `file_ibfk_2` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

	ALTER TABLE `hoj`.`problem`  ADD COLUMN `is_group` tinyint(1) DEFAULT '0';
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`problem` ADD CONSTRAINT `problem_ibfk_2` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	ALTER TABLE `hoj`.`tag`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`tag` ADD CONSTRAINT `tag_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;


	ALTER TABLE `hoj`.`training`  ADD COLUMN `is_group` tinyint(1) DEFAULT '0';
	ALTER TABLE `hoj`.`training`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`training` ADD CONSTRAINT `training_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	ALTER TABLE `hoj`.`training_category`  ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`training_category` ADD CONSTRAINT `training_category_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

	insert  into `auth`(`id`,`name`,`permission`,`status`,`gmt_create`,`gmt_modified`) values (13,'group','group_add',0,'2022-03-11 13:36:55','2022-03-11 13:36:55'),
	(14,'group','group_del',0,'2022-03-11 13:36:55','2022-03-11 13:36:55');

	insert  into `role_auth`(`auth_id`,`role_id`,`gmt_create`,`gmt_modified`) values (13,1000,'2021-06-12 23:16:58','2021-06-12 23:16:58'),(13,1001,'2021-06-12 23:16:58','2021-06-12 23:16:58'),
	(13,1002,'2021-06-12 23:16:58','2021-06-12 23:16:58'),(13,1008,'2021-06-12 23:16:58','2021-06-12 23:16:58'),(14,1000,'2021-06-12 23:16:58','2021-06-12 23:16:58'),
	(14,1001,'2021-06-12 23:16:58','2021-06-12 23:16:58'),(14,1002,'2021-06-12 23:16:58','2021-06-12 23:16:58'),(14,1008,'2021-06-12 23:16:58','2021-06-12 23:16:58');

END
IF ; END$$

DELIMITER ;
CALL add_group ;

DROP PROCEDURE add_group;



/*
* 2022.04.13 problem表增加apply_public_progress

*/
DROP PROCEDURE
IF EXISTS problem_Add_apply_public_progress;
DELIMITER $$

CREATE PROCEDURE problem_Add_apply_public_progress ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'apply_public_progress'
) THEN
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `apply_public_progress` int(11) DEFAULT NULL COMMENT '申请公开的进度：null为未申请，1为申请中，2为申请通过，3为申请拒绝';
END
IF ; END$$

DELIMITER ;
CALL problem_Add_apply_public_progress ;

DROP PROCEDURE problem_Add_apply_public_progress;



/*
* 2022.06.26 给指定表的字段修改字符集为utf8mb4

*/
DROP PROCEDURE
IF EXISTS table_Change_utf8mb4;
DELIMITER $$

CREATE PROCEDURE table_Change_utf8mb4 ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'announcement'
	AND column_name = 'title' AND CHARACTER_SET_NAME = 'utf8mb4'
) THEN
	ALTER TABLE hoj.announcement MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.announcement MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.admin_sys_notice MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.admin_sys_notice MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.contest MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.contest MODIFY COLUMN `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.contest_explanation MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.contest_problem MODIFY COLUMN `display_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.contest_print MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.discussion MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.discussion MODIFY COLUMN `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.discussion MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.discussion_report MODIFY COLUMN `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.group MODIFY COLUMN `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.group_member MODIFY COLUMN `reason` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.language MODIFY COLUMN `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.msg_remind MODIFY COLUMN `source_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `source` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `input` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `output` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `hint` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `spj_code` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.problem MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.reply MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.training MODIFY COLUMN `description` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.training MODIFY COLUMN `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.user_info MODIFY COLUMN `signature` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.judge MODIFY COLUMN `code` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	ALTER TABLE hoj.comment MODIFY COLUMN `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

END
IF ; END$$

DELIMITER ;
CALL table_Change_utf8mb4 ;

DROP PROCEDURE table_Change_utf8mb4;


/*
* 2022.08.05 题目标签添加分类

*/
DROP PROCEDURE
IF EXISTS problem_tag_Add_classification;
DELIMITER $$

CREATE PROCEDURE problem_tag_Add_classification ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'tag'
	AND column_name = 'tcid'
) THEN
	CREATE TABLE `tag_classification`  (
	  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
	  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签分类名称',
	  `oj` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签分类所属oj',
	  `gmt_create` datetime NULL DEFAULT NULL,
	  `gmt_modified` datetime NULL DEFAULT NULL,
	  `rank` int(10) UNSIGNED ZEROFILL NULL DEFAULT NULL COMMENT '标签分类优先级 越小越高',
	  PRIMARY KEY (`id`) USING BTREE
	) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

	ALTER TABLE `hoj`.`tag`  ADD COLUMN `tcid` bigint(20) unsigned DEFAULT NULL;
	ALTER TABLE `hoj`.`tag` ADD CONSTRAINT `tag_ibfk_2` FOREIGN KEY (`tcid`) REFERENCES `tag_classification` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

END
IF ; END$$

DELIMITER ;
CALL problem_tag_Add_classification ;

DROP PROCEDURE problem_tag_Add_classification;


/*
* 2022.08.21 提交评测增加人工评测标记

*/
DROP PROCEDURE
IF EXISTS judge_tag_Add_is_manual;
DELIMITER $$

CREATE PROCEDURE judge_tag_Add_is_manual ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'is_manual'
) THEN

	ALTER TABLE `hoj`.`judge`  ADD COLUMN `is_manual` tinyint(1) DEFAULT '0' COMMENT '是否为人工评测';

END
IF ; END$$

DELIMITER ;
CALL judge_tag_Add_is_manual ;

DROP PROCEDURE judge_tag_Add_is_manual;

/*
* 2022.08.30 OI题目增加subtask

*/
DROP PROCEDURE
IF EXISTS add_Problem_Subtask;
DELIMITER $$

CREATE PROCEDURE add_Problem_Subtask ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'judge_case_mode'
) THEN

	ALTER TABLE `hoj`.`problem`  ADD COLUMN `judge_case_mode` varchar(255) DEFAULT 'default' COMMENT '题目样例评测模式,default,subtask_lowest,subtask_average';
	ALTER TABLE `hoj`.`problem_case`  ADD COLUMN `group_num` int(11) DEFAULT '1' COMMENT 'subtask分组的编号';
	ALTER TABLE `hoj`.`judge_case`  ADD COLUMN `group_num` int(11) DEFAULT NULL COMMENT 'subtask分组的组号';
	ALTER TABLE `hoj`.`judge_case`  ADD COLUMN `seq` int(11) DEFAULT NULL COMMENT '排序';
	ALTER TABLE `hoj`.`judge_case`  ADD COLUMN `mode` varchar(255) DEFAULT 'default' COMMENT 'default,subtask_lowest,subtask_average';

END
IF ; END$$

DELIMITER ;
CALL add_Problem_Subtask ;

DROP PROCEDURE add_Problem_Subtask;



/*
* 2022.10.02  比赛增加奖项排名显示

*/
DROP PROCEDURE
IF EXISTS add_Contest_Award;
DELIMITER $$

CREATE PROCEDURE add_Contest_Award ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'award_type'
) THEN

	ALTER TABLE `hoj`.`contest`  ADD COLUMN `award_type` int(11) DEFAULT '0' COMMENT '奖项类型：0(不设置),1(设置占比),2(设置人数)';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `award_config` text DEFAULT NULL COMMENT '奖项配置 json';

END
IF ; END$$

DELIMITER ;
CALL add_Contest_Award ;

DROP PROCEDURE add_Contest_Award;

/*
* 2022.11.23  调整增加C++语言

*/
DROP PROCEDURE
IF EXISTS add_Language_Change;
DELIMITER $$

CREATE PROCEDURE add_Language_Change ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'language'
	AND column_name = 'seq'
) THEN

	ALTER TABLE `hoj`.`language`  ADD COLUMN `seq` int(11) DEFAULT '0' COMMENT '语言排序';
	UPDATE `language` set `seq` = 5, `description` = 'G++ 9.4.0' WHERE (`name` = 'C++ With O2' OR `name` = 'C++') AND `oj`='ME';
	UPDATE `language` set `seq` = 10, `description` = 'GCC 9.4.0' WHERE (`name` = 'C With O2' OR `name` = 'C') AND `oj`='ME';
	UPDATE `language` set `description` = 'Golang 1.19' WHERE `name`='Golang' AND `oj`='ME';
	INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 17', '/usr/bin/g++ -DONLINE_JUDGE -w -fmax-errors=1 -std=c++17 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'ME', 5);
	INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 17 With O2', '/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=1 -std=c++17 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'ME', 5);
	INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 20', '/usr/bin/g++ -DONLINE_JUDGE -w -fmax-errors=1 -std=c++20 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'ME', 5);
	INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 20 With O2', '/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=1 -std=c++20 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'ME', 5);

END
IF ; END$$

DELIMITER ;
CALL add_Language_Change ;

DROP PROCEDURE add_Language_Change;


/*
* 2023.05.01  增加读写模式 支持文件IO

*/
DROP PROCEDURE
IF EXISTS add_Problem_FileIO;
DELIMITER $$

CREATE PROCEDURE add_Problem_FileIO ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'is_file_io'
) THEN

	ALTER TABLE `hoj`.`problem`  ADD COLUMN `is_file_io` tinyint(1) DEFAULT '0' COMMENT '是否是file io自定义输入输出文件模式';
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `io_read_file_name` VARCHAR(255) NULL COMMENT '题目指定的file io输入文件的名称';
	ALTER TABLE `hoj`.`problem`  ADD COLUMN `io_write_file_name` VARCHAR(255) NULL COMMENT '题目指定的file io输出文件的名称';
END
IF ; END$$

DELIMITER ;
CALL add_Problem_FileIO ;

DROP PROCEDURE add_Problem_FileIO;


/*
* 2023.06.10  增加允许比赛结束后进行交题的开关

*/
DROP PROCEDURE
IF EXISTS add_Contest_allow_end_submit;
DELIMITER $$

CREATE PROCEDURE add_Contest_allow_end_submit ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'allow_end_submit'
) THEN

	ALTER TABLE `hoj`.`contest`  ADD COLUMN `allow_end_submit` tinyint(1) DEFAULT '0' COMMENT '是否允许比赛结束后进行提交';
END
IF ; END$$

DELIMITER ;
CALL add_Contest_allow_end_submit ;

DROP PROCEDURE add_Contest_allow_end_submit;

/*
* 增加首页轮播图跳转

*/
DROP PROCEDURE
IF EXISTS add_Home_Link;
DELIMITER $$

CREATE PROCEDURE add_Home_Link ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'file'
	AND column_name = 'link'
) THEN

	ALTER TABLE `hoj`.`file`  ADD COLUMN `link` varchar(255) DEFAULT NULL comment '图片对应的跳转链接';
	ALTER TABLE `hoj`.`file`  ADD COLUMN `hint` varchar(255) DEFAULT NULL comment '图片对应的文字描述';
END
IF ; END$$

DELIMITER ;
CALL add_Home_Link ;

DROP PROCEDURE add_Home_Link;

/*
* 增加用户的个人偏好设置表

*/
DROP PROCEDURE
IF EXISTS add_User_Info_Preferences_Setting;
DELIMITER $$

CREATE PROCEDURE add_User_Info_Preferences_Setting ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_preferences'
)THEN

	CREATE TABLE `user_preferences` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `uid` varchar(32) NOT NULL COMMENT '用户id',
	  `ui_language` varchar(255) DEFAULT NULL COMMENT '界面语言',
	  `ui_theme` varchar(255) DEFAULT NULL COMMENT '界面风格',
	  `code_language` varchar(255) DEFAULT NULL COMMENT '代码语言',
	  `code_size` varchar(255) DEFAULT NULL COMMENT '字体大小',
	  `ide_theme` varchar(255) DEFAULT NULL COMMENT '编译器主题',
	  `code_template` longtext DEFAULT NULL COMMENT '个人代码模板',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`,`uid`),
	  KEY `uid` (`uid`),
	  CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_User_Info_Preferences_Setting;

DROP PROCEDURE add_User_Info_Preferences_Setting;

/*
* 修改管理员权限

*/
insert  into `role_auth`(`auth_id`,`role_id`) values (4,1001),(5,1001),(4,1008);

/*
* 增加文件柜

*/
DROP PROCEDURE
IF EXISTS add_BoxFile;
DELIMITER $$

CREATE PROCEDURE add_BoxFile ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'open_file'
) THEN

	ALTER TABLE `hoj`.`contest`  ADD COLUMN `open_file` tinyint(1) DEFAULT '0' COMMENT '是否打开文件柜';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `file_config` text DEFAULT NULL COMMENT '文件柜配置 json';

END
IF ; END$$

DELIMITER ;
CALL add_BoxFile ;

DROP PROCEDURE add_BoxFile;

/*
* 增加同步赛

*/
DROP PROCEDURE
IF EXISTS add_Synchronous;
DELIMITER $$

CREATE PROCEDURE add_Synchronous ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'synchronous_config'
) THEN

	ALTER TABLE `hoj`.`contest`  ADD COLUMN `synchronous_config` text null comment '同步赛配置 json';
END
IF ; END$$

DELIMITER ;
CALL add_Synchronous ;

DROP PROCEDURE add_Synchronous;


/*
* 增加 SCPC 远程评测

*/
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 17', '/usr/bin/g++ -DONLINE_JUDGE -w -fmax-errors=1 -std=c++17 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC', 5);
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 17 With O2', '/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=1 -std=c++17 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC', 5);
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 20', '/usr/bin/g++ -DONLINE_JUDGE -w -fmax-errors=1 -std=c++20 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC', 5);
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`, `seq`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ 20 With O2', '/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=1 -std=c++20 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC', 5);
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ('text/x-csrc', 'GCC 9.4.0', 'C', '/usr/bin/gcc -DONLINE_JUDGE -w -fmax-errors=3 -std=c11 {src_path} -lm -o {exe_path}', '#include <stdio.h>\r\nint main() {\r\n    int a,b;\r\n    scanf(\"%d %d\",&a,&b);\r\n    printf(\"%d\",a+b);\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <stdio.h>\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  printf(\"%d\", add(1, 2));\r\n  return 0;\r\n}\r\n//APPEND END', 1, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ('text/x-csrc', 'GCC 9.4.0', 'C With O2', '/usr/bin/gcc -DONLINE_JUDGE -O2 -w -fmax-errors=3 -std=c11 {src_path} -lm -o {exe_path}', '#include <stdio.h>\r\nint main() {\r\n    int a,b;\r\n    scanf(\"%d %d\",&a,&b);\r\n    printf(\"%d\",a+b);\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <stdio.h>\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  printf(\"%d\", add(1, 2));\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++', '/usr/bin/g++ -DONLINE_JUDGE -w -fmax-errors=3 -std=c++14 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 1, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ('text/x-c++src', 'G++ 9.4.0', 'C++ With O2', '/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=3 -std=c++14 {src_path} -lm -o {exe_path}', '#include<iostream>\r\nusing namespace std;\r\nint main()\r\n{\r\n    int a,b;\r\n    cin >> a >> b;\r\n    cout << a + b;\r\n    return 0;\r\n}', '//PREPEND BEGIN\r\n#include <iostream>\r\nusing namespace std;\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\nint add(int a, int b) {\r\n  // Please fill this blank\r\n  return ___________;\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nint main() {\r\n  cout << add(1, 2);\r\n  return 0;\r\n}\r\n//APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ('text/x-java', 'OpenJDK 1.8', 'Java', '/usr/bin/javac {src_path} -d {exe_dir} -encoding UTF8', 'import java.util.Scanner;\r\npublic class Main{\r\n    public static void main(String[] args){\r\n        Scanner in=new Scanner(System.in);\r\n        int a=in.nextInt();\r\n        int b=in.nextInt();\r\n        System.out.println((a+b));\r\n    }\r\n}', '//PREPEND BEGIN\r\nimport java.util.Scanner;\r\n//PREPEND END\r\n\r\npublic class Main{\r\n    //TEMPLATE BEGIN\r\n    public static Integer add(int a,int b){\r\n        return _______;\r\n    }\r\n    //TEMPLATE END\r\n\r\n    //APPEND BEGIN\r\n    public static void main(String[] args){\r\n        System.out.println(add(a,b));\r\n    }\r\n    //APPEND END\r\n}\r\n', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-python', 'Python 3.7.5', 'Python3', '/usr/bin/python3 -m py_compile {src_path}', 'a, b = map(int, input().split())\r\nprint(a + b)', '//PREPEND BEGIN\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\ndef add(a, b):\r\n    return a + b\r\n//TEMPLATE END\r\n\r\n\r\nif __name__ == \'__main__\':  \r\n    //APPEND BEGIN\r\n    a, b = 1, 1\r\n    print(add(a, b))\r\n    //APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-python', 'Python 2.7.17', 'Python2', '/usr/bin/python -m py_compile {src_path}', 'a, b = map(int, raw_input().split())\r\nprint a+b', '//PREPEND BEGIN\r\n//PREPEND END\r\n\r\n//TEMPLATE BEGIN\r\ndef add(a, b):\r\n    return a + b\r\n//TEMPLATE END\r\n\r\n\r\nif __name__ == \'__main__\':  \r\n    //APPEND BEGIN\r\n    a, b = 1, 1\r\n    print add(a, b)\r\n    //APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-go', 'Golang 1.19', 'Golang', '/usr/bin/go build -o {exe_path} {src_path}', 'package main\r\nimport \"fmt\"\r\n\r\nfunc main(){\r\n    var x int\r\n    var y int\r\n    fmt.Scanln(&x,&y)\r\n    fmt.Printf(\"%d\",x+y)  \r\n}\r\n', '\r\npackage main\r\n\r\n//PREPEND BEGIN\r\nimport \"fmt\"\r\n//PREPEND END\r\n\r\n\r\n//TEMPLATE BEGIN\r\nfunc add(a,b int)int{\r\n    return ______\r\n}\r\n//TEMPLATE END\r\n\r\n//APPEND BEGIN\r\nfunc main(){\r\n    var x int\r\n    var y int\r\n    fmt.Printf(\"%d\",add(x,y))  \r\n}\r\n//APPEND END\r\n', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-csharp', 'C# Mono 4.6.2', 'C#', '/usr/bin/mcs -optimize+ -out:{exe_path} {src_path}', 'using System;\r\nusing System.Linq;\r\n\r\nclass Program {\r\n    public static void Main(string[] args) {\r\n        Console.WriteLine(Console.ReadLine().Split().Select(int.Parse).Sum());\r\n    }\r\n}', '//PREPEND BEGIN\r\nusing System;\r\nusing System.Collections.Generic;\r\nusing System.Text;\r\n//PREPEND END\r\n\r\nclass Solution\r\n{\r\n    //TEMPLATE BEGIN\r\n    static int add(int a,int b){\r\n        return _______;\r\n    }\r\n    //TEMPLATE END\r\n\r\n    //APPEND BEGIN\r\n    static void Main(string[] args)\r\n    {\r\n        int a ;\r\n        int b ;\r\n        Console.WriteLine(add(a,b));\r\n    }\r\n    //APPEND END\r\n}', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-php', 'PHP 7.3.33', 'PHP', '/usr/bin/php {src_path}', '<?=array_sum(fscanf(STDIN, \"%d %d\"));', NULL, 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-python', 'PyPy 2.7.18 (7.3.8)', 'PyPy2', '/usr/bin/pypy -m py_compile {src_path}', 'print sum(int(x) for x in raw_input().split(\' \'))', '//PREPEND BEGIN\n//PREPEND END\n\n//TEMPLATE BEGIN\ndef add(a, b):\n    return a + b\n//TEMPLATE END\n\n\nif __name__ == \'__main__\':  \n    //APPEND BEGIN\n    a, b = 1, 1\n    print add(a, b)\n    //APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/x-python', 'PyPy 3.8.12 (7.3.8)', 'PyPy3', '/usr/bin/pypy3 -m py_compile {src_path}', 'print(sum(int(x) for x in input().split(\' \')))', '//PREPEND BEGIN\n//PREPEND END\n\n//TEMPLATE BEGIN\ndef add(a, b):\n    return a + b\n//TEMPLATE END\n\n\nif __name__ == \'__main__\':  \n    //APPEND BEGIN\n    a, b = 1, 1\n    print(add(a, b))\n    //APPEND END', 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/javascript', 'Node.js 14.19.0', 'JavaScript Node', '/usr/bin/node {src_path}', 'var readline = require(\'readline\');\nconst rl = readline.createInterface({\n        input: process.stdin,\n        output: process.stdout\n});\nrl.on(\'line\', function(line){\n   var tokens = line.split(\' \');\n    console.log(parseInt(tokens[0]) + parseInt(tokens[1]));\n});', NULL, 0, 'SCPC');
INSERT INTO `language`(`content_type`,`description`,`name`,`compile_command`,`template`, `code_template`, `is_spj`, `oj`) VALUES ( 'text/javascript', 'JavaScript V8 8.4.109', 'JavaScript V8', '/usr/bin/jsv8/d8 {src_path}', 'const [a, b] = readline().split(\' \').map(n => parseInt(n, 10));\nprint((a + b).toString());', NULL, 0, 'SCPC');


/*
* 增加正式赛

*/
DROP PROCEDURE
IF EXISTS add_Official;
DELIMITER $$

CREATE PROCEDURE add_Official ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'sign_start_time'
) THEN

	ALTER TABLE `hoj`.`contest`  ADD COLUMN `sign_start_time` datetime NULL DEFAULT NULL comment '报名开始时间';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `sign_end_time` datetime NULL DEFAULT NULL comment '报名结束时间';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `sign_duration`  bigint(20) DEFAULT NULL comment '报名时长(s)';
	ALTER TABLE `hoj`.`contest`  ADD COLUMN `max_participants` int(11) DEFAULT NULL comment '队员上限(最大为3)';

END
IF ; END$$

DELIMITER ;
CALL add_Official ;

DROP PROCEDURE add_Official;

/*
* 增加正式赛报名表

*/
DROP PROCEDURE
IF EXISTS contest_Add_sign;
DELIMITER $$

CREATE PROCEDURE contest_Add_sign ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_sign'
) THEN

	CREATE TABLE `user_sign` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `uid` varchar(32) NOT NULL COMMENT '用户id',
	  `username` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
  	  `realname` varchar(50) DEFAULT NULL COMMENT '真实姓名',
	  `school` varchar(50) DEFAULT NULL COMMENT '学校',
	  `course` varchar(50) DEFAULT NULL COMMENT '专业/班级',
	  `number` varchar(50) DEFAULT NULL COMMENT '学号',
	  `clothes_size` varchar(10) DEFAULT NULL COMMENT '衣服尺寸',
	  `phone_number` varchar(20) DEFAULT NULL COMMENT '联系方式',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`,`uid`),
	  KEY `uid` (`uid`),
	  CONSTRAINT `user_sign_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

END
IF ; END$$

DELIMITER ;
CALL contest_Add_sign ;

DROP PROCEDURE contest_Add_sign;

/*
* 增加比赛的报名表

*/
DROP PROCEDURE
IF EXISTS Add_contest_sign;
DELIMITER $$

CREATE PROCEDURE Add_contest_sign ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_sign'
) THEN
	CREATE TABLE `contest_sign` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `cid` bigint(20) unsigned DEFAULT NULL COMMENT '比赛id',
	  `cname` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '队伍中文名称',
	  `ename` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '队伍英文名称',
      `school` varchar(100) DEFAULT NULL COMMENT '学校',
	  `team_names` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '队员用户id',
	  `team_config` text DEFAULT NULL COMMENT '队伍信息',
	  `participants` int(11) DEFAULT NULL COMMENT '队伍人数',
	  `type` int(11) DEFAULT '0' COMMENT '报名类型（0为正式名额，1为打星名额）',
	  `status` int(11) DEFAULT '0' COMMENT '报名审核状态（0表示审核中，1为审核通过，2为审核不通过。）',
	  `gender` int(11) DEFAULT '0' COMMENT '报名类型（0为正式队伍，1为女生队伍）',
	  `msg` varchar(255) DEFAULT NULL COMMENT '审核不通过原因',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `cid` (`cid`),
	  CONSTRAINT `contest_sign_ibfk_1` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL Add_contest_sign ;

DROP PROCEDURE Add_contest_sign;

/*
* 批量转移用户信息

*/
INSERT INTO user_sign (`uid`, `username`, `realname`, `school`, `course`, `number`)
SELECT `uuid`, `username`, `realname`, `school`, `course`, `number`
FROM user_info;

/*
* 增加查重的查重表

*/
DROP PROCEDURE
IF EXISTS Add_contest_moss;
DELIMITER $$

CREATE PROCEDURE Add_contest_moss ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_moss'
) THEN
	CREATE TABLE `contest_moss` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `cid` bigint(20) unsigned DEFAULT NULL COMMENT '比赛id',
	  `html` varchar(255) DEFAULT NULL COMMENT '查重预览页',
	  `username1` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户名',
	  `uid1` varchar(32) DEFAULT NULL COMMENT '用户id',
	  `percent1` bigint(20) DEFAULT NULL COMMENT '重复片段占代码总长度百分比',
	  `username2` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户名',
	  `uid2` varchar(32) DEFAULT NULL COMMENT '用户id',
	  `percent2` bigint(20) DEFAULT NULL COMMENT '重复片段占代码总长度百分比',
	  `length` bigint DEFAULT NULL COMMENT '重复片段长度',
	  `href` varchar(255) DEFAULT NULL COMMENT '查重详情页',
	  `language` varchar(255) DEFAULT NULL COMMENT '查重语言',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `cid` (`cid`),
	  CONSTRAINT `contest_moss_ibfk_1` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL Add_contest_moss ;

DROP PROCEDURE Add_contest_moss;


/*
* 增加查重的结果表

*/
DROP PROCEDURE
IF EXISTS Add_contest_moss_result;
DELIMITER $$

CREATE PROCEDURE Add_contest_moss_result ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_moss_result'
) THEN
	CREATE TABLE `contest_moss_result` (
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `cid` bigint(20) unsigned DEFAULT NULL COMMENT '比赛id',
	  `href` varchar(255) DEFAULT NULL COMMENT '查重详情页',
	  `col1` varchar(255) DEFAULT NULL COMMENT '重复片段行数位置列表',
	  `icon1` longtext DEFAULT NULL COMMENT '重复率按键列表',
	  `code1` longtext DEFAULT NULL COMMENT '代码',
	  `col2` varchar(255) DEFAULT NULL COMMENT '重复片段行数位置列表',
	  `icon2` longtext DEFAULT NULL COMMENT '重复率按键列表',
	  `code2` longtext DEFAULT NULL COMMENT '代码',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `cid` (`cid`),
	  CONSTRAINT `contest_moss_result_ibfk_1` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL Add_contest_moss_result ;

DROP PROCEDURE Add_contest_moss_result;

/*
* 增加账号列表为非空

*/
ALTER TABLE remote_judge_account MODIFY COLUMN password VARCHAR(255) NULL COMMENT '密码';


/*
* 用户名, 邮箱区分大小写

*/
ALTER TABLE contest_print
DROP FOREIGN KEY contest_print_ibfk_2;
ALTER TABLE discussion_report
DROP FOREIGN KEY discussion_report_ibfk_2;
ALTER TABLE judge
DROP FOREIGN KEY judge_ibfk_3;
ALTER TABLE problem
DROP FOREIGN KEY problem_ibfk_1;

ALTER TABLE user_info
MODIFY COLUMN username VARCHAR(100) COLLATE utf8mb4_bin NOT NULL COMMENT '用户名';

ALTER TABLE user_info
MODIFY COLUMN email varchar(320) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '邮箱';

-- 示例：调整 contest_print 表的字符集
ALTER TABLE contest_print
MODIFY COLUMN username VARCHAR(100) COLLATE utf8mb4_bin NOT NULL;
-- 添加外键约束，确保排序规则一致
ALTER TABLE contest_print
ADD CONSTRAINT contest_print_ibfk_2
FOREIGN KEY (`username`)
REFERENCES `user_info` (username) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE discussion_report
MODIFY COLUMN reporter VARCHAR(100) COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE discussion_report
ADD CONSTRAINT discussion_report_ibfk_2
FOREIGN KEY (`reporter`)
REFERENCES `user_info` (username) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE judge
MODIFY COLUMN username VARCHAR(100) COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE judge
ADD CONSTRAINT judge_ibfk_3
FOREIGN KEY (`username`)
REFERENCES `user_info` (username) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE problem
MODIFY COLUMN author VARCHAR(100) COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE problem
ADD CONSTRAINT problem_ibfk_1
FOREIGN KEY (`author`)
REFERENCES `user_info` (username) ON DELETE CASCADE ON UPDATE CASCADE;

/*
* 增加 judge 表的 sorted_id

*/
DROP PROCEDURE
IF EXISTS add_Judge_SortedId;
DELIMITER $$

CREATE PROCEDURE add_Judge_SortedId ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'sorted_id'
) THEN

	ALTER TABLE `hoj`.`judge`  ADD COLUMN `sorted_id` bigint unsigned COMMENT '排序后的submit_id';

END
IF ; END$$

DELIMITER ;
CALL add_Judge_SortedId ;

DROP PROCEDURE add_Judge_SortedId;

/*
* 增加 judge 表的 sorted_id 列数据

*/
	UPDATE judge
JOIN (
    SELECT submit_id, ROW_NUMBER() OVER (ORDER BY submit_time) AS row_num
    FROM judge
) AS subquery
ON judge.submit_id = subquery.submit_id
SET judge.sorted_id = subquery.row_num
WHERE judge.submit_id = subquery.submit_id;

/*
* 增加 judge 表的 索引

*/

DROP PROCEDURE
IF EXISTS add_Judge_Index;
DELIMITER $$

CREATE PROCEDURE add_Judge_Index ()
BEGIN
	CREATE INDEX cid_gid_pid_status ON judge(`cid`,`gid`,`pid`,`status`);
	CREATE INDEX cid_gid_pid_uid_oiRankScore ON judge(`cid`, `gid`, `pid`, `uid`, `oi_rank_score`);
	CREATE INDEX sorted_id ON `hoj`.`judge` (`sorted_id` DESC);
	CREATE INDEX display_pid ON `hoj`.`judge` (`display_pid`);
END$$
DELIMITER ;
CALL add_Judge_Index ;

DROP PROCEDURE add_Judge_Index;

/*
* 增加 user_acproblem 表的 索引

*/
DROP PROCEDURE
IF EXISTS add_UserAcproblem_uid_pid_gmtCreateIndex;
DELIMITER $$
CREATE PROCEDURE add_UserAcproblem_uid_pid_gmtCreateIndex ()
BEGIN
	CREATE INDEX uid_pid_gmtCreate ON `hoj`.`user_acproblem` (`uid`, `pid`, `gmt_create` DESC);
END$$
DELIMITER ;
CALL add_UserAcproblem_uid_pid_gmtCreateIndex ;

DROP PROCEDURE add_UserAcproblem_uid_pid_gmtCreateIndex;


/*
* 增加 judge_case 表的 input_content

*/
DROP PROCEDURE
IF EXISTS add_JudgeCase_Content;
DELIMITER $$

CREATE PROCEDURE add_JudgeCase_Content ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge_case'
	AND column_name = 'input_content'
) THEN

	ALTER TABLE `hoj`.`judge_case`  ADD COLUMN `input_content` longtext COMMENT '样例输入';
	ALTER TABLE `hoj`.`judge_case`  ADD COLUMN `output_content` longtext COMMENT '样例输出';

END
IF ; END$$

DELIMITER ;
CALL add_JudgeCase_Content;

DROP PROCEDURE add_JudgeCase_Content;

/*
* 添加用户 OJ 信息表

*/
DROP PROCEDURE
IF EXISTS add_user_remoteOj;
DELIMITER $$

CREATE PROCEDURE add_user_remoteOj ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_multi_oj'
) THEN
	CREATE TABLE `user_multi_oj` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `uid` varchar(32) NOT NULL COMMENT '用户id',
	  `username` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
      `codeforces` varchar(255) DEFAULT NULL COMMENT 'codeforces的username',
      `nowcoder` varchar(255) DEFAULT NULL COMMENT 'nowcoder的username',
      `vjudge` varchar(255) DEFAULT NULL COMMENT 'vjudge的username',
      `poj` varchar(255) DEFAULT NULL COMMENT 'poj的username',
      `atcode` varchar(255) DEFAULT NULL COMMENT 'atcode的username',
      `leetcode` varchar(255) DEFAULT NULL COMMENT 'leetcode的username',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`,`uid`),
	  KEY `uid` (`uid`),
	  CONSTRAINT `user_multi_oj_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_user_remoteOj ;

DROP PROCEDURE add_user_remoteOj;

/*
* 批量转移 oj 信息

*/
INSERT INTO user_multi_oj (`uid`, `username`, `codeforces`)
SELECT `uuid`, `username`, `cf_username`
FROM user_info;


/*
* 增加 user_record 表的内容

*/
DROP PROCEDURE
IF EXISTS add_userRecord;
DELIMITER $$

CREATE PROCEDURE add_userRecord ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_record'
	AND column_name = 'codeforces_rating'
) THEN

	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `codeforces_rating` int COMMENT 'codeforces 分数';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `codeforces_max_rating` int COMMENT 'codeforces 最大分数';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `nowcoder_rating` int COMMENT 'nowcoder 分数';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `codeforces_ac` int COMMENT 'codeforces AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `nowcoder_ac` int COMMENT 'nowcoder AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `vjudge_ac` int COMMENT 'vjudge AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `poj_ac` int COMMENT 'poj AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `atcode_ac` int COMMENT 'atcode AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `leetcode_ac` int COMMENT 'leetcode AC';
	ALTER TABLE `hoj`.`user_record`  ADD COLUMN `see` BOOLEAN DEFAULT 0  NULL  COMMENT '是否展示';

END
IF ; END$$

DELIMITER ;
CALL add_userRecord ;

DROP PROCEDURE add_userRecord;


/*
* 更新 vjudge_submit_id 为 String
*/
DROP PROCEDURE IF EXISTS judge_Update_vjudge_submit_id;
DELIMITER $$

CREATE PROCEDURE judge_Update_vjudge_submit_id ()
BEGIN
    ALTER TABLE judge MODIFY COLUMN vjudge_submit_id VARCHAR(255) NULL COMMENT 'vjudge判题在其它oj的提交id';
END$$

DELIMITER ;
CALL judge_Update_vjudge_submit_id ;

/*
 * 增加 role 表的 contest_account 比赛账号

 */
INSERT INTO `role`(`id`, `role`, `description`, `status`) VALUES ('1009', 'contest_account', '比赛账号', 0);
INSERT INTO `role`(`id`, `role`, `description`, `status`) VALUES ('1010', 'team_contest_account', '组队比赛账号', 0);

/*
 * 增加 role_auth 表的 比赛账号 权限

 */
INSERT INTO `role_auth`(`auth_id`, `role_id`) VALUES (2, 1009),(8, 1009),(9, 1009),(10, 1009),(11, 1009),(12, 1009),(13, 1009),(14, 1009);
INSERT INTO `role_auth`(`auth_id`, `role_id`) VALUES (2, 1010),(8, 1010),(9, 1010),(10, 1010),(11, 1010),(12, 1010),(13, 1010),(14, 1010);

/*
* 添加 school 表

*/
DROP PROCEDURE
IF EXISTS add_school;
DELIMITER $$

CREATE PROCEDURE add_school ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'school'
) THEN
	CREATE TABLE `school` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `province` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '省份',
	  `city` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '城市',
      `name` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '学校名称',
      `short_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '学校简称',
      `file_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  KEY `id` (`id`)
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_school ;

DROP PROCEDURE add_school;

/*
* 添加 examination_room 表

*/
DROP PROCEDURE
IF EXISTS add_examination_room;
DELIMITER $$

CREATE PROCEDURE add_examination_room ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'examination_room'
) THEN
	CREATE TABLE `examination_room` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `school_id` bigint unsigned NOT NULL COMMENT '学校id',
	  `building` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '建筑号',
      `room` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '房间号',
      `max_row` int DEFAULT '7' COMMENT '最大行数，默认从0到n-1',
      `max_col` int DEFAULT '7' COMMENT '最大列数，默认从0到n-1',
      `status` int(11) DEFAULT '0' COMMENT '教室是否废弃',
	  `author` varchar(255) NOT NULL COMMENT '发表者用户名',
	  `modified_user` varchar(255) DEFAULT NULL COMMENT '修改者的用户名',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`,`school_id`),
	  KEY `school_id` (`school_id`),
	  CONSTRAINT `examination_room_ibfk_1` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_examination_room ;

DROP PROCEDURE add_examination_room;

/*
* 添加 examination_seat 考场布局表

*/
DROP PROCEDURE
IF EXISTS add_examination_seat;
DELIMITER $$

CREATE PROCEDURE add_examination_seat ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'examination_seat'
) THEN
	CREATE TABLE `examination_seat` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `eid` bigint unsigned NOT NULL COMMENT '考场编号',
	  `grow` int DEFAULT NULL COMMENT '座位的x轴',
      `gcol` int DEFAULT NULL COMMENT '座位的y轴',
      `type` int(11) DEFAULT NULL COMMENT '0为可选座位，3为维修座位，4为该地方无座位',
      `status` int(11) DEFAULT '0',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`, `eid`),
	  KEY `eid` (`eid`),
	  CONSTRAINT `examination_seat_ibfk_1` FOREIGN KEY (`eid`) REFERENCES `examination_room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_examination_seat ;

DROP PROCEDURE add_examination_seat;

/*
* 添加 examination_seat 比赛座位表

*/
DROP PROCEDURE
IF EXISTS add_contest_seat;
DELIMITER $$

CREATE PROCEDURE add_contest_seat ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_seat'
) THEN
	CREATE TABLE `contest_seat` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `uid` varchar(255) DEFAULT NULL COMMENT '用户编号',
      `cid` bigint unsigned NOT NULL COMMENT '比赛Id',
      `sid` bigint unsigned NOT NULL COMMENT '座位Id',
	  `sort_id` bigint unsigned DEFAULT NULL COMMENT '座位排序',
      `title` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '比赛标题',
	  `realname` varchar(100) COLLATE utf8mb4_bin NOT NULL COMMENT '考生姓名',
      `course` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '专业/班级',
      `number` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '学号',
      `subject` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '科目',
      `type` int(11) DEFAULT NULL COMMENT '0为可选座位，1为选中座位，2为已选座位',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`, `sid`),
	  KEY `sid` (`sid`),
	  CONSTRAINT `contest_seat_ibfk_1` FOREIGN KEY (`sid`) REFERENCES `examination_seat` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
	  KEY `cid` (`cid`),
	  CONSTRAINT `contest_seat_ibfk_2` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_contest_seat ;

DROP PROCEDURE add_contest_seat;


/*
* 比赛账号检测浏览器页面变化
*/
DROP PROCEDURE
IF EXISTS add_session_routName;
DELIMITER $$

CREATE PROCEDURE add_session_routName ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'session'
	AND column_name = 'route_name'
) THEN
	ALTER TABLE session ADD COLUMN route_name varchar(255) DEFAULT NULL COMMENT '浏览器页面';
END
IF ; END$$

DELIMITER ;
CALL add_session_routName ;

DROP PROCEDURE add_session_routName;


/*
* 比赛账号 IP 重置
*/
DROP PROCEDURE
IF EXISTS judge_tag_Add_is_reset;
DELIMITER $$

CREATE PROCEDURE judge_tag_Add_is_reset ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'is_reset'
) THEN

	ALTER TABLE `hoj`.`judge`  ADD COLUMN `is_reset` tinyint(1) DEFAULT null COMMENT '是否重置IP';

END
IF ; END$$

DELIMITER ;
CALL judge_tag_Add_is_reset ;

DROP PROCEDURE judge_tag_Add_is_reset;

/*
* 学校添加 name
*/
DROP PROCEDURE
IF EXISTS add_file_name;
DELIMITER $$

CREATE PROCEDURE add_file_name ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'school'
	AND column_name = 'file_name'
) THEN
	ALTER TABLE school ADD COLUMN `file_name` varchar(255) DEFAULT NULL COMMENT '文件名';
END
IF ; END$$

DELIMITER ;
CALL add_file_name ;

DROP PROCEDURE add_file_name;

/*
* user_info 添加 last_gmt_modified
*/
DROP PROCEDURE
IF EXISTS add_userInfo_lastGmtModified;
DELIMITER $$

CREATE PROCEDURE add_userInfo_lastGmtModified ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_info'
	AND column_name = 'last_gmt_modified'
) THEN
	ALTER TABLE user_info ADD COLUMN `last_gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上次修改用户名时间';
END
IF ; END$$

DELIMITER ;
CALL add_userInfo_lastGmtModified ;

DROP PROCEDURE add_userInfo_lastGmtModified;

/*
* 批量复制 user_info 中的 last_gmt_modified 为 gmt_create

*/
UPDATE user_info
SET last_gmt_modified = gmt_create
WHERE 1 = 1;

/*
* problem 添加 pdf_description
*/
DROP PROCEDURE
IF EXISTS add_Problem_pdfDescription;
DELIMITER $$

CREATE PROCEDURE add_Problem_pdfDescription ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'pdf_description'
) THEN
	ALTER TABLE problem ADD COLUMN `pdf_description` varchar(255) DEFAULT NULL COMMENT 'PDF链接';
END
IF ; END$$

DELIMITER ;
CALL add_Problem_pdfDescription ;

DROP PROCEDURE add_Problem_pdfDescription;

/*
* 添加 user_cloc 用户提交代码统计行数

*/
DROP PROCEDURE
IF EXISTS add_user_cloc;
DELIMITER $$

CREATE PROCEDURE add_user_cloc ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_cloc'
) THEN
	CREATE TABLE `user_cloc` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `uid` varchar(255) DEFAULT NULL COMMENT '用户编号',
	  `username` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
	  `realname` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '真实姓名',
	  `time` varchar(255) DEFAULT NULL COMMENT '日期',
	  `json` longtext DEFAULT NULL COMMENT '代码数据',
	  `sum`  bigint unsigned DEFAULT 0 COMMENT '代码量',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `uid` (`uid`),
	  KEY `username` (`username`),
	  CONSTRAINT `user_cloc_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
      CONSTRAINT `user_cloc_ibfk_2` FOREIGN KEY (`username`) REFERENCES `user_info` (`username`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_user_cloc ;

DROP PROCEDURE add_user_cloc;

/*
* 添加 Honor 统计获奖

*/
DROP PROCEDURE
IF EXISTS add_honor;
DELIMITER $$

CREATE PROCEDURE add_honor ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'honor'
) THEN
	CREATE TABLE `honor` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `title` varchar(255) NOT NULL COMMENT '荣誉名称',
	  `description` longtext COMMENT '荣誉简介',
	  `author` varchar(255) NOT NULL COMMENT '荣誉创建者用户名',
	  `type` varchar(255) NOT NULL COMMENT '荣誉权限类型：Gold、Silver、Bronze',
	  `level` varchar(255) DEFAULT NULL COMMENT '荣誉的等级（全球赛，国赛，省赛，校赛）',
	  `date` datetime NULL DEFAULT NULL comment '荣誉的时间',
	  `team_member` varchar(255) DEFAULT NULL COMMENT '荣誉的队员',
	  `link` varchar(255) DEFAULT NULL COMMENT '跳转链接',
	  `status` tinyint(1) DEFAULT '1' COMMENT '是否可用',
	  `is_group` tinyint(1) DEFAULT '0',
	  `gid` bigint(20) unsigned DEFAULT NULL,
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `gid` (`gid`),
	  CONSTRAINT `honor_ibfk_1` FOREIGN KEY (`gid`) REFERENCES `group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_honor ;

DROP PROCEDURE add_honor;

/*
* contest 添加 oj
*/
DROP PROCEDURE
IF EXISTS add_Contest_oj;
DELIMITER $$

CREATE PROCEDURE add_Contest_oj ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'oj'
) THEN
	ALTER TABLE contest ADD COLUMN `oj` varchar(50) DEFAULT NULL COMMENT '其他平台oj';
END
IF ; END$$

DELIMITER ;
CALL add_Contest_oj ;

DROP PROCEDURE add_Contest_oj;


/*
* 添加 statistic 记录系列比赛对应 cids

*/
DROP PROCEDURE
IF EXISTS add_StatisticContest;
DELIMITER $$

CREATE PROCEDURE add_StatisticContest ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'statistic_contest'
) THEN
	CREATE TABLE `statistic_contest` (
	  `scid` varchar(255) NOT NULL COMMENT '系列比赛id',
	  `title` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '系列比赛名称',
      `cids` longtext NOT NULL COMMENT '包含比赛的cids',
      `percents` longtext DEFAULT NULL COMMENT '包含比赛的比例',
	  `visible` tinyint(1) DEFAULT '1' COMMENT '是否可见',
	  `author` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '作者',
	  `account` varchar(255) COLLATE utf8mb4_bin COMMENT '爬取使用账号',
	  `data` longtext COLLATE utf8mb4_bin COMMENT '用户的字典',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`scid`)
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_StatisticContest ;

DROP PROCEDURE add_StatisticContest;

/*
* 添加 statistic_rank 系列比赛对应榜单信息

*/
DROP PROCEDURE
IF EXISTS add_StatisticRank;
DELIMITER $$

CREATE PROCEDURE add_StatisticRank ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'statistic_rank'
) THEN
	CREATE TABLE `statistic_rank` (
	  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
	  `scid` varchar(255) NOT NULL COMMENT '系列比赛id',
	  `uid` varchar(255) NOT NULL,
	  `username` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
	  `realname` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '真实姓名',
	  `school` varchar(100) DEFAULT NULL COMMENT '学校',
	  `ac` double DEFAULT NULL COMMENT 'ac题目数',
	  `total` int DEFAULT NULL COMMENT '总提交数',
	  `total_time` double DEFAULT NULL COMMENT '总用时',
      `rank` int DEFAULT NULL COMMENT '总排名',
	  `synchronous` tinyint(1) DEFAULT '1' COMMENT '是否为外网站数据',
      `json` longtext COLLATE utf8mb4_bin DEFAULT NULL COMMENT '比赛对应的提交信息',
	  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  KEY `scid` (`scid`),
	  CONSTRAINT `statistic_rank_ibfk_1` FOREIGN KEY (`scid`) REFERENCES `statistic_contest` (`scid`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
END
IF ; END$$

DELIMITER ;
CALL add_StatisticRank ;

DROP PROCEDURE add_StatisticRank;


/*
* 添加 Problem_description

*/
DROP PROCEDURE
IF EXISTS add_problemDescription;
DELIMITER $$

CREATE PROCEDURE add_problemDescription ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem_description'
) THEN
	CREATE TABLE `problem_description` (
	`id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	`pid` bigint(20) unsigned NOT NULL COMMENT '题目id',
	`title` varchar(255) COMMENT '题目',
	`description` longtext COMMENT '描述',
	`input` longtext COMMENT '输入描述',
	`output` longtext COMMENT '输出描述',
	`examples` longtext COMMENT '题面样例',
	`source` text COMMENT '题目来源',
	`hint` longtext COMMENT '备注,提醒',
	`rank` int DEFAULT '0' COMMENT '编号，升序',
	`author` varchar(255) DEFAULT NULL COMMENT '创建者用户名',
	`pdf_description` varchar(255) DEFAULT NULL COMMENT 'PDF链接',
	`html` longtext COMMENT '题面',
	`gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
	`gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	KEY `pid` (`pid`),
    CONSTRAINT `problem_description_ibfk_1` FOREIGN KEY (`pid`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
END
IF ; END$$

DELIMITER ;
CALL add_problemDescription ;

DROP PROCEDURE add_problemDescription;

/*
* 将Problem 中的description 转移到 problem_description

*/

INSERT INTO problem_description(`pid`, `title`, `author`, `description`, `input`, `output`, `examples`, `source`, `hint`, `pdf_description`, `gmt_create`, `gmt_modified`)
SELECT `id`, `title`, `author`, `description`, `input`, `output`, `examples`, `source`, `hint`, `pdf_description`, `gmt_create`, `gmt_modified` FROM problem;

/*
* 将Problem 中的题面元素去除

*/

DROP PROCEDURE
IF EXISTS problem_Delete_description;
DELIMITER $$

CREATE PROCEDURE problem_Delete_description ()
BEGIN

IF EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'title'
) THEN
    ALTER TABLE `hoj`.`problem` DROP COLUMN `title`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `description`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `input`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `output`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `examples`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `source`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `hint`;
    ALTER TABLE `hoj`.`problem` DROP COLUMN `pdf_description`;
END
IF ; END$$

DELIMITER ;
CALL problem_Delete_description ;

DROP PROCEDURE problem_Delete_description;

/*
* contest_problem 添加 peid
*/
DROP PROCEDURE
IF EXISTS add_ContestProblem_peid;
DELIMITER $$

CREATE PROCEDURE add_ContestProblem_peid ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_problem'
	AND column_name = 'peid'
) THEN
	ALTER TABLE contest_problem ADD COLUMN `peid` bigint(20) DEFAULT NULL COMMENT '题面id';
END
IF ; END$$

DELIMITER ;
CALL add_ContestProblem_peid ;

DROP PROCEDURE add_ContestProblem_peid;


/*
* training_problem 添加 peid
*/
DROP PROCEDURE
IF EXISTS add_TrainingProblem_peid;
DELIMITER $$

CREATE PROCEDURE add_TrainingProblem_peid ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'training_problem'
	AND column_name = 'peid'
) THEN
	ALTER TABLE training_problem ADD COLUMN `peid` bigint(20) DEFAULT NULL COMMENT '题面id';
END
IF ; END$$

DELIMITER ;
CALL add_TrainingProblem_peid ;

DROP PROCEDURE add_TrainingProblem_peid;

/*
* languge 添加 key
*/
DROP PROCEDURE
IF EXISTS add_Language_Key;
DELIMITER $$

CREATE PROCEDURE add_Language_Key ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'language'
	AND column_name = 'key'
) THEN
	ALTER TABLE language ADD COLUMN `key` varchar(255) DEFAULT NULL COMMENT '语言提交key';
END
IF ; END$$

DELIMITER ;
CALL add_Language_Key ;

DROP PROCEDURE add_Language_Key;


/*
* judge 添加 key
*/
DROP PROCEDURE
IF EXISTS add_Judge_Key;
DELIMITER $$

CREATE PROCEDURE add_Judge_Key ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'judge'
	AND column_name = 'key'
) THEN
	ALTER TABLE judge ADD COLUMN `key` varchar(255) DEFAULT NULL COMMENT '语言提交key';
END
IF ; END$$

DELIMITER ;
CALL add_Judge_Key ;

DROP PROCEDURE add_Judge_Key;



/*
* contest 添加 pdf_description
*/
DROP PROCEDURE
IF EXISTS add_Contest_PdfDescription;
DELIMITER $$

CREATE PROCEDURE add_Contest_PdfDescription ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'pdf_description'
) THEN
	ALTER TABLE contest ADD COLUMN `pdf_description` varchar(255) DEFAULT NULL COMMENT 'PDF链接';
END
IF ; END$$

DELIMITER ;
CALL add_Contest_PdfDescription ;

DROP PROCEDURE add_Contest_PdfDescription;


/*
* contest_problem 添加 pdf_description
*/
DROP PROCEDURE
IF EXISTS add_ContestProblem_PdfDescription;
DELIMITER $$

CREATE PROCEDURE add_ContestProblem_PdfDescription ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_problem'
	AND column_name = 'pdf_description'
) THEN
	ALTER TABLE contest_problem ADD COLUMN `pdf_description` varchar(255) DEFAULT NULL COMMENT 'PDF链接';
END
IF ; END$$

DELIMITER ;
CALL add_ContestProblem_PdfDescription ;

DROP PROCEDURE add_ContestProblem_PdfDescription;


DROP PROCEDURE IF EXISTS add_Discussion_Cid;
DELIMITER $$

CREATE PROCEDURE add_Discussion_Cid ()
BEGIN
-- 检查是否已存在 cid 字段
IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'discussion'
	AND column_name = 'cid'
) THEN
	-- 如果不存在则添加 cid 字段
	ALTER TABLE discussion ADD COLUMN `cid` bigint unsigned COMMENT '比赛id';
END IF;

-- 检查是否已存在外键 discussion_ibfk_8
IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.TABLE_CONSTRAINTS
	WHERE
		CONSTRAINT_NAME = 'discussion_ibfk_8'
	AND TABLE_NAME = 'discussion'
) THEN
	-- 添加外键约束，将 cid 与 contest 表的 id 关联
	ALTER TABLE discussion
	ADD CONSTRAINT `discussion_ibfk_8` FOREIGN KEY (`cid`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
END IF;
END$$

DELIMITER ;

-- 调用存储过程
CALL add_Discussion_Cid();

-- 删除存储过程
DROP PROCEDURE add_Discussion_Cid;


DROP PROCEDURE IF EXISTS add_Discussion_Tid;
DELIMITER $$

CREATE PROCEDURE add_Discussion_Tid ()
BEGIN
-- 检查是否已存在 cid 字段
IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'discussion'
	AND column_name = 'tid'
) THEN
	-- 如果不存在则添加 cid 字段
	ALTER TABLE discussion ADD COLUMN `tid` bigint unsigned COMMENT '训练id';
END IF;

-- 检查是否已存在外键 discussion_ibfk_9
IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.TABLE_CONSTRAINTS
	WHERE
		CONSTRAINT_NAME = 'discussion_ibfk_9'
	AND TABLE_NAME = 'discussion'
) THEN
	-- 添加外键约束，将 tid 与 contest 表的 id 关联
	ALTER TABLE discussion
	ADD CONSTRAINT `discussion_ibfk_9` FOREIGN KEY (`tid`) REFERENCES `training` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
END IF;
END$$

DELIMITER ;

-- 调用存储过程
CALL add_Discussion_Tid();

-- 删除存储过程
DROP PROCEDURE add_Discussion_Tid;


/*
* remote_judge_account 添加 is_alive
*/
DROP PROCEDURE
IF EXISTS add_RemoteJudgeAccount_TitleAndLink;
DELIMITER $$

CREATE PROCEDURE add_RemoteJudgeAccount_TitleAndLink ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'remote_judge_account'
	AND column_name = 'is_alive'
) THEN
	ALTER TABLE remote_judge_account ADD COLUMN `is_alive` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否更新Cookies';
	ALTER TABLE remote_judge_account ADD COLUMN `title` varchar(255) DEFAULT NULL COMMENT '名称';
	ALTER TABLE remote_judge_account ADD COLUMN `link` varchar(255) DEFAULT NULL COMMENT '网址';
END
IF ; END$$

DELIMITER ;
CALL add_RemoteJudgeAccount_TitleAndLink ;

DROP PROCEDURE add_RemoteJudgeAccount_TitleAndLink;


/*
* 更新 judge 表中的 submit_id
*/
DELIMITER $$

-- 删除外键的通用存储过程
DROP PROCEDURE IF EXISTS DropForeignKeyIfExists$$
CREATE PROCEDURE DropForeignKeyIfExists (
    IN tbl_name VARCHAR(64),
    IN fk_name VARCHAR(64)
)
BEGIN
    DECLARE fk_exists INT;

    -- 检查外键是否存在
    SELECT COUNT(*)
    INTO fk_exists
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_NAME = tbl_name
    AND CONSTRAINT_NAME = fk_name
    AND TABLE_SCHEMA = DATABASE();

    -- 如果外键存在，则删除它
    IF fk_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl_name, ' DROP FOREIGN KEY ', fk_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 添加外键的通用存储过程
DROP PROCEDURE IF EXISTS AddForeignKey$$
CREATE PROCEDURE AddForeignKey (
    IN tbl_name VARCHAR(64),
    IN fk_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN ref_table_name VARCHAR(64),
    IN ref_col_name VARCHAR(64)
)
BEGIN
    DECLARE fk_exists INT;

    -- 检查外键是否已经存在
    SELECT COUNT(*)
    INTO fk_exists
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_NAME = tbl_name
    AND CONSTRAINT_NAME = fk_name
    AND TABLE_SCHEMA = DATABASE();

    -- 如果外键不存在，则添加它
    IF fk_exists = 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE ', tbl_name,
            ' ADD CONSTRAINT ', fk_name,
            ' FOREIGN KEY (', col_name, ') REFERENCES ', ref_table_name, '(', ref_col_name, ')'
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 更新记录 submit_id 的存储过程
DROP PROCEDURE IF EXISTS UpdateSubmitId$$
CREATE PROCEDURE UpdateSubmitId()
BEGIN
    -- 更新对应的 submit_id，确保只更新匹配的行
    UPDATE contest_record cr
    JOIN judge j ON cr.submit_id = j.submit_id
    SET cr.submit_id = j.sorted_id
    WHERE cr.submit_id = j.submit_id;

    UPDATE training_record tr
    JOIN judge j ON tr.submit_id = j.submit_id
    SET tr.submit_id = j.sorted_id
    WHERE tr.submit_id = j.submit_id;

    UPDATE user_acproblem ua
    JOIN judge j ON ua.submit_id = j.submit_id
    SET ua.submit_id = j.sorted_id
    WHERE ua.submit_id = j.submit_id;

    UPDATE judge_case jc
    JOIN judge j ON jc.submit_id = j.submit_id
    SET jc.submit_id = j.sorted_id
    WHERE jc.submit_id = j.submit_id;
END$$

-- 整体流程的存储过程
DROP PROCEDURE IF EXISTS UpdateJudgeTable$$
CREATE PROCEDURE UpdateJudgeTable()
BEGIN
    -- 禁用外键检查
    SET FOREIGN_KEY_CHECKS = 0;

    -- 按照时间顺序更新 sorted_id
    SET @row_number = 0;

    -- 更新 sorted_id，确保每个 judge 记录都有一个新的排序 ID
    UPDATE judge
    SET sorted_id = (@row_number := @row_number + 1)
    WHERE submit_time IS NOT NULL -- 确保有时间数据
    ORDER BY submit_time ASC;

    -- 删除外键约束
    CALL DropForeignKeyIfExists('contest_record', 'contest_record_ibfk_5');
    CALL DropForeignKeyIfExists('training_record', 'training_record_ibfk_5');
    CALL DropForeignKeyIfExists('user_acproblem', 'user_acproblem_ibfk_3');
    CALL DropForeignKeyIfExists('judge_case', 'judge_case_ibfk_3');

    -- 更新 submit_id
    CALL UpdateSubmitId();

    -- 创建临时表
    CREATE TABLE judge_temp LIKE judge;

    -- 删除 sorted_id 列
    ALTER TABLE judge_temp DROP COLUMN `sorted_id`;

    -- 复制数据到临时表，并替换 submit_id 为 sorted_id
    INSERT INTO judge_temp (submit_id, pid, display_pid, uid, username, submit_time, status, share, error_message, time, memory, score, length, code, language, gid, cid, cpid, judger, ip, version, oi_rank_score, vjudge_submit_id, vjudge_username, vjudge_password, oi_rank, is_manual, synchronous, gmt_create, gmt_modified, is_reset, `key`)
    SELECT sorted_id, pid, display_pid, uid, username, submit_time, status, share, error_message, time, memory, score, length, code, language, gid, cid, cpid, judger, ip, version, oi_rank_score, vjudge_submit_id, vjudge_username, vjudge_password, oi_rank, is_manual, synchronous, gmt_create, gmt_modified, is_reset, `key`
    FROM judge;

    -- 删除原始表
    DROP TABLE judge;

    -- 重命名新表为原来的表名
    RENAME TABLE judge_temp TO judge;

    -- 重新添加外键约束
    CALL AddForeignKey('contest_record', 'contest_record_ibfk_5', 'submit_id', 'judge', 'submit_id');
    CALL AddForeignKey('training_record', 'training_record_ibfk_5', 'submit_id', 'judge', 'submit_id');
    CALL AddForeignKey('user_acproblem', 'user_acproblem_ibfk_3', 'submit_id', 'judge', 'submit_id');
    CALL AddForeignKey('judge_case', 'judge_case_ibfk_3', 'submit_id', 'judge', 'submit_id');

    -- 启用外键检查
    SET FOREIGN_KEY_CHECKS = 1;
END$$

DELIMITER ;

-- 调用主存储过程以执行所有操作
CALL UpdateJudgeTable();


/*
* user_acproblem 添加 gid
*/
DROP PROCEDURE
IF EXISTS add_UserAcproblem_Gid;
DELIMITER $$

CREATE PROCEDURE add_UserAcproblem_Gid ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_acproblem'
	AND column_name = 'gid'
) THEN
	ALTER TABLE user_acproblem ADD COLUMN `gid` bigint(20) unsigned DEFAULT NULL;
END
IF ; END$$

DELIMITER ;
CALL add_UserAcproblem_Gid ;

DROP PROCEDURE add_UserAcproblem_Gid;


/*
* user_acproblem 插入和更新 gid
*/
INSERT INTO user_acproblem (gid, pid, uid, submit_id, gmt_create, gmt_modified)
SELECT j.gid, j.pid, j.uid, j.submit_id, j.gmt_create, j.gmt_modified
FROM judge j
LEFT JOIN user_acproblem u ON u.submit_id = j.submit_id
WHERE j.status = 0 AND u.submit_id IS NULL;

UPDATE user_acproblem uc
JOIN judge j ON uc.submit_id = j.submit_id
SET uc.gid = j.gid, uc.gmt_create = j.gmt_create;


/*
* user_sign 添加 faculty
*/
DROP PROCEDURE
IF EXISTS add_UserSign_Faculty;
DELIMITER $$

CREATE PROCEDURE add_UserSign_Faculty ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_sign'
	AND column_name = 'faculty'
) THEN
	ALTER TABLE user_sign ADD COLUMN `faculty` varchar(100) DEFAULT NULL COMMENT '院系';
END
IF ; END$$

DELIMITER ;
CALL add_UserSign_Faculty;

DROP PROCEDURE add_UserSign_Faculty;


-- 删除 contest_print_ibfk_2 外键约束
ALTER TABLE contest_print DROP FOREIGN KEY contest_print_ibfk_2;

-- 删除 user_cloc_ibfk_2 外键约束
ALTER TABLE user_cloc DROP FOREIGN KEY user_cloc_ibfk_2;

-- 对于 contest_print 表
ALTER TABLE contest_print DROP COLUMN username;
ALTER TABLE contest_print DROP COLUMN realname;

-- 对于 user_sign 表
ALTER TABLE user_sign DROP COLUMN username;

-- 对于 user_multi_oj 表
ALTER TABLE user_multi_oj DROP COLUMN username;

-- 对于 user_cloc 表
ALTER TABLE user_cloc DROP COLUMN username;
ALTER TABLE user_cloc DROP COLUMN realname;

-- 对于 statistic_rank 表
ALTER TABLE statistic_rank DROP COLUMN username;
ALTER TABLE statistic_rank DROP COLUMN realname;

-- 删除依赖于 user_info.avatar 的外键约束
ALTER TABLE comment DROP FOREIGN KEY comment_ibfk_6;
# ALTER TABLE discussion DROP FOREIGN KEY discussion_ibfk_4;
ALTER TABLE reply DROP FOREIGN KEY reply_ibfk_2;
ALTER TABLE reply DROP FOREIGN KEY reply_ibfk_3;

-- 删除 user_info 表中 avatar 字段的唯一索引
ALTER TABLE user_info DROP INDEX avatar;

-- 修改 avatar 字段为可重复值
ALTER TABLE user_info MODIFY avatar VARCHAR(255) NULL COMMENT '头像地址';


/*
* contest_print 添加 uid
*/
DROP PROCEDURE
IF EXISTS add_ContestPrint_Uid;
DELIMITER $$

CREATE PROCEDURE add_ContestPrint_Uid ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_print'
	AND column_name = 'uid'
) THEN
	ALTER TABLE contest_print ADD COLUMN `uid` varchar(32) COLLATE utf8_general_ci DEFAULT NULL COMMENT '用户编号';
    ALTER TABLE contest_print ADD KEY `uid` (`uid`);
	ALTER TABLE contest_print ADD CONSTRAINT `contest_print_ibfk_3` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;
END
IF ; END$$

DELIMITER ;
CALL add_ContestPrint_Uid;

DROP PROCEDURE add_ContestPrint_Uid;


/*
* problem 添加 status

*/
DROP PROCEDURE
IF EXISTS add_Problem_Status;
DELIMITER $$

CREATE PROCEDURE add_Problem_Status ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'problem'
	AND column_name = 'status'
) THEN
	ALTER TABLE problem ADD COLUMN `status` tinyint(1) DEFAULT '0' COMMENT '是否封禁';
END
IF ; END$$

DELIMITER ;
CALL add_Problem_Status;

DROP PROCEDURE add_Problem_Status;


/*
* 添加 remote_judge

*/
DROP PROCEDURE
IF EXISTS add_remoteJudge;
DELIMITER $$

CREATE PROCEDURE add_remoteJudge ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'remote_judge'
) THEN
	CREATE TABLE `remote_judge` (
		`oj` varchar(255) NOT NULL COMMENT '题目id',
		`percent` int(11) DEFAULT NULL COMMENT '通过率, 0~100',
		`gmt_create` datetime DEFAULT CURRENT_TIMESTAMP
	) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
END
IF ; END$$

DELIMITER ;
CALL add_remoteJudge ;

DROP PROCEDURE add_remoteJudge;


/*
* user_sign 添加 englishname, st_school, ed_school
*/
DROP PROCEDURE
IF EXISTS add_UserSign;
DELIMITER $$

CREATE PROCEDURE add_UserSign ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'user_sign'
	AND column_name = 'englishname'
) THEN
	ALTER TABLE user_sign ADD COLUMN `englishname` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '英文姓名';
	ALTER TABLE user_sign ADD COLUMN `st_school` datetime DEFAULT NULL COMMENT '入学年份';
	ALTER TABLE user_sign ADD COLUMN `ed_school` datetime DEFAULT NULL COMMENT '毕业年份';
	ALTER TABLE user_sign MODIFY COLUMN username varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户名';
    ALTER TABLE user_sign MODIFY COLUMN realname varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '真实姓名';
END
IF ; END$$

DELIMITER ;
CALL add_UserSign;

DROP PROCEDURE add_UserSign;


/*
* contest 添加 modify_end_time
*/
DROP PROCEDURE
IF EXISTS add_Contest_modifyEndTime;
DELIMITER $$

CREATE PROCEDURE add_Contest_modifyEndTime ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest'
	AND column_name = 'modify_end_time'
) THEN
	ALTER TABLE contest ADD COLUMN `modify_end_time` datetime DEFAULT NULL COMMENT '信息修改结束时间';
END
IF ; END$$

DELIMITER ;
CALL add_Contest_modifyEndTime;

DROP PROCEDURE add_Contest_modifyEndTime;


/*
* contest_sign 添加 username1, username2, username3 等
*/
DROP PROCEDURE
IF EXISTS add_TeamSign_teamNumber;
DELIMITER $$

CREATE PROCEDURE add_TeamSign_teamNumber ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'contest_sign'
	AND column_name = 'username1'
) THEN
	ALTER TABLE contest_sign MODIFY COLUMN `type` int(11) DEFAULT '0' COMMENT '报名类型（0为正式名额，1为女队名额，2为打星名额，3为外卡名额）';
	ALTER TABLE contest_sign DROP COLUMN gender, DROP COLUMN team_names;
	ALTER TABLE contest_sign ADD COLUMN `instructor` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '指导老师';
	ALTER TABLE contest_sign ADD COLUMN `username1` varchar(100) DEFAULT NULL COMMENT '队长';
	ALTER TABLE contest_sign ADD COLUMN `username2` varchar(100) DEFAULT NULL COMMENT '队员1';
	ALTER TABLE contest_sign ADD COLUMN `username3` varchar(100) DEFAULT NULL COMMENT '队员2';
	ALTER TABLE contest_sign ADD COLUMN `visible` tinyint(1) DEFAULT '1' COMMENT '是否为队伍池中的，0为队伍池中的';
    ALTER TABLE contest_sign ADD KEY `username1` (`username1`);
    ALTER TABLE contest_sign ADD KEY `username2` (`username2`);
    ALTER TABLE contest_sign ADD KEY `username3` (`username3`);
	ALTER TABLE contest_sign ADD CONSTRAINT `contest_sign_ibfk_3` FOREIGN KEY (`username1`) REFERENCES `user_info` (`username`) ON DELETE CASCADE ON UPDATE CASCADE;
	ALTER TABLE contest_sign ADD CONSTRAINT `contest_sign_ibfk_4` FOREIGN KEY (`username2`) REFERENCES `user_info` (`username`) ON DELETE CASCADE ON UPDATE CASCADE;
	ALTER TABLE contest_sign ADD CONSTRAINT `contest_sign_ibfk_5` FOREIGN KEY (`username3`) REFERENCES `user_info` (`username`) ON DELETE CASCADE ON UPDATE CASCADE;

END
IF ; END$$

DELIMITER ;
CALL add_TeamSign_teamNumber;

DROP PROCEDURE add_TeamSign_teamNumber;


/*
* role 添加 coach_admin
*/
DROP PROCEDURE
IF EXISTS add_Role_CoachAdmin;
DELIMITER $$

CREATE PROCEDURE add_Role_CoachAdmin ()
BEGIN

IF NOT EXISTS (
	SELECT 1 FROM role WHERE role = "coach_admin"
) THEN
	INSERT INTO role (id, role, description, status) VALUES (1011, 'coach_admin', '教练', 0);
END
IF ; END$$

DELIMITER ;
CALL add_Role_CoachAdmin;

DROP PROCEDURE add_Role_CoachAdmin;


/*
* 添加 school_user
*/
DROP PROCEDURE
IF EXISTS add_schoolUser;
DELIMITER $$

CREATE PROCEDURE add_schoolUser ()
BEGIN

IF NOT EXISTS (
	SELECT
		1
	FROM
		information_schema.`COLUMNS`
	WHERE
		table_name = 'school_user'
) THEN
    CREATE TABLE `school_user` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `sid` bigint(20) unsigned NOT NULL COMMENT '学校id',
    `uid` varchar(32) COLLATE utf8_general_ci DEFAULT NULL COMMENT '用户编号',
    `coach_uid` varchar(32) COLLATE utf8_general_ci DEFAULT NULL COMMENT '教练用户编号',
	`status` int(11) DEFAULT '0',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `sid` (`sid`),
    KEY `uid` (`uid`),
    KEY `coach_uid` (`coach_uid`),
    CONSTRAINT `school_user_ibfk_1` FOREIGN KEY (`sid`) REFERENCES `school` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `school_user_ibfk_2` FOREIGN KEY (`uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `school_user_ibfk_3` FOREIGN KEY (`coach_uid`) REFERENCES `user_info` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
END
IF ; END$$

DELIMITER ;
CALL add_schoolUser ;

DROP PROCEDURE add_schoolUser;


/*
* 修改 contest_sign 名为 team_sign
*/
rename table contest_sign to team_sign;


/*
* 将 problem 表中的 io_score 修改为 score
*/
DROP PROCEDURE IF EXISTS update_Problem_IoScoreToScore;
DELIMITER $$

CREATE PROCEDURE update_Problem_IoScoreToScore ()
BEGIN
    -- 如果 problem 表中存在 io_score 字段 且 不存在 score 字段，则进行重命名
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE table_name = 'problem' AND column_name = 'io_score'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE table_name = 'problem' AND column_name = 'score'
    ) THEN
        ALTER TABLE problem CHANGE COLUMN `io_score` `score` INT(11) DEFAULT NULL COMMENT '题目总分数';
    END IF;
END$$

DELIMITER ;

CALL update_Problem_IoScoreToScore;

DROP PROCEDURE update_Problem_IoScoreToScore;




