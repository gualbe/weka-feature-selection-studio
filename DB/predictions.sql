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
-- Database: "attrselexp"
--

-- --------------------------------------------------------

--
-- Table structure for table "predictions"
--

CREATE TABLE "predictions" (
  "id" int(11) NOT NULL,
  "idexperiment" int(11) NOT NULL,
  "actual" double NOT NULL,
  "predicted" double NOT NULL,
  "dataset" text COLLATE utf8_spanish_ci NOT NULL,
  "evaluator" text COLLATE utf8_spanish_ci NOT NULL,
  "search" text COLLATE utf8_spanish_ci NOT NULL,
  "classifier" text COLLATE utf8_spanish_ci NOT NULL,
  "attributes" varchar(200) COLLATE utf8_spanish_ci DEFAULT NULL,
  "index_attr_selected" varchar(200) COLLATE utf8_spanish_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_spanish_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table "predictions"
--
ALTER TABLE "predictions"
  ADD PRIMARY KEY ("id"),
  ADD KEY "idexperiments" ("idexperiment");

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table "predictions"
--
ALTER TABLE "predictions"
  MODIFY "id" int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- Constraints for dumped tables
--

--
-- Constraints for table "predictions"
--
ALTER TABLE "predictions"
  ADD CONSTRAINT "predictions_ibfk_1" FOREIGN KEY ("idexperiment") REFERENCES "experiment" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
