package com.java.dao;

import com.java.model.SampleCode;
import com.java.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SampleCodeDAO {

    private static final Logger logger = Logger.getLogger(SampleCodeDAO.class.getName());

    public boolean addSampleCode(SampleCode sc) {
        String sql = "INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sc.getProblemId());
            ps.setString(2, sc.getCodeContent());
            ps.setString(3, sc.getLanguage());
            ps.setString(4, sc.getExpectedType());
            ps.setBoolean(5, sc.isAiGenerated());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        sc.setId(rs.getInt(1));
                    }
                }
            }
            return affected > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add sample code", e);
            return false;
        }
    }

    public List<SampleCode> getByProblemId(int problemId) {
        List<SampleCode> list = new ArrayList<>();
        String sql = "SELECT * FROM SampleCodes WHERE problem_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get sample codes for problem " + problemId, e);
        }
        return list;
    }

    public SampleCode getById(int id) {
        String sql = "SELECT * FROM SampleCodes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get sample code " + id, e);
        }
        return null;
    }

    public boolean updateSampleCode(SampleCode sc) {
        String sql = "UPDATE SampleCodes SET code_content = ?, language = ?, expected_type = ?, is_ai_generated = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sc.getCodeContent());
            ps.setString(2, sc.getLanguage());
            ps.setString(3, sc.getExpectedType());
            ps.setBoolean(4, sc.isAiGenerated());
            ps.setInt(5, sc.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update sample code " + sc.getId(), e);
            return false;
        }
    }

    public boolean deleteSampleCode(int id) {
        String sql = "DELETE FROM SampleCodes WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete sample code " + id, e);
            return false;
        }
    }

    public int deleteAiGeneratedByProblemId(int problemId) {
        String sql = "DELETE FROM SampleCodes WHERE problem_id = ? AND is_ai_generated = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete AI sample codes for problem " + problemId, e);
            return 0;
        }
    }

    private SampleCode mapResultSet(ResultSet rs) throws SQLException {
        SampleCode sc = new SampleCode();
        sc.setId(rs.getInt("id"));
        sc.setProblemId(rs.getInt("problem_id"));
        sc.setCodeContent(rs.getString("code_content"));
        sc.setLanguage(rs.getString("language"));
        sc.setExpectedType(rs.getString("expected_type"));
        sc.setAiGenerated(rs.getBoolean("is_ai_generated"));
        sc.setCreatedAt(rs.getTimestamp("created_at"));
        return sc;
    }
}
