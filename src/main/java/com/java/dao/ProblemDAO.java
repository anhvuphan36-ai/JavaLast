package com.java.dao;

import com.java.model.Problem;
import com.java.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProblemDAO {

    public int addProblem(Problem problem) throws SQLException {
        String sql = "INSERT INTO Problems (title, description, image_path, time_limit, memory_limit, contest_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, problem.getTitle());
            stmt.setString(2, problem.getDescription());
            stmt.setString(3, problem.getImagePath());
            stmt.setInt(4, problem.getTimeLimit());
            stmt.setInt(5, problem.getMemoryLimit());
            stmt.setString(6, problem.getContestType());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public Problem getProblemById(int id) throws SQLException {
        String sql = "SELECT * FROM Problems WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProblem(rs);
                }
            }
        }
        return null;
    }

    public List<Problem> getAllProblems() throws SQLException {
        String sql = "SELECT * FROM Problems ORDER BY created_at DESC";
        List<Problem> problems = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                problems.add(mapResultSetToProblem(rs));
            }
        }
        return problems;
    }

    public boolean updateProblem(Problem problem) throws SQLException {
        String sql = "UPDATE Problems SET title=?, description=?, image_path=?, time_limit=?, memory_limit=?, contest_type=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, problem.getTitle());
            stmt.setString(2, problem.getDescription());
            stmt.setString(3, problem.getImagePath());
            stmt.setInt(4, problem.getTimeLimit());
            stmt.setInt(5, problem.getMemoryLimit());
            stmt.setString(6, problem.getContestType());
            stmt.setInt(7, problem.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateCheckerScript(int problemId, String checkerScript) throws SQLException {
        String sql = "UPDATE Problems SET checker_script=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, checkerScript);
            stmt.setInt(2, problemId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteProblem(int id) throws SQLException {
        String sql = "DELETE FROM Problems WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public void resetAutoIncrement() throws SQLException {
        String maxSql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM Problems";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(maxSql)) {
            int nextId = 1;
            if (rs.next()) {
                nextId = rs.getInt("next_id");
            }
            try (Statement alter = conn.createStatement()) {
                alter.executeUpdate("ALTER TABLE Problems AUTO_INCREMENT = " + nextId);
            }
        }
    }

    public int countProblems() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Problems";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Problem mapResultSetToProblem(ResultSet rs) throws SQLException {
        Problem p = new Problem();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setImagePath(rs.getString("image_path"));
        p.setTimeLimit(rs.getInt("time_limit"));
        p.setMemoryLimit(rs.getInt("memory_limit"));
        p.setContestType(rs.getString("contest_type"));
        p.setCheckerScript(rs.getString("checker_script"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
