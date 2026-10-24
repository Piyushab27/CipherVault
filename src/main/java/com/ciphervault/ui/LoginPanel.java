package com.ciphervault.ui;

import com.ciphervault.model.AccountLockedException;
import com.ciphervault.model.VaultException;
import com.ciphervault.service.VaultService;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class LoginPanel extends JPanel {
    private final VaultService service;
    private final Runnable onLoggedIn;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JCheckBox showPassword = new JCheckBox("Show password");
    private final JLabel message = new JLabel(" ");
    private final char defaultEcho = passwordField.getEchoChar();

    public LoginPanel(VaultService service, Runnable onLoggedIn) {
        this.service = service;
        this.onLoggedIn = onLoggedIn;
        setLayout(new GridBagLayout());

        JLabel logo = new JLabel(new ImageIcon(drawLogo()));
        JLabel title = new JLabel("CipherVault");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel subtitle = new JLabel("Your passwords, behind one master password");
        message.setForeground(new Color(0xC62828));

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JButton forgotButton = new JButton("Forgot password?");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);
        buttons.add(forgotButton);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.gridx = 0;
        g.gridwidth = 2;
        add(logo, g);
        add(title, g);
        add(subtitle, g);
        g.gridwidth = 1;
        g.anchor = GridBagConstraints.EAST;
        g.gridy = 3;
        add(new JLabel("Username:"), g);
        g.gridy = 4;
        add(new JLabel("Master password:"), g);
        g.gridx = 1;
        g.anchor = GridBagConstraints.WEST;
        g.gridy = 3;
        add(usernameField, g);
        g.gridy = 4;
        add(passwordField, g);
        g.gridy = 5;
        add(showPassword, g);
        g.gridx = 0;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.CENTER;
        g.gridy = 6;
        add(buttons, g);
        g.gridy = 7;
        add(message, g);

        showPassword.addActionListener(e -> passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEcho));
        loginButton.addActionListener(e -> doLogin());
        registerButton.addActionListener(e -> doRegister());
        forgotButton.addActionListener(e -> doForgot());
        KeyAdapter enterToLogin = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doLogin();
                }
            }
        };
        usernameField.addKeyListener(enterToLogin);
        passwordField.addKeyListener(enterToLogin);
    }

    /** Called when the app returns to this screen (logout or auto-lock). */
    public void reset(String notice) {
        passwordField.setText("");
        showPassword.setSelected(false);
        passwordField.setEchoChar(defaultEcho);
        message.setText(notice == null ? " " : notice);
        passwordField.requestFocusInWindow();
    }

    private void doLogin() {
        message.setForeground(new Color(0xC62828));
        char[] password = passwordField.getPassword();
        try {
            service.login(usernameField.getText(), password);
            reset(null);
            onLoggedIn.run();
        } catch (AccountLockedException e) {
            message.setText(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Account locked", JOptionPane.ERROR_MESSAGE);
        } catch (VaultException e) {
            message.setText(e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            passwordField.setText("");
        }
    }

    private void doRegister() {
        JTextField user = new JTextField(16);
        JPasswordField pass = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);
        JComboBox<String> question = new JComboBox<>(VaultService.SECURITY_QUESTIONS);
        JTextField answer = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Username:"));
        form.add(user);
        form.add(new JLabel("Master password:"));
        form.add(pass);
        form.add(new JLabel("Confirm password:"));
        form.add(confirm);
        form.add(new JLabel("Security question:"));
        form.add(question);
        form.add(new JLabel("Your answer:"));
        form.add(answer);
        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Theme.apply(form);

        if (JOptionPane.showConfirmDialog(this, form, "Register", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        message.setForeground(new Color(0xC62828));
        char[] p1 = pass.getPassword();
        char[] p2 = confirm.getPassword();
        try {
            if (!Arrays.equals(p1, p2)) {
                message.setText("The two passwords do not match");
                return;
            }
            service.register(user.getText(), p1, (String) question.getSelectedItem(), answer.getText());
            usernameField.setText(user.getText().trim());
            message.setForeground(new Color(0x2E7D32));
            message.setText("Account created. You can log in now.");
            return;
        } catch (VaultException e) {
            message.setText(e.getMessage());
        } finally {
            Arrays.fill(p1, '\0');
            Arrays.fill(p2, '\0');
        }
    }

    /** Forgot master password: answer the security question, then choose a new master password. */
    private void doForgot() {
        message.setForeground(new Color(0xC62828));
        String username = JOptionPane.showInputDialog(this, "Username:", usernameField.getText());
        if (username == null) {
            return;
        }
        String questionText;
        try {
            questionText = service.getSecurityQuestion(username);
        } catch (VaultException e) {
            message.setText(e.getMessage());
            return;
        }
        if (questionText == null) {
            JOptionPane.showMessageDialog(this, "No security question is set for this account, so the password "
                    + "cannot be reset here.\nLog in and use File > Set security question next time.",
                    "Forgot password", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField answer = new JTextField(16);
        JPasswordField pass = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Question:"));
        form.add(new JLabel("<html><div style='width:170px'>" + questionText + "</div></html>"));
        form.add(new JLabel("Your answer:"));
        form.add(answer);
        form.add(new JLabel("New master password:"));
        form.add(pass);
        form.add(new JLabel("Confirm new password:"));
        form.add(confirm);
        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Theme.apply(form);

        while (JOptionPane.showConfirmDialog(this, form, "Forgot password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            char[] p1 = pass.getPassword();
            char[] p2 = confirm.getPassword();
            try {
                if (!Arrays.equals(p1, p2)) {
                    JOptionPane.showMessageDialog(this, "The two new passwords do not match", "Forgot password",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                service.resetMasterPassword(username, answer.getText(), p1);
                usernameField.setText(username.trim());
                message.setForeground(new Color(0x2E7D32));
                message.setText("Master password reset. You can log in now.");
                break;
            } catch (AccountLockedException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Forgot password", JOptionPane.ERROR_MESSAGE);
                break;
            } catch (VaultException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Forgot password", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(p1, '\0');
                Arrays.fill(p2, '\0');
            }
        }
    }

    private static BufferedImage drawLogo() {
        BufferedImage img = new BufferedImage(72, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1F4E79));
        g.setStroke(new BasicStroke(7f));
        g.drawArc(20, 6, 32, 40, 0, 180);
        g.fillRoundRect(12, 30, 48, 38, 10, 10);
        g.setColor(Color.WHITE);
        g.fillOval(31, 41, 10, 10);
        g.fillRect(34, 48, 4, 12);
        g.dispose();
        return img;
    }
}
