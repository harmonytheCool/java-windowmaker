import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Random;
import javax.sound.sampled.*;

public class HarmonysWindowmaker extends JFrame {

    private final Random random = new Random();
    private final int safeDistance = 150;

    private int currentX, currentY;
    private int targetX, targetY;
    private static final int GLIDE_SPEED = 6;

    private String currentAudioPath = "";
    private boolean isSoundPlaying = false;

    private String fontFam = "Arial";
    private int baseFontSize = 12;

    private Color bgColor = new Color(0, 0, 0);
    private Color titleColor = Color.WHITE;
    private Color bodyColor = new Color(200, 200, 200);

    private Image backgroundImage = null;

    private final int windowWidth = 600;
    private final int windowHeight = 200;
    private final int sidebarExpandedWidth = 180;

    private JLayeredPane layeredPane;
    private JPanel windowContentPanel;

    private JPanel sidebarPanel;
    private JPanel sidebarContents;
    private JScrollPane sidebarScrollPane;

    private JLabel iconLabel, titleLabel, descLabel;
    private JButton okButton;

    private boolean sidebarOpen = false;
    private boolean buttonVisible = true;

    private boolean dvdMode = false;

    // Whether the window runs away from the mouse.
    private boolean runawayEnabled = true;

    // Emergency stop.
    private boolean everythingStopped = false;

    // Window dragging.
    private Point dragStart = null;

    private int dvdVelocityX = 5;
    private int dvdVelocityY = 5;

    private Clip bgmClip;

    // Timer is a field so STOP EVERYTHING can stop it.
    private Timer mainLoopTimer;

