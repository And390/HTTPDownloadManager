package ru.downloadmanager;


import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class ItemsPanel extends JScrollPane {

    private JTable table;
    private ArrayList<DownloadItem> itemsList = new ArrayList<>();

    public ItemsPanel(DownloadManager downloadManager) {
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        table = new JTable(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return itemsList.size();
            }

            @Override
            public int getColumnCount() {
                return 7;
            }

            @Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return "progress";
                    case 1:
                        return "received";
                    case 2:
                        return "total";
                    case 3:
                        return "file";
                    case 4:
                        return "URL";
                    case 5:
                        return "";
                    case 6:
                        return "";
                    default:
                        throw new IllegalArgumentException("columnIndex: " + columnIndex);
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                DownloadItem item = itemsList.get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        return item;
                    case 1:
                        return item.getReceived();
                    case 2:
                        return item.getTotal();
                    case 3:
                        return item.getFile().getName();
                    case 4:
                        return item.getUrl();
                    case 5:
                        return item.isDone() ? null : item.isActive() ? "stop" : "start";
                    case 6:
                        return "x";
                    default:
                        throw new IllegalArgumentException("columnIndex: " + columnIndex);
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 5 || columnIndex == 6;
            }
        }) {

            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                if (rowIndex >= 0) {
                    DownloadItem item = itemsList.get(rowIndex);
                    if (item.getError() != null) {
                        return item.getError().getMessage();
                    }
                }
                return null;
            }
        };

        table.getColumnModel().getColumn(0).setCellRenderer(new ProgressBarColumn());
        new ButtonColumn(table, (event) -> {
            int row = Integer.parseInt(event.getActionCommand());
            DownloadItem item = itemsList.get(row);
            if (item.isActive()) {
                downloadManager.stop(item.getId());
            } else {
                downloadManager.start(item.getId());
            }
        }, 5);
        new ButtonColumn(table, (event) -> {
            int row = Integer.parseInt(event.getActionCommand());
            DownloadItem item = itemsList.remove(row);
            downloadManager.remove(item.getId());
        }, 6);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        table.getColumnModel().getColumn(5).setMinWidth(50);
        table.getColumnModel().getColumn(5).setMaxWidth(50);
        table.getColumnModel().getColumn(6).setMinWidth(25);
        table.getColumnModel().getColumn(6).setMaxWidth(25);
        setViewportView(table);
    }

    public void add(DownloadItem item) {
        itemsList.add(item);
    }

    public void update(DownloadItem[] items) {
        itemsList.clear();
        itemsList.addAll(Arrays.asList(items));
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();

        for (int i = 0; i < table.getRowCount(); i++) {
            table.setRowHeight(i, MainWindow.LINE_HEIGHT);
        }
    }

}
