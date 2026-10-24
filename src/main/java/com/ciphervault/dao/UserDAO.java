package com.ciphervault.dao;

import com.ciphervault.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    /** Returns null when no such user exists. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, master_hash, failed_attempts, security_question, "
                + "security_answer_hash, reset_attempts FROM users WHERE username = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("username"),
                            rs.getString("master_hash"), rs.getInt("failed_attempts"),
                            rs.getString("security_question"), rs.getString("security_answer_hash"),
                            rs.getInt("reset_attempts"));
                }
                return null;
            }
        }
    }

    /** Question and answer hash may be null. Throws SQLIntegrityConstraintViolationException when the username is taken. */
    public int insert(String username, String masterHash, String question, String answerHash) throws SQLException {
        String sql = "INSERT INTO users (username, master_hash, security_question, security_answer_hash) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, masterHash);
            ps.setString(3, question);
            ps.setString(4, answerHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Also clears both attempt counters, so a reset or change unlocks the account. */
    public void updateMasterHash(int userId, String masterHash) throws SQLException {
        String sql = "UPDATE users SET master_hash = ?, failed_attempts = 0, reset_attempts = 0 WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, masterHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateSecurity(int userId, String question, String answerHash) throws SQLException {
        String sql = "UPDATE users SET security_question = ?, security_answer_hash = ?, reset_attempts = 0 "
                + "WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, question);
            ps.setString(2, answerHash);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void updateFailedAttempts(int userId, int attempts) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateResetAttempts(int userId, int attempts) throws SQLException {
        String sql = "UPDATE users SET reset_attempts = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}