    public HarmonysWindowmaker() {

        setUndecorated(true);
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        Point initialPos = getLocation();

        currentX = initialPos.x;
        currentY = initialPos.y;
        targetX = currentX;
        targetY = currentY;

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(
            new Dimension(
                windowWidth,
                windowHeight
            )
        );

        windowContentPanel =
            new JPanel(new GridBagLayout()) {

                @Override
                protected void paintComponent(
                    Graphics g
                ) {

                    super.paintComponent(g);

                    if (backgroundImage != null) {

                        g.drawImage(
                            backgroundImage,
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            this
                        );
                    }
                }
            };

        windowContentPanel.setBounds(
            15,
            0,
            windowWidth - 15,
            windowHeight
        );

        windowContentPanel.setBorder(
            BorderFactory.createEmptyBorder(
                20,
                25,
                20,
                25
            )
        );

        GridBagConstraints gbc =
            new GridBagConstraints();

        gbc.fill =
            GridBagConstraints.BOTH;

        gbc.insets =
            new Insets(
                0,
                10,
                0,
                10
            );

        iconLabel =
            new JLabel(
                UIManager.getIcon(
                    "OptionPane.warningIcon"
                )
            );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.0;

        windowContentPanel.add(
            iconLabel,
            gbc
        );

        JPanel textPanel =
            new JPanel(
                new GridLayout(
                    2,
                    1,
                    0,
                    5
                )
            );

        textPanel.setOpaque(false);

        titleLabel =
            new JLabel(
                "DEVICE_ERROR"
            );

        descLabel =
            new JLabel(
                "<html>CHOOSE YOUR TEXT WITH THE SIDEBAR</html>"
            );

        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;

        windowContentPanel.add(
            textPanel,
            gbc
        );

        okButton =
            new JButton(
                "PROCEED"
            );

        okButton.setFocusable(false);
        okButton.setEnabled(false);

        JPanel buttonPanel =
            new JPanel(
                new FlowLayout(
                    FlowLayout.RIGHT
                )
            );

        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        windowContentPanel.add(
            buttonPanel,
            gbc
        );

        /*
         * WINDOW DRAGGING
         *
         * The window is undecorated, so this gives the main
         * content area normal click-and-drag behavior.
         *
         * Buttons are deliberately excluded so they remain
         * clickable normally.
         */
        MouseAdapter windowDrag =
            new MouseAdapter() {

                @Override
                public void mousePressed(
                    MouseEvent e
                ) {

                    if (
                        everythingStopped ||
                        dvdMode
                    ) {
                        return;
                    }

                    dragStart =
                        e.getPoint();
                }

                @Override
                public void mouseDragged(
                    MouseEvent e
                ) {

                    if (
                        everythingStopped ||
                        dvdMode ||
                        dragStart == null
                    ) {
                        return;
                    }

                    Point location =
                        getLocation();

                    int newX =
                        location.x +
                        e.getX() -
                        dragStart.x;

                    int newY =
                        location.y +
                        e.getY() -
                        dragStart.y;

                    setLocation(
                        newX,
                        newY
                    );

                    currentX = newX;
                    currentY = newY;

                    targetX = newX;
                    targetY = newY;
                }

                @Override
                public void mouseReleased(
                    MouseEvent e
                ) {

                    dragStart = null;
                }
            };

        installDragListeners(
            windowContentPanel,
            windowDrag
        );

        sidebarPanel =
            new JPanel(
                new BorderLayout()
            );

        sidebarPanel.setBackground(
            new Color(
                20,
                20,
                20
            )
        );

        sidebarPanel.setBorder(
            BorderFactory.createMatteBorder(
                0,
                0,
                0,
                2,
                Color.DARK_GRAY
            )
        );

        sidebarPanel.setBounds(
            0,
            0,
            15,
            windowHeight
        );

        sidebarContents =
            new JPanel();

        sidebarContents.setLayout(
            new BoxLayout(
                sidebarContents,
                BoxLayout.Y_AXIS
            )
        );

        sidebarContents.setBackground(
            new Color(
                20,
                20,
                20
            )
        );

        sidebarScrollPane =
            new JScrollPane(
                sidebarContents
            );

        sidebarScrollPane.setBorder(null);

        sidebarScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        sidebarScrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        sidebarScrollPane
            .getViewport()
            .setBackground(
                new Color(
                    20,
                    20,
                    20
                )
            );

        sidebarScrollPane.setOpaque(false);

        sidebarScrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(12);

        sidebarScrollPane
            .getVerticalScrollBar()
            .setUI(
                new BasicScrollBarUI() {

                    private final Color TRACK =
                        new Color(
                            20,
                            20,
                            20
                        );

                    private final Color THUMB =
                        new Color(
                            90,
                            90,
                            90
                        );

                    private final Color THUMB_HOVER =
                        new Color(
                            140,
                            140,
                            140
                        );

                    @Override
                    protected void configureScrollBarColors() {

                        thumbColor =
                            THUMB;

                        trackColor =
                            TRACK;
                    }

                    @Override
                    protected JButton createDecreaseButton(
                        int orientation
                    ) {

                        return createInvisibleButton();
                    }

                    @Override
                    protected JButton createIncreaseButton(
                        int orientation
                    ) {

                        return createInvisibleButton();
                    }

                    private JButton createInvisibleButton() {

                        JButton button =
                            new JButton();

                        button.setPreferredSize(
                            new Dimension(
                                0,
                                0
                            )
                        );

                        button.setMinimumSize(
                            new Dimension(
                                0,
                                0
                            )
                        );

                        button.setMaximumSize(
                            new Dimension(
                                0,
                                0
                            )
                        );

                        button.setOpaque(false);
                        button.setContentAreaFilled(false);
                        button.setBorderPainted(false);

                        return button;
                    }

                    @Override
                    protected void paintTrack(
                        Graphics g,
                        JComponent c,
                        Rectangle trackBounds
                    ) {

                        Graphics2D g2 =
                            (Graphics2D)
                                g.create();

                        g2.setColor(
                            TRACK
                        );

                        g2.fillRect(
                            trackBounds.x,
                            trackBounds.y,
                            trackBounds.width,
                            trackBounds.height
                        );

                        g2.dispose();
                    }

                    @Override
                    protected void paintThumb(
                        Graphics g,
                        JComponent c,
                        Rectangle thumbBounds
                    ) {

                        if (
                            thumbBounds.isEmpty() ||
                            !scrollbar.isEnabled()
                        ) {
                            return;
                        }

                        Graphics2D g2 =
                            (Graphics2D)
                                g.create();

                        g2.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                        );

                        Color color =
                            isThumbRollover()
                                ? THUMB_HOVER
                                : THUMB;

                        g2.setColor(
                            color
                        );

                        int x =
                            thumbBounds.x + 2;

                        int y =
                            thumbBounds.y + 1;

                        int width =
                            Math.max(
                                1,
                                thumbBounds.width - 4
                            );

                        int height =
                            Math.max(
                                1,
                                thumbBounds.height - 2
                            );

                        g2.fillRect(
                            x,
                            y,
                            width,
                            height
                        );

                        g2.dispose();
                    }

                    @Override
                    protected void setThumbBounds(
                        int x,
                        int y,
                        int width,
                        int height
                    ) {

                        super.setThumbBounds(
                            x,
                            y,
                            width,
                            height
                        );

                        scrollbar.repaint();
                    }
                }
            );

