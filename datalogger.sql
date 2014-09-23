-- phpMyAdmin SQL Dump
-- version 4.2.9
-- http://www.phpmyadmin.net
--
-- Värd: 127.0.0.1
-- Tid vid skapande: 23 sep 2014 kl 20:32
-- Serverversion: 10.0.13-MariaDB
-- PHP-version: 5.4.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Databas: `datalogger`
--

-- --------------------------------------------------------

--
-- Tabellstruktur `logdata`
--

CREATE TABLE IF NOT EXISTS `logdata` (
`id` int(11) NOT NULL,
  `value` double NOT NULL,
  `logtype_id` int(11) NOT NULL,
  `tstamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `logdev_id` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

--
-- Dumpning av Data i tabell `logdata`
--

INSERT INTO `logdata` (`id`, `value`, `logtype_id`, `tstamp`, `logdev_id`) VALUES
(1, 123.456, 1, '2014-09-23 18:04:34', 1);

-- --------------------------------------------------------

--
-- Tabellstruktur `logdev`
--

CREATE TABLE IF NOT EXISTS `logdev` (
`id` int(11) NOT NULL,
  `name` varchar(80) NOT NULL,
  `description` varchar(255) NOT NULL,
  `devtype` varchar(20) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

--
-- Dumpning av Data i tabell `logdev`
--

INSERT INTO `logdev` (`id`, `name`, `description`, `devtype`) VALUES
(1, 'fictive', 'testing device 1', 'virtual');

-- --------------------------------------------------------

--
-- Tabellstruktur `logtype`
--

CREATE TABLE IF NOT EXISTS `logtype` (
`id` int(11) NOT NULL,
  `unit_id` int(11) NOT NULL,
  `description` varchar(255) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

--
-- Dumpning av Data i tabell `logtype`
--

INSERT INTO `logtype` (`id`, `unit_id`, `description`) VALUES
(1, 1, 'testning');

-- --------------------------------------------------------

--
-- Tabellstruktur `unit`
--

CREATE TABLE IF NOT EXISTS `unit` (
`id` int(11) NOT NULL,
  `description` varchar(255) NOT NULL,
  `name` varchar(20) NOT NULL,
  `base_unit_id` int(11) DEFAULT NULL,
  `base_factor` double NOT NULL DEFAULT '1'
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

--
-- Dumpning av Data i tabell `unit`
--

INSERT INTO `unit` (`id`, `description`, `name`, `base_unit_id`, `base_factor`) VALUES
(1, 'no-unit', '', NULL, 1),
(2, 'meter', 'm', NULL, 1),
(3, 'millimeter', 'mm', 2, 0.001),
(4, 'temperature', '°C', NULL, 1);

--
-- Index för dumpade tabeller
--

--
-- Index för tabell `logdata`
--
ALTER TABLE `logdata`
 ADD PRIMARY KEY (`id`), ADD KEY `logtype_id` (`logtype_id`), ADD KEY `logdev_id` (`logdev_id`);

--
-- Index för tabell `logdev`
--
ALTER TABLE `logdev`
 ADD PRIMARY KEY (`id`);

--
-- Index för tabell `logtype`
--
ALTER TABLE `logtype`
 ADD PRIMARY KEY (`id`);

--
-- Index för tabell `unit`
--
ALTER TABLE `unit`
 ADD PRIMARY KEY (`id`), ADD KEY `base_unit_id` (`base_unit_id`);

--
-- AUTO_INCREMENT för dumpade tabeller
--

--
-- AUTO_INCREMENT för tabell `logdata`
--
ALTER TABLE `logdata`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT för tabell `logdev`
--
ALTER TABLE `logdev`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT för tabell `logtype`
--
ALTER TABLE `logtype`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=2;
--
-- AUTO_INCREMENT för tabell `unit`
--
ALTER TABLE `unit`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=5;
--
-- Restriktioner för dumpade tabeller
--

--
-- Restriktioner för tabell `logdata`
--
ALTER TABLE `logdata`
ADD CONSTRAINT `logdata_ibfk_2` FOREIGN KEY (`logdev_id`) REFERENCES `logdev` (`id`),
ADD CONSTRAINT `logdata_ibfk_1` FOREIGN KEY (`logtype_id`) REFERENCES `logtype` (`id`);

--
-- Restriktioner för tabell `unit`
--
ALTER TABLE `unit`
ADD CONSTRAINT `unit_ibfk_1` FOREIGN KEY (`base_unit_id`) REFERENCES `unit` (`id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
