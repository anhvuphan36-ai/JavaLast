package com.java.dao;

import com.java.model.Testcase;
import com.java.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestcaseDAO {

    private static final Logger logger = Logger.getLogger(TestcaseDAO.class.getName());

    // 1. Hàm Thêm Testcase mới
    public boolean addTestcase(Testcase t) {
        String sql = "INSERT INTO Testcases (problem_id, input_data, expected_output, testcase_type, is_ai_generated) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, t.getProblemId());
            ps.setString(2, t.getInputData());
            ps.setString(3, t.getExpectedOutput());
            ps.setString(4, t.getTestcaseType());
            ps.setBoolean(5, t.isAiGenerated());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        t.setId(rs.getInt(1));
                    }
                }
            }
            return affected > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to add testcase", e);
            return false;
        }
    }

    public boolean deleteTestcasesByInput(int problemId, String inputData) {
        String sql = "DELETE FROM Testcases WHERE problem_id = ? AND input_data = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            ps.setString(2, inputData);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete testcases by input", e);
            return false;
        }
    }

    // 2. Hàm Lấy danh sách Testcase
    public List<Testcase> getTestcasesByProblemId(int problemId) {
        List<Testcase> list = new ArrayList<>();
        String sql = "SELECT * FROM Testcases WHERE problem_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String input = rs.getString("input_data");
                    String output = rs.getString("expected_output");
                    String type = rs.getString("testcase_type");
                    boolean aiGen = rs.getBoolean("is_ai_generated");

                    list.add(new Testcase(id, problemId, input, output, type, aiGen));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to get testcases for problem " + problemId, e);
        }
        return list;
    }
    // 3. Hàm CẬP NHẬT (Sửa) Testcase
    public boolean updateTestcase(Testcase t) {
        String sql = "UPDATE Testcases SET input_data = ?, expected_output = ?, testcase_type = ?, is_ai_generated = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, t.getInputData());
            ps.setString(2, t.getExpectedOutput());
            ps.setString(3, t.getTestcaseType());
            ps.setBoolean(4, t.isAiGenerated());
            ps.setInt(5, t.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update testcase " + t.getId(), e);
            return false;
        }
    }

    // 4. Hàm XÓA Testcase
    public boolean deleteTestcase(int id) {
        String sql = "DELETE FROM Testcases WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete testcase " + id, e);
            return false;
        }
    }

    // 5. Cập nhật expected output
    public boolean updateExpectedOutput(int id, String newOutput) {
        String sql = "UPDATE Testcases SET expected_output = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newOutput);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to update expected output for testcase " + id, e);
            return false;
        }
    }

    // 6. Xóa tất cả testcase của một bài
    public int deleteAllByProblemId(int problemId) {
        String sql = "DELETE FROM Testcases WHERE problem_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete all testcases for problem " + problemId, e);
            return 0;
        }
    }

    // 7. Xóa chỉ testcase do AI sinh cho một bài
    public int deleteAiGeneratedByProblemId(int problemId) {
        String sql = "DELETE FROM Testcases WHERE problem_id = ? AND is_ai_generated = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to delete AI testcases for problem " + problemId, e);
            return 0;
        }
    }
}