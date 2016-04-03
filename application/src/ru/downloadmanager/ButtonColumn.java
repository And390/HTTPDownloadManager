package ru.downloadmanager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;


public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {

    private JTable table;
    private ActionListener action;
    private int mnemonic;

    private JButton button;
    private Object editorValue;

    public ButtonColumn(JTable table, ActionListener action, int column) {
        this.table = table;
        this.action = action;

        button = new JButton();
        button.setFocusPainted(false);
        button.addActionListener(this);
        button.setMargin(MainWindow.buttonMargin);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {

        this.editorValue = value;

        if (value == null) {
            return null;
        } else {
            button.setText(value.toString());
            button.setIcon(null);
        }

        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (value == null) {
            return null;
        } else {
            button.setText(value.toString());
            button.setIcon(null);
        }

        return button;
    }

    public void actionPerformed(ActionEvent e) {
        int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();

        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "" + row);
        action.actionPerformed(event);
    }


}
