CREATE DATABASE IF NOT EXISTS JudgeSystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE JudgeSystem;

CREATE TABLE IF NOT EXISTS Problems (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_path VARCHAR(500),
    time_limit INT DEFAULT 2000,
    memory_limit INT DEFAULT 256,
    contest_type VARCHAR(50) DEFAULT 'ICPC',
    checker_script TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Testcases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    problem_id INT NOT NULL,
    input_data TEXT,
    expected_output TEXT,
    testcase_type VARCHAR(50) DEFAULT 'normal',
    is_ai_generated BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (problem_id) REFERENCES Problems(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS SampleCodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    problem_id INT NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(20) NOT NULL DEFAULT 'java',
    expected_type VARCHAR(10) NOT NULL DEFAULT 'AC',
    is_ai_generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (problem_id) REFERENCES Problems(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Submissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    problem_id INT NOT NULL,
    sample_code_id INT,
    testcase_id INT NOT NULL,
    actual_output TEXT,
    execution_time INT,
    memory_used INT,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (problem_id) REFERENCES Problems(id) ON DELETE CASCADE,
    FOREIGN KEY (sample_code_id) REFERENCES SampleCodes(id) ON DELETE SET NULL,
    FOREIGN KEY (testcase_id) REFERENCES Testcases(id) ON DELETE CASCADE
);
