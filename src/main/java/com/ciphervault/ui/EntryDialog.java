package com.ciphervault.ui;

import com.ciphervault.model.Credential;
import com.ciphervault.model.VaultException;
import com.ciphervault.service.PasswordGenerator;
import com.ciphervault.service.StrengthChecker;
import com.ciphervault.service.VaultService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Add / Edit form. Pass null as the entry to add a new one. */
public class EntryDialog extends JDialog {
    private final VaultService service;
    private final Credential editing;
    private boolean saved;

    private final JTextField siteField = new JTextField(22);
    private final JTextField userField = new JTextField(22);
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JRadioButton randomRadio = new JRadioButton("Random password");
    private final JRadioButton typedRadio = new JRadioButton("I will type one", true);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JCheckBox showBox = new JCheckBox("Show");
    private final JLabel strengthLabel = new JLabel(" ");
    private final JTextField lengthField = new JTextField("12", 4);
    private final JCheckBox symbolsBox = new JCheckBox("Symbols", true);
    private final char defaultEcho = passwordField.getEchoChar();

    public EntryDialog(JFrame owner, VaultService service, Credential editing) {
        super(owner, editing == null ? "Add Entry" : "Edit Entry", true);
        this.service = service;
        this.editing = editing;

        try {
            for (String name : service.getCategories()) {
                categoryBox.addItem(name);
            }
        } catch (VaultException e) {
            JOptionPane.showMessageDialog(owner, e.getMessage(), "CipherVault", JOptionPane.ERROR_MESSAGE);
        }

        ButtonGroup mode = new ButtonGroup();
        mode.add(randomRadio);
        mode.add(typedRadio);
        strengthLabel.setName("strength");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        int row = 0;
        row = addRow(form, row, "Site:", siteField);
        row = addRow(form, row, "Username:", userField);
        row = addRow(form, row, "Category:", categoryBox);
        row = addRow(form, row, "Password:", modePanel());
        row = addRow(form, row, "", passwordRow());
        row = addRow(form, row, "Strength:", strengthLabel);
        row = addRow(form, row, "Generator:", generatorPanel());
        row = addRow(form, row, "Recent sites:", recentSites());

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                showStrength();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                showStrength();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                showStrength();
            }
        });
        randomRadio.addActionListener(e -> passwordField.setEditable(false));
        typedRadio.addActionListener(e -> passwordField.setEditable(true));
        showBox.addActionListener(e -> passwordField.setEchoChar(showBox.isSelected() ? (char) 0 : defaultEcho));
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        if (editing != null) {
            siteField.setText(editing.getSiteName());
            userField.setText(editing.getUsername());
            categoryBox.setSelectedItem(editing.getCategory());
            passwordField.setText(editing.getPassword());
        }
        showStrength();
        Theme.apply(getContentPane());
        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog and returns true when an entry was saved. */
    public boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private int addRow(JPanel form, int row, String label, java.awt.Component field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.gridy = row;
        g.gridx = 0;
        g.anchor = GridBagConstraints.NORTHEAST;
        form.add(new JLabel(label), g);
        g.gridx = 1;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        form.add(field, g);
        return row + 1;
    }

    private JPanel modePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.add(randomRadio);
        p.add(typedRadio);
        return p;
    }

    private JPanel passwordRow() {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(passwordField, BorderLayout.CENTER);
        p.add(showBox, BorderLayout.EAST);
        return p;
    }

    private JPanel generatorPanel() {
        JButton generate = new JButton("Generate");
        generate.addActionListener(e -> generate());
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Length (8 to 32):"));
        p.add(lengthField);
        p.add(symbolsBox);
        p.add(generate);
        return p;
    }

    private JScrollPane recentSites() {
        Set<String> sites = new LinkedHashSet<>();
        List<Credential> recent = new ArrayList<>(service.getEntries());
        recent.sort(Comparator.comparing(Credential::getUpdatedAt,
                Comparator.nullsFirst(Comparator.<java.time.LocalDate>naturalOrder())).reversed());
        for (Credential c : recent) {
            if (sites.size() < 6) {
                sites.add(c.getSiteName());
            }
        }
        DefaultListModel<String> model = new DefaultListModel<>();
        sites.forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(3);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                siteField.setText(list.getSelectedValue());
            }
        });
        return new JScrollPane(list);
    }

    private void generate() {
        try {
            int length = Integer.parseInt(lengthField.getText().trim());
            String password = PasswordGenerator.generate(length, symbolsBox.isSelected());
            randomRadio.setSelected(true);
            passwordField.setEditable(true);
            passwordField.setText(password);
            passwordField.setEditable(false);
            showBox.setSelected(true);
            passwordField.setEchoChar((char) 0);
        } catch (NumberFormatException ex) {
            error("Enter a number");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void showStrength() {
        char[] pw = passwordField.getPassword();
        try {
            if (pw.length == 0) {
                strengthLabel.setText(" ");
            } else {
                String strength = StrengthChecker.check(java.nio.CharBuffer.wrap(pw));
                strengthLabel.setText(strength);
                strengthLabel.setForeground(Theme.strengthColor(strength));
            }
        } finally {
            java.util.Arrays.fill(pw, '\0');
        }
    }

    private void save() {
        String password = new String(passwordField.getPassword());
        String category = (String) categoryBox.getSelectedItem();
        try {
            if (editing == null) {
                service.addEntry(siteField.getText(), userField.getText(), password, category);
            } else {
                service.updateEntry(editing, siteField.getText(), userField.getText(), password, category);
            }
            saved = true;
            dispose();
        } catch (VaultException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "CipherVault", JOptionPane.ERROR_MESSAGE);
    }
}
