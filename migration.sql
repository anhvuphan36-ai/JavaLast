-- Migration script for existing JudgeSystem databases
-- Run this AFTER applying init.sql changes to fix FK constraints

USE JudgeSystem;

-- Fix 1: Allow NULL sample_code_id for ad-hoc judging submissions
ALTER TABLE Submissions
    MODIFY COLUMN sample_code_id INT,
    DROP FOREIGN KEY Submissions_ibfk_2,
    ADD CONSTRAINT Submissions_ibfk_2 FOREIGN KEY (sample_code_id) REFERENCES SampleCodes(id) ON DELETE SET NULL;
