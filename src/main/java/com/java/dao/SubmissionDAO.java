package com.java.dao;

import com.java.model.Submission;
import com.java.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SubmissionDAO {

    private static final Logger logger = Logger.getLogger(SubmissionDAO.class.getName());

    public boolean addSubmission(Submission s) {
        String sql = "INSERT INTO Submissions (problem_id, sample_code_id, testcase_id, actual_output, execution_time, memory_used, status, error_message) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, s.getProblemId());
            if (s.getSampleCodeId() > 0) {
                ps.setInt(2, s.getSampleCodeId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, s.getTestcaseId());
            ps.setString(4, s.getActualOutput());
            ps.setInt(5, s.getExecutionTime());
            ps.setInt(6, s.getMemoryUsed());
            ps.setString(7, s.getStatus());
            ps.setString(8, s.getErrorMessage());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        s.setId(rs.getInt(1));
                    }
                }
            }
            return affected > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add submission", e);
            return false;
        }
    }

    public List<Submission> getByProblemId(int problemId) {
        List<Submission> list = new ArrayList<>();
        String sql = "SELECT * FROM Submissions WHERE problem_id = ? ORDER BY submitted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get submissions for problem " + problemId, e);
        }
        return list;
    }

    public List<Submission> getAllSubmissions() {
        List<Submission> list = new ArrayList<>();
        String sql = "SELECT * FROM Submissions ORDER BY submitted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql);
              ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get all submissions", e);
        }
        return list;
    }

    public List<Submission> getBySampleCodeId(int sampleCodeId) {
        List<Submission> list = new ArrayList<>();
        String sql = "SELECT * FROM Submissions WHERE sample_code_id = ? ORDER BY submitted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sampleCodeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get submissions for sample code " + sampleCodeId, e);
        }
        return list;
    }

    public Submission getById(int id) {
        String sql = "SELECT * FROM Submissions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get submission " + id, e);
        }
        return null;
    }

    public boolean updateSubmission(Submission s) {
        String sql = "UPDATE Submissions SET actual_output = ?, execution_time = ?, memory_used = ?, status = ?, error_message = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getActualOutput());
            ps.setInt(2, s.getExecutionTime());
            ps.setInt(3, s.getMemoryUsed());
            ps.setString(4, s.getStatus());
            ps.setString(5, s.getErrorMessage());
            ps.setInt(6, s.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update submission " + s.getId(), e);
            return false;
        }
    }

    public boolean deleteSubmission(int id) {
        String sql = "DELETE FROM Submissions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete submission " + id, e);
            return false;
        }
    }

    private Submission mapResultSet(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getInt("id"));
        s.setProblemId(rs.getInt("problem_id"));
        s.setSampleCodeId(rs.getInt("sample_code_id"));
        s.setTestcaseId(rs.getInt("testcase_id"));
        s.setActualOutput(rs.getString("actual_output"));
        s.setExecutionTime(rs.getInt("execution_time"));
        s.setMemoryUsed(rs.getInt("memory_used"));
        s.setStatus(rs.getString("status"));
        s.setErrorMessage(rs.getString("error_message"));
        s.setSubmittedAt(rs.getTimestamp("submitted_at"));
        return s;
    }
}
