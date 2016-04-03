package ru.downloadmanager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;


public class ProgressBarColumn implements TableCellRenderer {

    static {

    }

    private ArrayList<JProgressBar> progressBars = new ArrayList<>();

    public ProgressBarColumn() {

    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        while (row >= progressBars.size()) {
            JProgressBar progressBar = new JProgressBar();
            progressBars.add(progressBar);
            progressBar.setStringPainted(true);
        }
        JProgressBar progressBar = progressBars.get(row);
        DownloadItem item = (DownloadItem)value;
        if (item.getError() != null) {
            progressBar.setForeground(new Color(200, 0, 0));
            progressBar.setValue(progressBar.getMaximum());
            progressBar.setString(item.getError().getMessage());
        } else if (item.isDone()) {
            progressBar.setForeground(new Color(163, 184, 204));
            progressBar.setValue(progressBar.getMaximum());
            progressBar.setString("100%");
        } else {
            float v = item.getReceived() / (float) item.getTotal();
            progressBar.setForeground(new Color(100, 200, 150));
            progressBar.setValue(Math.round(v * progressBar.getMaximum()));
            progressBar.setString((int)(v * 100) + "%");
        }
        return progressBar;
    }


}
