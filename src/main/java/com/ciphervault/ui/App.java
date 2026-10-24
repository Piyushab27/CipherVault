package com.ciphervault.ui;

import com.ciphervault.dao.DBConnection;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Swing's own look and feel: the same on every OS
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // the default look and feel still works
            }
            try {
                DBConnection.initialise();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Could not set up the database:\n" + e.getMessage()
                                + "\n\nCheck that MySQL is running and the CV_DB_URL, CV_DB_USER and CV_DB_PASS settings are right.",
                        "CipherVault", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new MainFrame().setVisible(true);
        });
    }
}
