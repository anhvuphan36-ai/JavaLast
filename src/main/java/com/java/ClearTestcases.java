package com.java;

import com.java.util.DatabaseConnection;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ClearTestcases {
    public static void main(String[] args) {
        int[] problemIds = {11, 20, 21};

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int pid : problemIds) {
                // Xóa trong DB
                String sql = "DELETE FROM Testcases WHERE problem_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, pid);
                    int deleted = ps.executeUpdate();
                    System.out.println("De " + pid + ": xoa " + deleted + " testcase trong DB");
                }

                // Xóa file trên disk
                File dir = new File("./JudgeSystemData/Testcases/problem_" + pid);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            f.delete();
                        }
                    }
                    System.out.println("De " + pid + ": xoa file trong " + dir.getPath());
                }
            }
            System.out.println("\nXong! 3 de da duoc xoa sach testcase.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
