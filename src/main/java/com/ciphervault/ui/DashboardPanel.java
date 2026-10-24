package com.ciphervault.ui;

import com.ciphervault.model.Credential;
import com.ciphervault.model.VaultException;
import com.ciphervault.service.CsvService;
import com.ciphervault.service.PasswordHider;
import com.ciphervault.service.PasswordUtils;
import com.ciphervault.service.VaultService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DashboardPanel extends JPanel {
    private final JFrame owner;
    private final VaultService service;
    private final CredentialTableModel model = new CredentialTableModel();
    private final JTable table = new JTable(model);
    private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("All entries")));
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> strengthFilter = new JComboBox<>(new String[] {"All", "Weak", "Medium", "Strong"});
    private final JLabel status = new JLabel(" ");

    private String selectedCategory;
    private boolean rebuildingTree;
    private boolean showAll;
    private Thread showAllHider;
    private final Set<Integer> revealed = new HashSet<>();
    private Consumer<Boolean> showAllListener = shown -> { };

    public DashboardPanel(JFrame owner, VaultService service) {
        this.owner = owner;
        this.service = service;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton add = new JButton("Add");
        JButton show = new JButton("Show");
        JButton checkReuse = new JButton("Check Reuse");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(new JLabel("Strength:"));
        top.add(strengthFilter);
        top.add(add);
        top.add(show);
        top.add(checkReuse);
        add(top, BorderLayout.NORTH);

        JScrollPane treePane = new JScrollPane(tree);
        treePane.setPreferredSize(new java.awt.Dimension(170, 100));
        add(treePane, BorderLayout.WEST);

        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(4).setCellRenderer(new StrengthRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        strengthFilter.addActionListener(e -> refresh());
        tree.addTreeSelectionListener(e -> {
            if (rebuildingTree || e.getPath() == null) {
                return;
            }
            Object node = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            selectedCategory = node instanceof CategoryNode ? ((CategoryNode) node).name : null;
            refresh();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });
        add.addActionListener(e -> openEntryDialog(null));
        show.addActionListener(e -> revealSelected());
        checkReuse.addActionListener(e -> checkReuse());

        JPopupMenu popup = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copy password");
        JMenuItem edit = new JMenuItem("Edit");
        JMenuItem delete = new JMenuItem("Delete");
        popup.add(copy);
        popup.add(edit);
        popup.add(delete);
        copy.addActionListener(e -> copySelected());
        edit.addActionListener(e -> openEntryDialog(selected()));
        delete.addActionListener(e -> deleteSelected());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selected() != null) {
                    openEntryDialog(selected());
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        popup.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    // ---- state ----

    /** Reloads everything shown from the service (after login or any change). */
    public void reload() {
        rebuildTree();
        refresh();
    }

    /** Forgets everything on screen (logout or auto-lock). */
    public void clear() {
        revealed.clear();
        showAll = false;
        selectedCategory = null;
        searchField.setText("");
        strengthFilter.setSelectedIndex(0);
        model.setRows(new ArrayList<>());
        rebuildingTree = true;
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("All entries")));
        rebuildingTree = false;
        status.setText(" ");
    }

    public void setShowAllListener(Consumer<Boolean> listener) {
        this.showAllListener = listener;
    }

    /** View > Show passwords. They hide again after 30 seconds. */
    public void setShowAll(boolean show) {
        showAll = show;
        if (showAllHider != null) {
            showAllHider.interrupt();
            showAllHider = null;
        }
        if (show) {
            showAllHider = PasswordHider.start(PasswordHider.DEFAULT_SECONDS, () -> SwingUtilities.invokeLater(() -> {
                showAll = false;
                showAllListener.accept(false);
                model.fireTableDataChanged();
            }));
        }
        model.fireTableDataChanged();
    }

    private void rebuildTree() {
        Map<String, Integer> counts = service.categoryCounts();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("All entries (" + service.getEntries().size() + ")");
        DefaultMutableTreeNode toSelect = root;
        try {
            for (String name : service.getCategories()) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new CategoryNode(name, counts.getOrDefault(name, 0)));
                root.add(node);
                if (name.equals(selectedCategory)) {
                    toSelect = node;
                }
            }
        } catch (VaultException e) {
            showError(e.getMessage());
        }
        rebuildingTree = true;
        tree.setModel(new DefaultTreeModel(root));
        tree.setSelectionPath(new TreePath(toSelect.getPath()));
        rebuildingTree = false;
        Theme.apply(tree);
    }

    private void refresh() {
        List<Credential> rows = service.search(searchField.getText(), (String) strengthFilter.getSelectedItem(),
                selectedCategory);
        model.setRows(rows);
        showSelectedDetails();
    }

    private void showSelectedDetails() {
        Credential c = selected();
        if (c != null) {
            status.setText(c.getDetails());
            return;
        }
        int reused = (int) service.getEntries().stream().filter(Credential::isReused).count();
        status.setText(String.format("%d shown  |  %.0f%% strong  |  %d reused", model.getRowCount(),
                service.percentStrong(), reused));
    }

    private Credential selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.getRow(table.convertRowIndexToModel(row));
    }

    // ---- actions ----

    private void openEntryDialog(Credential existing) {
        if (new EntryDialog(owner, service, existing).showDialog()) {
            reload();
        }
    }

    private void deleteSelected() {
        Credential c = selected();
        if (c == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(owner, "Delete the entry for " + c.getSiteName() + "?",
                "Delete entry", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                service.deleteEntry(c);
                reload();
            } catch (VaultException e) {
                showError(e.getMessage());
            }
        }
    }

    private void revealSelected() {
        Credential c = selected();
        if (c == null) {
            showError("Select a row first");
            return;
        }
        int id = c.getId();
        revealed.add(id);
        model.fireTableDataChanged();
        PasswordHider.start(PasswordHider.DEFAULT_SECONDS, () -> SwingUtilities.invokeLater(() -> {
            revealed.remove(id);
            model.fireTableDataChanged();
        }));
    }

    private void copySelected() {
        Credential c = selected();
        if (c == null) {
            return;
        }
        String password = c.getPassword();
        Clipboard clipboard = getToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(password), null);
        // the clipboard is cleared after 30 seconds if it still holds this password
        PasswordHider.start(PasswordHider.DEFAULT_SECONDS, () -> {
            try {
                if (password.equals(clipboard.getData(DataFlavor.stringFlavor))) {
                    clipboard.setContents(new StringSelection(""), null);
                }
            } catch (Exception ignored) {
                // clipboard holds something else or is unavailable
            }
        });
        status.setText("Password copied. The clipboard is cleared in 30 seconds.");
    }

    private void checkReuse() {
        List<Credential> all = service.getEntries();
        int flagged = VaultService.markReused(all);
        model.fireTableDataChanged();
        JOptionPane.showMessageDialog(owner, flagged == 0 ? "No reused passwords found."
                : flagged + " entries share a password with another entry. They are marked in the Reused column.",
                "Check Reuse", JOptionPane.INFORMATION_MESSAGE);
    }

    public void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import CSV (" + CsvService.IMPORT_HEADER + ")");
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            CsvService.ParseResult parsed = CsvService.importEntries(chooser.getSelectedFile());
            int added = service.importEntries(parsed.entries());
            reload();
            JOptionPane.showMessageDialog(owner, added + " entries imported, "
                    + (parsed.entries().size() - added + parsed.skipped()) + " skipped.");
        } catch (IOException e) {
            showError("Could not read the file: " + e.getMessage());
        } catch (VaultException e) {
            reload();
            showError(e.getMessage());
        }
    }

    public void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export stats CSV (no passwords are written)");
        chooser.setSelectedFile(new File("vault_stats.csv"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file.exists() && JOptionPane.showConfirmDialog(owner, file.getName() + " already exists. Replace it?",
                "Export CSV", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.exportStats(file);
            JOptionPane.showMessageDialog(owner, "File saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Could not write the file: " + e.getMessage());
        }
    }

    private void showError(String text) {
        JOptionPane.showMessageDialog(owner, text, "CipherVault", JOptionPane.ERROR_MESSAGE);
    }

    // ---- inner classes ----

    private static final class CategoryNode {
        private final String name;
        private final int count;

        private CategoryNode(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public String toString() {
            return name + " (" + count + ")";
        }
    }

    private static final class StrengthRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            c.setForeground(Theme.strengthColor(String.valueOf(value)));
            return c;
        }
    }

    /** MVC: this is the table model; the panel is the view and its listeners are the controller. */
    private final class CredentialTableModel extends AbstractTableModel {
        private final String[] columns = {"Site", "Username", "Password", "Category", "Strength", "Updated", "Reused"};
        private List<Credential> rows = new ArrayList<>();

        void setRows(List<Credential> newRows) {
            rows = new ArrayList<>(newRows);
            fireTableDataChanged();
        }

        Credential getRow(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Credential c = rows.get(row);
            switch (column) {
                case 0:
                    return c.getSiteName();
                case 1:
                    return c.getUsername();
                case 2:
                    return showAll || revealed.contains(c.getId()) ? c.getPassword() : PasswordUtils.MASK;
                case 3:
                    return c.getCategory();
                case 4:
                    return c.getStrength();
                case 5:
                    return c.getUpdatedAt() == null ? "" : c.getUpdatedAt().toString();
                default:
                    return c.isReused() ? "Yes" : "";
            }
        }
    }
}