        sidebarScrollPane
            .getVerticalScrollBar()
            .setPreferredSize(
                new Dimension(
                    10,
                    0
                )
            );

        sidebarPanel.add(
            sidebarScrollPane,
            BorderLayout.CENTER
        );

        setupSidebarButtons();

        MouseAdapter sidebarHoverEngine =
            new MouseAdapter() {

                @Override
                public void mouseEntered(
                    MouseEvent e
                ) {

                    if (!sidebarOpen) {

                        sidebarOpen = true;

                        layeredPane.setLayer(
                            sidebarPanel,
                            JLayeredPane.DRAG_LAYER
                        );

                        sidebarPanel.setBounds(
                            0,
                            0,
                            sidebarExpandedWidth,
                            windowHeight
                        );

                        windowContentPanel.setBounds(
                            sidebarExpandedWidth,
                            0,
                            windowWidth -
                                sidebarExpandedWidth,
                            windowHeight
                        );

                        toggleSidebarComponentsVisibility(
                            true
                        );

                        refreshPanels();
                    }
                }

                @Override
                public void mouseExited(
                    MouseEvent e
                ) {

                    if (
                        sidebarOpen &&
                        !sidebarPanel
                            .getBounds()
                            .contains(e.getPoint())
                    ) {

                        sidebarOpen = false;

                        layeredPane.setLayer(
                            sidebarPanel,
                            JLayeredPane.PALETTE_LAYER
                        );

                        sidebarPanel.setBounds(
                            0,
                            0,
                            15,
                            windowHeight
                        );

                        windowContentPanel.setBounds(
                            15,
                            0,
                            windowWidth - 15,
                            windowHeight
                        );

                        toggleSidebarComponentsVisibility(
                            false
                        );

                        refreshPanels();
                    }
                }
            };

        sidebarPanel.addMouseListener(
            sidebarHoverEngine
        );

        layeredPane.add(
            windowContentPanel,
            JLayeredPane.DEFAULT_LAYER
        );

        layeredPane.add(
            sidebarPanel,
            JLayeredPane.PALETTE_LAYER
        );

