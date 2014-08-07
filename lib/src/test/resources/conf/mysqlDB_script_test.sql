CREATE DATABASE `social_test` /*!40100 DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci */;

USE `social_test`;

CREATE TABLE `activity` (
  `_id` varchar(36) NOT NULL,
  `title` varchar(2000) DEFAULT NULL,
  `titleId` varchar(36) DEFAULT NULL,
  `body` varchar(2000) DEFAULT NULL,
  `bodyId` varchar(36) DEFAULT NULL,
  `postedTime` bigint(20) unsigned DEFAULT NULL,
  `lastUpdated` bigint(20) unsigned DEFAULT NULL,
  `posterId` varchar(36) DEFAULT NULL,
  `ownerId` varchar(36) DEFAULT NULL,
  `permaLink` varchar(255) DEFAULT NULL,
  `appId` varchar(36) DEFAULT NULL,
  `externalId` varchar(36) DEFAULT NULL,
  `priority` float DEFAULT NULL,
  `hidable` tinyint(4) DEFAULT NULL,
  `lockable` tinyint(4) DEFAULT NULL,
  `likers` varchar(2000) DEFAULT NULL,
  `metadata` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `comment` (
  `_id` varchar(36) NOT NULL,
  `activityId` varchar(36) NOT NULL,
  `title` varchar(2000) DEFAULT NULL,
  `titleId` varchar(36) DEFAULT NULL,
  `body` varchar(2000) DEFAULT NULL,
  `bodyId` varchar(36) DEFAULT NULL,
  `postedTime` bigint(20) unsigned DEFAULT NULL,
  `lastUpdated` bigint(20) unsigned DEFAULT NULL,
  `posterId` varchar(36) DEFAULT NULL,
  `ownerId` varchar(36) DEFAULT NULL,
  `permaLink` varchar(255) DEFAULT NULL,
  `hidable` tinyint(4) DEFAULT NULL,
  `lockable` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `stream_item` (
  `_id` varchar(36) NOT NULL,
  `activityId` varchar(36) DEFAULT NULL,
  `ownerId` varchar(36) DEFAULT NULL,
  `posterId` varchar(36) DEFAULT NULL,
  `viewerId` varchar(36) DEFAULT NULL,
  `viewerType` varchar(1000) DEFAULT NULL,
  `hidable` tinyint(4) DEFAULT NULL,
  `lockable` tinyint(4) DEFAULT NULL,
  `time` bigint(20) unsigned DEFAULT NULL,
  `mentioner` smallint(5) unsigned DEFAULT NULL,
  `commenter` smallint(5) unsigned DEFAULT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `stream_tracking` (
  `_id` varchar(36) NOT NULL,
  `viewerId` varchar(36) DEFAULT NULL,
  `time` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
