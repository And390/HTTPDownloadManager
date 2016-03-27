package ru.downloadmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.LinkedHashMap;


public class MainWindow {

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainWindow();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private DownloadManager manager;

    private JFrame frame;
    private JTextField urlField;
    private JLabel urlLabel;
    private JButton addButton;
    private JProgressBar totalProgress;
    private JLabel threadsLabel;
    private JSpinner threadsField;
    private LinkedHashMap<Integer, ItemPanel> itemPanels = new LinkedHashMap<>();
    private JScrollPane scrollPane;
    private JPanel scrolledPanel;

    private final static Insets buttonMargin = new Insets(2, 4, 2, 4);

    private MainWindow() {
        //    create main window
        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setTitle("HTTP Download Manager");
        frame.getContentPane().setLayout(null);

        //    locate main window under the cursor
        frame.setSize(800, 600);

        int cursorX = MouseInfo.getPointerInfo().getLocation().x;
        int cursorY = MouseInfo.getPointerInfo().getLocation().y;
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        int x = cursorX - frame.getWidth() / 2;
        int y = cursorY - frame.getHeight() / 2;
        x = x < 0 ? 0 : x + frame.getWidth() > screenWidth ? screenWidth - frame.getWidth() : x;
        y = y < 0 ? 0 : y + frame.getHeight() > screenHeight ? screenHeight - frame.getHeight() : y;
        frame.setLocation(x, y);

        //    create components
        urlLabel = new JLabel("URL:");
        frame.getContentPane().add(urlLabel);

        urlField = new JTextField();
        frame.getContentPane().add(urlField);

        addButton = new JButton();
        frame.getContentPane().add(addButton);
        addButton.setText("add");
        addButton.setMargin(buttonMargin);

        totalProgress = new JProgressBar();
        frame.getContentPane().add(totalProgress);
        totalProgress.setMinimum(0);
        totalProgress.setMaximum(100);
        totalProgress.setStringPainted(true);

        threadsLabel = new JLabel("threads:");
        frame.getContentPane().add(threadsLabel);

        int DEFAULT_THREADS_COUNT = 2;
        threadsField = new JSpinner(new SpinnerNumberModel(DEFAULT_THREADS_COUNT, 1, 1000, 1));
        frame.getContentPane().add(threadsField);

        scrolledPanel = new JPanel();
        scrolledPanel.setLayout(null);
        scrollPane = new JScrollPane(scrolledPanel);
        frame.getContentPane().add(scrollPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        //    set event listeners
        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                onResize();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        addButton.addActionListener((e) -> addURL());
        urlField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addURL();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        threadsField.addChangeListener((e) -> manager.setThreadsCount((Integer)threadsField.getValue()));

        //    start download manager
        manager = new DownloadManager(DEFAULT_THREADS_COUNT, "download");

        Timer timer = new Timer(200, (e) -> updateStatus());
        timer.start();
        updateStatus();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                manager.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        //    show
        onResize();
        frame.setVisible(true);
    }

    // locate components on resize event
    private void onResize() {
        final int PADDING_X = 8;
        final int PADDING_Y = 8;
        final int SPACE_X = 8;
        final int SPACE_Y = 8;
        final int BIG_SPACE_Y = 16;
        final int LINE_HEIGHT = 24;
        final int BUTTON_WIDTH = 50;

        int width = frame.getContentPane().getWidth();
        int height = frame.getContentPane().getHeight();

        int y = PADDING_Y;

        urlLabel.setLocation(PADDING_X, y);
        urlLabel.setSize((int)(urlLabel.getPreferredSize().getWidth()), LINE_HEIGHT);

        addButton.setSize(BUTTON_WIDTH, LINE_HEIGHT);
        addButton.setLocation(width - PADDING_X - addButton.getWidth(), y);

        urlField.setLocation(urlLabel.getX() + urlLabel.getWidth() + SPACE_X, y);
        urlField.setSize(addButton.getX() - SPACE_X - urlField.getX(), LINE_HEIGHT);

        y += LINE_HEIGHT + SPACE_Y;
        int x = PADDING_X;

        threadsLabel.setLocation(x, y);
        threadsLabel.setSize((int) (threadsLabel.getPreferredSize().getWidth()), LINE_HEIGHT);
        x += threadsLabel.getWidth() + SPACE_X;

        threadsField.setLocation(x, y);
        threadsField.setSize(100, LINE_HEIGHT);
        x += threadsField.getWidth() + SPACE_X;

        totalProgress.setLocation(x, y);
        totalProgress.setSize(width - PADDING_X - x, LINE_HEIGHT);

        y += LINE_HEIGHT + BIG_SPACE_Y;

        scrollPane.setLocation(PADDING_X, y);
        scrollPane.setSize(width - PADDING_X - scrollPane.getX(), height - PADDING_Y - scrollPane.getY());

        int scrolledSize = itemPanels.size() * (LINE_HEIGHT + SPACE_Y) - SPACE_Y;
        boolean scroll = scrolledSize > scrollPane.getViewportBorderBounds().getHeight();
        scrollPane.setVerticalScrollBarPolicy(scroll ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrolledPanel.setSize((int) scrollPane.getViewportBorderBounds().getWidth() - (scroll ? SPACE_X : 0), scrolledSize);
        scrolledPanel.setLocation(0, 0);

        y = 0;
        for (ItemPanel itemPanel : itemPanels.values()) {
            itemPanel.setLocation(0, y);
            itemPanel.setSize(itemPanel.getParent().getWidth() - itemPanel.getX(), LINE_HEIGHT);

            itemPanel.removeButton.setSize(new Dimension((int) (itemPanel.removeButton.getPreferredSize().getWidth()), LINE_HEIGHT));
            itemPanel.removeButton.setLocation(itemPanel.getWidth() - itemPanel.removeButton.getWidth(), 0);

            itemPanel.startStopButton.setSize(new Dimension(BUTTON_WIDTH, LINE_HEIGHT));
            itemPanel.startStopButton.setLocation(itemPanel.removeButton.getX() - SPACE_X - itemPanel.startStopButton.getWidth(), 0);

            itemPanel.progressBar.setLocation(0, 0);
            itemPanel.progressBar.setSize(itemPanel.startStopButton.getX() - SPACE_X, itemPanel.getHeight());

            y += LINE_HEIGHT + SPACE_Y;
        }
    }

    public class ItemPanel extends JPanel {
        JProgressBar progressBar;
        JButton startStopButton;
        JButton removeButton;

        int id;
        String urlText;
        volatile boolean running;

        public ItemPanel() {

        }

        public void init(String file, String url, int id) {
            setLayout(null);

            progressBar = new JProgressBar();
            this.add(progressBar);
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);

            startStopButton = new JButton();
            this.add(startStopButton);
            startStopButton.setMargin(buttonMargin);
            setRunning(true, false);
            startStopButton.addActionListener((e) -> {
                if (running) {
                    manager.stop(id);
                } else {
                    manager.start(id);
                    setRunning(true, false);
                }
            });

            removeButton = new JButton();
            this.add(removeButton);
            removeButton.setText("x");
            removeButton.setMargin(buttonMargin);
            removeButton.addActionListener((e) -> {
                manager.remove(id);
                ItemPanel panel = itemPanels.remove(id);
                if (panel != null) {
                    panel.getParent().remove(panel);
                    onResize();
                }
            });

            this.id = id;
            this.urlText = file;
        }

        public void setRunning(boolean running, boolean done) {
            this.running = running;
            startStopButton.setText(running ? "stop" : "start");
            startStopButton.setEnabled(!done);
        }
    }

    private void addURL() {
        try {
            String url = urlField.getText().trim();
            if (url.length() > 0) {
                urlField.setText("");

                ItemPanel itemPanel = new ItemPanel();
                DownloadItem downloadItem = manager.add(url, (state) -> SwingUtilities.invokeLater(() -> itemPanel.setRunning(false, state==DownloadItemState.DONE)));

                itemPanel.init(downloadItem.getFile().getName(), url, downloadItem.getId());
                itemPanels.put(downloadItem.getId(), itemPanel);
                scrolledPanel.add(itemPanel);
                onResize();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), "IO error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        DownloadState state = manager.getState();

        boolean hasUncompleted = false;
        for (DownloadItem item : state.getItems()) {
            ItemPanel itemPanel = itemPanels.get(item.getId());
            if (itemPanel == null) {
                continue;
            }
            JProgressBar progressBar = itemPanel.progressBar;
            String statusText;
            if (item.getError() != null) {
                progressBar.setForeground(Color.red);
                progressBar.setValue(progressBar.getMaximum());
                statusText = getErrorText(item.getError());
            } else {
                progressBar.setForeground(Color.blue);
                statusText = setProgress(progressBar, item.getTotal(), item.getReceived(), item.isDone(), item.isStopped());
                hasUncompleted |= !item.isDone();
            }
            progressBar.setString(statusText + "  |  " + itemPanel.urlText);
        }

        boolean hasAny = state.getItems().length != 0;
        String statusText = setProgress(totalProgress, state.getTotal(), state.getReceived(), hasAny && !hasUncompleted, false);
        totalProgress.setString(hasAny ? statusText : "no files");
    }

    private static String setProgress(JProgressBar progressBar, long total, long received, boolean done, boolean stopped) {
        if (done) {
            progressBar.setValue(progressBar.getMaximum());
            return "complete (" + received + " b)";
        } else if (total == 0) {
            progressBar.setValue(0);
            return received + "/? b" + (stopped ? " (stopped)" : "");
        } else {
            progressBar.setValue(Math.round(received * (float) progressBar.getMaximum() / total));
            return String.format("%d/%d b (%d%%)", received, total, Math.round(received * 100f / total))
                     + (stopped ? " (stopped)" : "");
        }
    }

    private static String getErrorText(Throwable e) {
        return  e instanceof DownloadException || e instanceof IOException ? e.getMessage() : e.toString();
    }
}
