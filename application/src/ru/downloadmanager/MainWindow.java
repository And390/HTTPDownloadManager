package ru.downloadmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;


public class MainWindow {

    public static final Insets buttonMargin = new Insets(2, 4, 2, 4);
    public static final int LINE_HEIGHT = 24;

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                error.printStackTrace();
                JOptionPane.showMessageDialog(null, error.getMessage(), null, JOptionPane.ERROR_MESSAGE);
            });
            new MainWindow();
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
    private ItemsPanel itemsPanel;

    private MainWindow() {
        //    config
        File downloadDir = new File(ApplicationConfig.getRequired("downloadDirectory"));
        if (!downloadDir.exists()) {
            throw new RuntimeException("Directory doesn't exist: " + downloadDir.getPath());
        }
        if (!downloadDir.isDirectory()) {
            throw new RuntimeException("File is not a directory: " + downloadDir.getPath());
        }
        int defaultThreadsCount = ApplicationConfig.getRequiredInt("defaultThreadsCount");
        String userAgent = ApplicationConfig.getRequired("User-Agent");
        int redirectionLimit = ApplicationConfig.getRequiredInt("redirectionLimit");

        //    start download manager
        manager = new DownloadManager(defaultThreadsCount, downloadDir);
        manager.setUserAgent(userAgent);
        manager.setRedirectionLimit(redirectionLimit);

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

        threadsField = new JSpinner(new SpinnerNumberModel(defaultThreadsCount, 1, 1000, 1));
        frame.getContentPane().add(threadsField);

        itemsPanel = new ItemsPanel(manager);
        frame.getContentPane().add(itemsPanel);

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
        frame.setVisible(true);
        onResize();
    }

    // locate components on resize event
    private void onResize() {
        final int PADDING_X = 8;
        final int PADDING_Y = 8;
        final int SPACE_X = 8;
        final int SPACE_Y = 8;
        final int BIG_SPACE_Y = 16;
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

        itemsPanel.setLocation(PADDING_X, y);
        itemsPanel.setSize(width - PADDING_X - itemsPanel.getX(), height - PADDING_Y - itemsPanel.getY());
    }

    private void addURL() {
        try {
            String url = urlField.getText().trim();
            if (url.length() > 0) {
                urlField.setText("");
                DownloadItem item = manager.add(url, (state) -> {});
                itemsPanel.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.getMessage(), "IO error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        DownloadState state = manager.getState();

        boolean hasUncompleted = false;
        for (DownloadItem item : state.getItems()) {
            if (item.isActive() || item.isStopped()) {
                hasUncompleted = true;
                break;
            }
        }

        long received = state.getReceived();
        long total = state.getTotal();
        if (state.getItems().length == 0) {
            totalProgress.setValue(0);
            totalProgress.setString("no files");
        } else if (!hasUncompleted) {
            totalProgress.setValue(totalProgress.getMaximum());
            totalProgress.setString("complete (" + received + " b)");
        } else if (total == 0) {
            totalProgress.setValue(0);
            totalProgress.setString(received + "/? b");
        } else {
            totalProgress.setValue(Math.round(received * (float) totalProgress.getMaximum() / total));
            totalProgress.setString(String.format("%d/%d b (%d%%)", received, total, Math.round(received * 100f / total)));
        }

        itemsPanel.update(state.getItems());
    }
}
