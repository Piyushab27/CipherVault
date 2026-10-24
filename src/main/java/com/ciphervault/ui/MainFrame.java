package com.ciphervault.ui;

import com.ciphervault.model.VaultException;
import com.ciphervault.service.AutoLockThread;
import com.ciphervault.service.VaultService;
import com.ciphervault.service.VaultThreads;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private static final long AUTO_LOCK_MILLIS = 2 * 60 * 1000L;
    private static final String LOGIN = "login";
    private static final String DASHBOARD = "dashboard";

    private final VaultService service = new VaultService();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel loginPanel;
    private final DashboardPanel dashboard;
    private final JMenuBar menuBar = new JMenuBar();
    private final JCheckBoxMenuItem showPasswords = new JCheckBoxMenuItem("Show passwords");
    private JMenuItem importItem;
    private JMenuItem exportItem;
    private JMenuItem logoutItem;
    private JMenuItem changePasswordItem;
    private JMenuItem securityItem;
    private AutoLockThread autoLock;

    public MainFrame() {
        super("CipherVault");
        loginPanel = new LoginPanel(service, this::startSession);
        dashboard = new DashboardPanel(this, service);
        dashboard.setShowAllListener(shown -> showPasswords.setSelected(shown));

        root.add(loginPanel, LOGIN);
        root.add(dashboard, DASHBOARD);
        setContentPane(root);
        buildMenus();
        setJMenuBar(menuBar);
        setSessionMenusEnabled(false);

        // any mouse or key activity restarts the auto-lock countdown
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (autoLock != null) {
                autoLock.touch();
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        setSize(900, 560);
        setLocationRelativeTo(null);
        Theme.apply(root);
    }

    private void buildMenus() {
        JMenu file = new JMenu("File");
        importItem = new JMenuItem("Import CSV");
        exportItem = new JMenuItem("Export CSV");
        logoutItem = new JMenuItem("Logout");
        JMenuItem exitItem = new JMenuItem("Exit");
        importItem.addActionListener(e -> dashboard.importCsv());
        exportItem.addActionListener(e -> dashboard.exportCsv());
        changePasswordItem = new JMenuItem("Change master password...");
        changePasswordItem.addActionListener(e -> changeMasterPassword());
        securityItem = new JMenuItem("Set security question...");
        securityItem.addActionListener(e -> setSecurityQuestion());
        logoutItem.addActionListener(e -> returnToLogin(null));
        exitItem.addActionListener(e -> confirmExit());
        file.add(importItem);
        file.add(exportItem);
        file.addSeparator();
        file.add(changePasswordItem);
        file.add(securityItem);
        file.add(logoutItem);
        file.add(exitItem);

        JMenu view = new JMenu("View");
        showPasswords.addActionListener(e -> dashboard.setShowAll(showPasswords.isSelected()));
        JRadioButtonMenuItem light = new JRadioButtonMenuItem("Light theme", true);
        JRadioButtonMenuItem dark = new JRadioButtonMenuItem("Dark theme");
        ButtonGroup themes = new ButtonGroup();
        themes.add(light);
        themes.add(dark);
        light.addActionListener(e -> setTheme(false));
        dark.addActionListener(e -> setTheme(true));
        view.add(showPasswords);
        view.addSeparator();
        view.add(light);
        view.add(dark);

        menuBar.add(file);
        menuBar.add(view);
    }

    private void setTheme(boolean dark) {
        Theme.setDark(dark);
        Theme.apply(root);
        Theme.apply(menuBar);
    }

    private void setSessionMenusEnabled(boolean enabled) {
        importItem.setEnabled(enabled);
        exportItem.setEnabled(enabled);
        logoutItem.setEnabled(enabled);
        changePasswordItem.setEnabled(enabled);
        securityItem.setEnabled(enabled);
        showPasswords.setEnabled(enabled);
    }

    /** Runs after a successful login. */
    private void startSession() {
        dashboard.reload();
        Theme.apply(dashboard);
        showPasswords.setSelected(false);
        setSessionMenusEnabled(true);
        cards.show(root, DASHBOARD);
        if (autoLock == null || !autoLock.isAlive()) {
            autoLock = new AutoLockThread(AUTO_LOCK_MILLIS,
                    () -> SwingUtilities.invokeLater(() -> returnToLogin("Locked after 2 minutes of no activity")));
            autoLock.start();
        }
        autoLock.unlockAndRestart();
    }

    private void returnToLogin(String notice) {
        if (autoLock != null) {
            autoLock.lockNow();
        }
        VaultThreads.interruptHiders();
        for (Window w : Window.getWindows()) {
            if (w != this && w.isDisplayable()) {
                w.dispose(); // close any open dialog so nothing stays on screen
            }
        }
        service.logout();
        dashboard.clear();
        setSessionMenusEnabled(false);
        loginPanel.reset(notice);
        cards.show(root, LOGIN);
    }

    private void changeMasterPassword() {
        JPasswordField current = new JPasswordField(16);
        JPasswordField next = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Current password:"));
        form.add(current);
        form.add(new JLabel("New password:"));
        form.add(next);
        form.add(new JLabel("Confirm new password:"));
        form.add(confirm);
        Theme.apply(form);

        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, "Change master password", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                break;
            }
            char[] oldPw = current.getPassword();
            char[] newPw = next.getPassword();
            char[] confirmPw = confirm.getPassword();
            try {
                if (!Arrays.equals(newPw, confirmPw)) {
                    JOptionPane.showMessageDialog(this, "The two new passwords do not match", "CipherVault",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                service.changeMasterPassword(oldPw, newPw);
                JOptionPane.showMessageDialog(this, "Master password changed.", "CipherVault",
                        JOptionPane.INFORMATION_MESSAGE);
                break;
            } catch (VaultException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "CipherVault", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(oldPw, '\0');
                Arrays.fill(newPw, '\0');
                Arrays.fill(confirmPw, '\0');
            }
        }
        current.setText("");
        next.setText("");
        confirm.setText("");
    }

    private void setSecurityQuestion() {
        JPasswordField master = new JPasswordField(16);
        JComboBox<String> question = new JComboBox<>(VaultService.SECURITY_QUESTIONS);
        JTextField answer = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Master password:"));
        form.add(master);
        form.add(new JLabel("Security question:"));
        form.add(question);
        form.add(new JLabel("Your answer:"));
        form.add(answer);
        Theme.apply(form);

        while (JOptionPane.showConfirmDialog(this, form, "Set security question", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            char[] pw = master.getPassword();
            try {
                service.setSecurityQuestion(pw, (String) question.getSelectedItem(), answer.getText());
                JOptionPane.showMessageDialog(this, "Security question saved. You can now use Forgot password? "
                        + "on the login screen.", "CipherVault", JOptionPane.INFORMATION_MESSAGE);
                break;
            } catch (VaultException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "CipherVault", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(pw, '\0');
            }
        }
        master.setText("");
        answer.setText("");
    }

    private void confirmExit() {
        int answer = JOptionPane.showConfirmDialog(this, "Close CipherVault?", "Exit", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            service.logout();
            VaultThreads.interruptAll();
            dispose();
            System.exit(0);
        }
    }
}
