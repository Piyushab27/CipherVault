package com.ciphervault.ui;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

/** Light and Dark theme, applied by walking the component tree. */
public final class Theme {
    private static boolean dark;

    private Theme() {
    }

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean value) {
        dark = value;
    }

    public static Color background() {
        return dark ? new Color(0x2B2B2B) : Color.WHITE;
    }

    public static Color foreground() {
        return dark ? new Color(0xE6E6E6) : Color.BLACK;
    }

    private static Color panel() {
        return dark ? new Color(0x3C3F41) : new Color(0xEEEEEE);
    }

    public static Color strengthColor(String strength) {
        if ("Weak".equals(strength)) {
            return dark ? new Color(0xFF6B6B) : new Color(0xC62828);
        }
        if ("Medium".equals(strength)) {
            return dark ? new Color(0xFFB74D) : new Color(0xE65100);
        }
        return dark ? new Color(0x81C784) : new Color(0x2E7D32);
    }

    public static void apply(Component c) {
        if (c instanceof JTable) {
            JTable t = (JTable) c;
            t.setBackground(background());
            t.setForeground(foreground());
            t.setGridColor(panel());
            t.setSelectionBackground(dark ? new Color(0x4B6EAF) : new Color(0xB8CFE5));
            t.setSelectionForeground(foreground());
            t.getTableHeader().setBackground(panel());
            t.getTableHeader().setForeground(foreground());
        } else if (c instanceof JTree) {
            JTree t = (JTree) c;
            t.setBackground(background());
            if (t.getCellRenderer() instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer r = (DefaultTreeCellRenderer) t.getCellRenderer();
                r.setBackgroundNonSelectionColor(background());
                r.setTextNonSelectionColor(foreground());
                r.setBackgroundSelectionColor(dark ? new Color(0x4B6EAF) : new Color(0xB8CFE5));
                r.setTextSelectionColor(foreground());
            }
        } else if (c instanceof JList) {
            c.setBackground(background());
            c.setForeground(foreground());
        } else if (c instanceof JTextComponent) {
            c.setBackground(background());
            c.setForeground(foreground());
            ((JTextComponent) c).setCaretColor(foreground());
        } else if (c instanceof JComponent) {
            c.setBackground(panel());
            if (!"strength".equals(c.getName())) {
                c.setForeground(foreground());
            }
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                apply(child);
            }
        }
        c.repaint();
    }
}
