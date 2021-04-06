-- phpMyAdmin SQL Dump
-- version 5.0.4
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 25, 2021 at 06:20 PM
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

--
-- Indexes for dumped tables
--

--
-- Indexes for table `metrics`
--
ALTER TABLE `metrics`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idexperiment` (`idexperiment`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `metrics`
--
ALTER TABLE `metrics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `metrics`
--
ALTER TABLE `metrics`
  ADD CONSTRAINT `metrics_ibfk_1` FOREIGN KEY (`idexperiment`) REFERENCES `experiment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