        setContentPane(
            layeredPane
        );

   
        getRootPane()
            .getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
            )
            .put(
                KeyStroke.getKeyStroke(
                    "ctrl I"
                ),
                "stopDVD"
            );

        getRootPane()
            .getActionMap()
            .put(
                "stopDVD",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(
                        java.awt.event.ActionEvent e
                    ) {

                        stopDVDMode();
                    }
                }
            );

        applyStyles();

        setVisible(true);

      
        mainLoopTimer =
            new Timer(
                16,
                e -> {

                    if (
                        everythingStopped
                    ) {
                        return;
                    }

                    if (dvdMode) {

                        Dimension screenSize =
                            Toolkit
                                .getDefaultToolkit()
                                .getScreenSize();

                        currentX +=
                            dvdVelocityX;

                        currentY +=
                            dvdVelocityY;

                        if (
                            currentX <= 0 ||
                            currentX + windowWidth >=
                                screenSize.width
                        ) {

                            dvdVelocityX =
                                -dvdVelocityX;
                        }

                        if (
                            currentY <= 0 ||
                            currentY + windowHeight >=
                                screenSize.height
                        ) {

                            dvdVelocityY =
                                -dvdVelocityY;
                        }

                        setLocation(
                            currentX,
                            currentY
                        );

                    } else {

                        int dx =
                            targetX -
                            currentX;

                        int dy =
                            targetY -
                            currentY;

                        if (
                            Math.abs(dx) > 1 ||
                            Math.abs(dy) > 1
                        ) {

                            currentX +=
                                dx /
                                GLIDE_SPEED;

                            currentY +=
                                dy /
                                GLIDE_SPEED;

                            setLocation(
                                currentX,
                                currentY
                            );
                        }
                    }
                }
            );

        mainLoopTimer.start();

       
        Thread globalMouseThread =
            new Thread(
                () -> {

                    while (true) {

                        try {

                            evaluateFleeing();

                            Thread.sleep(30);

                        } catch (
                            InterruptedException ex
                        ) {

                            break;
                        }
                    }
                }
            );

        globalMouseThread.setDaemon(
            true
        );

        globalMouseThread.start();
    }


    private void installDragListeners(
        Component component,
        MouseAdapter dragListener
    ) {

        if (
            component instanceof JButton
        ) {
            return;
        }

        component.addMouseListener(
            dragListener
        );

        component.addMouseMotionListener(
            dragListener
        );

        if (
            component instanceof Container
        ) {

            Component[] children =
                (
                    (Container)
                        component
                ).getComponents();

            for (
                Component child :
                children
            ) {

                installDragListeners(
                    child,
                    dragListener
                );
            }
        }
    }

    private void refreshPanels() {

        sidebarPanel.revalidate();
        sidebarPanel.repaint();

        sidebarScrollPane.revalidate();
        sidebarScrollPane.repaint();

        sidebarContents.revalidate();
        sidebarContents.repaint();

        windowContentPanel.revalidate();
        windowContentPanel.repaint();
    }

    private void setupSidebarButtons() {

        Dimension btnSize =
            new Dimension(
                160,
                20
            );

        JButton btnText =
            createSidebarBtn(
                "(Window Text)",
                btnSize
            );

        btnText.addActionListener(
            e -> openTextSettings()
        );

        JButton btnBtn =
            createSidebarBtn(
                "(Button Text)",
                btnSize
            );

        btnBtn.addActionListener(
            e -> openButtonSettings()
        );

        JButton toggleBtnVisibility =
            createSidebarBtn(
                "(Toggle Button)",
                btnSize
            );

        toggleBtnVisibility.addActionListener(
            e -> toggleMainButtonVisibility()
        );

        JButton btnFont =
            createSidebarBtn(
                "(Font)",
                btnSize
            );

        btnFont.addActionListener(
            e -> openFontSettings()
        );

        JButton btnColor =
            createSidebarBtn(
                "(Color)",
                btnSize
            );

        btnColor.addActionListener(
            e -> openColorSettings()
        );

        JButton btnTextColor =
            createSidebarBtn(
                "(Text Color)",
                btnSize
            );

        btnTextColor.addActionListener(
            e -> openTextColorPicker()
        );

        JButton btnBg =
            createSidebarBtn(
                "(BG)",
                btnSize
            );

        btnBg.addActionListener(
            e -> openBackgroundImagePicker()
        );

        JButton btnSprite =
            createSidebarBtn(
                "(Icon)",
                btnSize
            );

        btnSprite.addActionListener(
            e -> openImagePicker()
        );

        JButton btnSound =
            createSidebarBtn(
                "(Audio)",
                btnSize
            );

        btnSound.addActionListener(
            e -> openAudioPicker()
        );

        JButton btnDVD =
            createSidebarBtn(
                "(DVD Mode)",
                btnSize
            );

        btnDVD.addActionListener(
            e -> startDVDMode()
        );

        JButton btnStopDVD =
            createSidebarBtn(
                "(Stop DVD)",
                btnSize
            );

        btnStopDVD.addActionListener(
            e -> stopDVDMode()
        );

        /*
         * RUNAWAY TOGGLE
         */
        JButton btnRunaway =
            createSidebarBtn(
                "(Runaway: ON)",
                btnSize
            );

        btnRunaway.addActionListener(
            e -> {

                runawayEnabled =
                    !runawayEnabled;

                btnRunaway.setText(
                    runawayEnabled
                        ? "(Runaway: ON)"
                        : "(Runaway: OFF)"
                );
            }
        );

        /*
         * EMERGENCY STOP
         */
        JButton btnStopEverything =
            createSidebarBtn(
                "(STOP EVERYTHING NOW)",
                btnSize
            );

        btnStopEverything.setBackground(
            new Color(
                80,
                0,
                0
            )
        );

        btnStopEverything.addActionListener(
            e -> stopEverythingNow()
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                5
            )
        );

        sidebarContents.add(
            btnText
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnBtn
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            toggleBtnVisibility
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnFont
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnColor
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnTextColor
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnBg
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnSprite
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnSound
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnDVD
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnStopDVD
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnRunaway
        );

        sidebarContents.add(
            Box.createVerticalStrut(
                2
            )
        );

        sidebarContents.add(
            btnStopEverything
        );

        toggleSidebarComponentsVisibility(
            false
        );
    }

    private JButton createSidebarBtn(
        String label,
        Dimension size
    ) {

        JButton btn =
            new JButton(
                label
            );

        btn.setMaximumSize(
            size
        );

        btn.setPreferredSize(
            size
        );

        btn.setMinimumSize(
            size
        );

        btn.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );

        btn.setFont(
            new Font(
                "Courier New",
                Font.BOLD,
                11
            )
        );

        btn.setFocusable(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);

        btn.setBackground(
            Color.BLACK
        );

        btn.setForeground(
            Color.WHITE
        );

        return btn;
    }

    private void toggleSidebarComponentsVisibility(
        boolean visible
    ) {

        for (
            Component c :
            sidebarContents.getComponents()
        ) {

            if (
                c instanceof JButton
            ) {

                c.setVisible(
                    visible
                );
            }
        }

        sidebarContents.revalidate();
        sidebarContents.repaint();

        sidebarScrollPane.setVisible(
            true
        );
    }

    private void toggleMainButtonVisibility() {

        buttonVisible =
            !buttonVisible;

        okButton.setVisible(
            buttonVisible
        );

        windowContentPanel.revalidate();
        windowContentPanel.repaint();
    }

    public void startDVDMode() {

        if (
            !dvdMode &&
            !everythingStopped
        ) {

            JFileChooser chooser =
                new JFileChooser();

            chooser.setDialogTitle(
                "Select WAV Background Music Track for DVD Mode"
            );

            chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                    "WAV Audio Files (*.wav)",
                    "wav"
                )
            );

            if (
                chooser.showOpenDialog(this) ==
                JFileChooser.APPROVE_OPTION
            ) {

                currentAudioPath =
                    chooser
                        .getSelectedFile()
                        .getAbsolutePath();

                dvdMode = true;

                playMusicLoop();
            }
        }
    }

    private void playMusicLoop() {

        try {

            File audioFile =
                new File(
                    currentAudioPath
                );

            if (
                audioFile.exists()
            ) {

                AudioInputStream audioStream =
                    AudioSystem.getAudioInputStream(
                        audioFile
                    );

                bgmClip =
                    AudioSystem.getClip();

                bgmClip.open(
                    audioStream
                );

                bgmClip.loop(
                    Clip.LOOP_CONTINUOUSLY
                );

                bgmClip.start();
            }

        } catch (
            Exception ex
        ) {

            ex.printStackTrace();
        }
    }

    private void stopDVDMode() {

        dvdMode = false;

        if (
            bgmClip != null
        ) {

            if (
                bgmClip.isRunning()
            ) {

                bgmClip.stop();
            }

            bgmClip.close();
            bgmClip = null;
        }
    }

    /*
     * STOP EVERYTHING NOW
     *
     * This permanently freezes the window until the
     * application is restarted.
     */
    private void stopEverythingNow() {

        everythingStopped = true;

        dvdMode = false;

        if (
            mainLoopTimer != null
        ) {

            mainLoopTimer.stop();
        }

        if (
            bgmClip != null
        ) {

            if (
                bgmClip.isRunning()
            ) {

                bgmClip.stop();
            }

            bgmClip.close();
            bgmClip = null;
        }

        isSoundPlaying = false;

        currentX = getX();
        currentY = getY();

        targetX = currentX;
        targetY = currentY;

        dragStart = null;
    }

    private void applyStyles() {

        windowContentPanel.setBackground(
            bgColor
        );

        titleLabel.setForeground(
            titleColor
        );

        descLabel.setForeground(
            bodyColor
        );

        okButton.setForeground(
            titleColor
        );

        titleLabel.setFont(
            new Font(
                fontFam,
                Font.BOLD,
                baseFontSize + 4
            )
        );

        descLabel.setFont(
            new Font(
                fontFam,
                Font.PLAIN,
                baseFontSize + 1
            )
        );

        okButton.setFont(
            new Font(
                fontFam,
                Font.BOLD,
                baseFontSize
            )
        );
    }

    private void openTextSettings() {

        String newTitle =
            JOptionPane.showInputDialog(
                this,
                "Title Text:",
                titleLabel.getText()
            );

        if (
            newTitle != null
        ) {

            titleLabel.setText(
                newTitle
            );
        }

        String newDesc =
            JOptionPane.showInputDialog(
                this,
                "Body Text:",
                descLabel
                    .getText()
                    .replace(
                        "<html>",
                        ""
                    )
                    .replace(
                        "</html>",
                        ""
                    )
            );

        if (
            newDesc != null
        ) {

            descLabel.setText(
                "<html>" +
                newDesc.replaceAll(
                    "\n",
                    "<br>"
                ) +
                "</html>"
            );
        }
    }

    private void openButtonSettings() {

        String btnText =
            JOptionPane.showInputDialog(
                this,
                "Button Text:",
                okButton.getText()
            );

        if (
            btnText != null
        ) {

            okButton.setText(
                btnText
            );
        }

        int enabledChoice =
            JOptionPane.showConfirmDialog(
                this,
                "Enable button?",
                "Button Config",
                JOptionPane.YES_NO_OPTION
            );

        okButton.setEnabled(
            enabledChoice ==
            JOptionPane.YES_OPTION
        );
    }

    private void openFontSettings() {

        String newFam =
            JOptionPane.showInputDialog(
                this,
                "Font Family Name:",
                fontFam
            );

        if (
            newFam != null &&
            !newFam.trim().isEmpty()
        ) {

            fontFam =
                newFam.trim();
        }

        String sizeStr =
            JOptionPane.showInputDialog(
                this,
                "Base Font Size:",
                String.valueOf(
                    baseFontSize
                )
            );

        try {

            if (
                sizeStr != null
            ) {

                baseFontSize =
                    Integer.parseInt(
                        sizeStr.trim()
                    );
            }

        } catch (
            Exception ignored
        ) {}

        applyStyles();
    }

    private void openColorSettings() {

        Color newColor =
            JColorChooser.showDialog(
                this,
                "Background Tint",
                bgColor
            );

        if (
            newColor != null
        ) {

            backgroundImage = null;

            bgColor =
                newColor;

            applyStyles();

            windowContentPanel.repaint();
        }
    }

    private void openTextColorPicker() {

        Color newColor =
            JColorChooser.showDialog(
                this,
                "Text Color",
                titleColor
            );

        if (
            newColor != null
        ) {

            titleColor =
                newColor;

            bodyColor =
                newColor;

            applyStyles();

            windowContentPanel.repaint();
        }
    }

    private void openBackgroundImagePicker() {

        JFileChooser fileChooser =
            new JFileChooser();

        if (
            fileChooser.showOpenDialog(this) ==
            JFileChooser.APPROVE_OPTION
        ) {

            try {

                Image img =
                    ImageIO.read(
                        fileChooser
                            .getSelectedFile()
                    );

                if (
                    img != null
                ) {

                    backgroundImage =
                        img;

                    windowContentPanel.repaint();
                }

            } catch (
                Exception ignored
            ) {}
        }
    }

    private void openImagePicker() {

        JFileChooser fileChooser =
            new JFileChooser();

        if (
            fileChooser.showOpenDialog(this) ==
            JFileChooser.APPROVE_OPTION
        ) {

            try {

                Image rawImg =
                    ImageIO.read(
                        fileChooser
                            .getSelectedFile()
                    );

                if (
                    rawImg != null
                ) {

                    iconLabel.setIcon(
                        new ImageIcon(
                            rawImg.getScaledInstance(
                                80,
                                80,
                                Image.SCALE_SMOOTH
                            )
                        )
                    );
                }

            } catch (
                Exception ignored
            ) {}
        }
    }

    private void openAudioPicker() {

        JFileChooser fileChooser =
            new JFileChooser();

        if (
            fileChooser.showOpenDialog(this) ==
            JFileChooser.APPROVE_OPTION
        ) {

            currentAudioPath =
                fileChooser
                    .getSelectedFile()
                    .getAbsolutePath();
        }
    }

    private void playSound(
        String path
    ) {

        if (
            everythingStopped ||
            isSoundPlaying ||
            path.isEmpty()
        ) {

            return;
        }

        new Thread(
            () -> {

                try {

                    File soundFile =
                        new File(
                            path
                        );

                    if (
                        !soundFile.exists() ||
                        everythingStopped
                    ) {

                        return;
                    }

                    isSoundPlaying =
                        true;

                    AudioInputStream audioStream =
                        AudioSystem.getAudioInputStream(
                            soundFile
                        );

                    Clip clip =
                        AudioSystem.getClip();

                    clip.open(
                        audioStream
                    );

                    if (
                        everythingStopped
                    ) {

                        clip.close();

                        isSoundPlaying =
                            false;

                        return;
                    }

                    clip.start();

                    clip.addLineListener(
                        event -> {

                            if (
                                event.getType() ==
                                LineEvent.Type.STOP
                            ) {

                                clip.close();

                                isSoundPlaying =
                                    false;
                            }
                        }
                    );

                } catch (
                    Exception ex
                ) {

                    isSoundPlaying =
                        false;
                }

            }
        ).start();
    }

    private void evaluateFleeing() {

        if (
            everythingStopped
        ) {

            return;
        }

        if (
            !runawayEnabled
        ) {

            return;
        }

        if (
            dvdMode
        ) {

            return;
        }

        if (
            sidebarOpen
        ) {

            return;
        }

        PointerInfo pointerInfo =
            MouseInfo.getPointerInfo();

        if (
            pointerInfo == null
        ) {

            return;
        }

        Point mouseAbsolute =
            pointerInfo.getLocation();

        Dimension screenSize =
            Toolkit
                .getDefaultToolkit()
                .getScreenSize();

        int windowCenterX =
            currentX +
            (getWidth() / 2);

        int windowCenterY =
            currentY +
            (getHeight() / 2);

        double distance =
            Math.hypot(
                mouseAbsolute.x -
                    windowCenterX,
                mouseAbsolute.y -
                    windowCenterY
            );

        if (
            distance < safeDistance
        ) {

            int moveX =
                (
                    mouseAbsolute.x <
                    windowCenterX
                )
                    ? 260
                    : -260;

            int moveY =
                (
                    mouseAbsolute.y <
                    windowCenterY
                )
                    ? 260
                    : -260;

            int nextTargetX =
                currentX +
                moveX;

            int nextTargetY =
                currentY +
                moveY;

            if (
                nextTargetX < 0 ||
                nextTargetX + getWidth() >
                    screenSize.width ||
                nextTargetY < 25 ||
                nextTargetY + getHeight() >
                    screenSize.height
            ) {

                targetX =
                    (
                        screenSize.width /
                        2
                    ) -
                    (
                        getWidth() /
                        2
                    ) +
                    random.nextInt(
                        200
                    ) -
                    100;

                targetY =
                    (
                        screenSize.height /
                        2
                    ) -
                    (
                        getHeight() /
                        2
                    ) +
                    random.nextInt(
                        200
                    ) -
                    100;

            } else {

                targetX =
                    nextTargetX;

                targetY =
                    nextTargetY;
            }

            playSound(
                currentAudioPath
            );
        }
    }

    public static void main(
        String[] args
    ) {

        SwingUtilities.invokeLater(
            () ->
                new HarmonysWindowmaker()
        );
    }
}