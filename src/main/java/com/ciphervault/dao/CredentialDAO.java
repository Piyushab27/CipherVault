package com.ciphervault.dao;

import com.ciphervault.model.BankCredential;
import com.ciphervault.model.Credential;
import com.ciphervault.service.PasswordUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Saved passwords are stored encoded (see PasswordUtils); callers only ever see the plain text. */
public class CredentialDAO {

    public List<Credential> findByUser(int userId) throws SQLException {
        String sql = "SELECT c.cred_id, c.site_name, c.username, c.password, c.strength, c.updated_at, cat.name "
                + "FROM credentials c LEFT JOIN categories cat ON c.category_id = cat.category_id "
                + "WHERE c.user_id = ? ORDER BY c.site_name";
        List<Credential> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("name");
                    String password = PasswordUtils.decode(rs.getString("password"));
                    Credential c = "Banking".equals(category)
                            ? new BankCredential(rs.getString("site_name"), rs.getString("username"), password, "Bank account")
                            : new Credential(rs.getString("site_name"), rs.getString("username"), password,
                                    category == null ? "Other" : category);
                    c.setId(rs.getInt("cred_id"));
                    c.setStrength(rs.getString("strength"));
                    Date updated = rs.getDate("updated_at");
                    c.setUpdatedAt(updated == null ? null : updated.toLocalDate());
                    result.add(c);
                }
            }
        }
        return result;
    }

    /** category name -> id, in id order. */
    public Map<String, Integer> findCategories() throws SQLException {
        Map<String, Integer> categories = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT category_id, name FROM categories ORDER BY category_id")) {
            while (rs.next()) {
                categories.put(rs.getString("name"), rs.getInt("category_id"));
            }
        }
        return categories;
    }

    /** Calls the add_credential stored procedure and returns the new cred_id. */
    public int insert(int userId, int categoryId, Credential c) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall("{call add_credential(?, ?, ?, ?, ?, ?, ?)}")) {
            cs.setInt(1, userId);
            cs.setInt(2, categoryId);
            cs.setString(3, c.getSiteName());
            cs.setString(4, c.getUsername());
            cs.setString(5, PasswordUtils.encode(c.getPassword()));
            cs.setString(6, c.getStrength());
            cs.registerOutParameter(7, Types.INTEGER);
            cs.execute();
            return cs.getInt(7);
        }
    }

    /** Edit runs as a transaction: commit when the row was updated, rollback on any failure. */
    public void update(int userId, int categoryId, Credential c) throws SQLException {
        String sql = "UPDATE credentials SET category_id = ?, site_name = ?, username = ?, password = ?, strength = ? "
                + "WHERE cred_id = ? AND user_id = ?";
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, categoryId);
                ps.setString(2, c.getSiteName());
                ps.setString(3, c.getUsername());
                ps.setString(4, PasswordUtils.encode(c.getPassword()));
                ps.setString(5, c.getStrength());
                ps.setInt(6, c.getId());
                ps.setInt(7, userId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Entry not found");
                }
            }
            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                    // connection is already broken; the server rolls back on its own
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                    // nothing left to do
                }
            }
        }
    }

    public void delete(int userId, int credId) throws SQLException {
        String sql = "DELETE FROM credentials WHERE cred_id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, credId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}
