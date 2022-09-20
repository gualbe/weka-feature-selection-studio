-- phpMyAdmin SQL Dump
-- version 5.0.4
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 25, 2021 at 06:14 PM
-- Server version: 10.4.17-MariaDB
-- PHP Version: 8.0.2

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `attrselexp`
--
CREATE DATABASE IF NOT EXISTS `attrselexp` DEFAULT CHARACTER SET utf8 COLLATE utf8_spanish_ci;
USE `attrselexp`;

-- --------------------------------------------------------

--
-- Table structure for table `experiment`
--

CREATE TABLE `experiment` (
  `id` int(11) NOT NULL,
  `idexperiment_group` int(11) NOT NULL,
  `dataset` text COLLATE utf8_spanish_ci NOT NULL,
  `evaluator` text COLLATE utf8_spanish_ci NOT NULL,
  `search` text COLLATE utf8_spanish_ci NOT NULL,
  `classifier` text COLLATE utf8_spanish_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_spanish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `experiment_group`
--

CREATE TABLE `experiment_group` (
  `id` int(11) NOT NULL,
  `datetime` text COLLATE utf8_spanish_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_spanish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `metrics`
--

CREATE TABLE `metrics` (
  `id` int(11) NOT NULL,
  `idexperiment` int(11) NOT NULL,
  `dataset` text COLLATE utf8_spanish_ci NOT NULL,
  `evaluator` text COLLATE utf8_spanish_ci NOT NULL,
  `search` text COLLATE utf8_spanish_ci NOT NULL,
  `classifier` text COLLATE utf8_spanish_ci NOT NULL,
  `num_attr` int(10) NOT NULL,
  `accuracy` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `precision_value` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `recall` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `f_measure` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `kappa` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `mcc` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `auc` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `mae` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `mse` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `rmse` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `mape` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL,
  `r2` varchar(100) COLLATE utf8_spanish_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_spanish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `predictions`
--

CREATE TABLE `predictions` (
  `id` int(11) NOT NULL,
  `idexperiment` int(11) NOT NULL,
  `actual` double NOT NULL,
  `predicted` double NOT NULL,
  `dataset` text COLLATE utf8_spanish_ci NOT NULL,
  `evaluator` text COLLATE utf8_spanish_ci NOT NULL,
  `search` text COLLATE utf8_spanish_ci NOT NULL,
  `classifier` text COLLATE utf8_spanish_ci NOT NULL,
  `attributes` varchar(200) COLLATE utf8_spanish_ci DEFAULT NULL,
  `index_attr_selected` varchar(200) COLLATE utf8_spanish_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_spanish_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `experiment`
--
ALTER TABLE `experiment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idexperiment_group` (`idexperiment_group`) USING BTREE;

--
-- Indexes for table `experiment_group`
--
ALTER TABLE `experiment_group`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `metrics`
--
ALTER TABLE `metrics`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idexperiment` (`idexperiment`);

--
-- Indexes for table `predictions`
--
ALTER TABLE `predictions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idexperiments` (`idexperiment`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `experiment`
--
ALTER TABLE `experiment`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT for table `experiment_group`
--
ALTER TABLE `experiment_group`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT for table `metrics`
--
ALTER TABLE `metrics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT for table `predictions`
--
ALTER TABLE `predictions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `experiment`
--
ALTER TABLE `experiment`
  ADD CONSTRAINT `experiment_ibfk_1` FOREIGN KEY (`idexperiment_group`) REFERENCES `experiment_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `metrics`
--
ALTER TABLE `metrics`
  ADD CONSTRAINT `metrics_ibfk_1` FOREIGN KEY (`idexperiment`) REFERENCES `experiment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `predictions`
--
ALTER TABLE `predictions`
  ADD CONSTRAINT `predictions_ibfk_1` FOREIGN KEY (`idexperiment`) REFERENCES `experiment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
