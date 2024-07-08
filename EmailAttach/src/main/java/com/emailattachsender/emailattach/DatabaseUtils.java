package com.emailattachsender.emailattach;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class DatabaseUtils {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/database";
    private static final String USER = "dbuser"; // Substitua pelo usuário do banco de dados
    private static final String PASSWORD = "dbpass"; // Substitua pela senha do banco de dados

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("ssl", "false"); // Desative o SSL se não estiver usando

        return DriverManager.getConnection(DB_URL, props);
    }

    public static boolean isFileAlreadyDownloaded(String fileId) {
        String query = "SELECT COUNT(*) FROM DownloadedFiles WHERE FileId = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void logDownloadedFile(String fileId, LocalDateTime downloadTime) {
        String query = "INSERT INTO DownloadedFiles (FileId, DownloadTime) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileId);
            stmt.setTimestamp(2, Timestamp.valueOf(downloadTime));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
