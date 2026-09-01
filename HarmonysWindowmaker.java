import javax.swing.*;
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
    
    private JLabel iconLabel, titleLabel, descLabel;
    private JButton okButton;
    private boolean sidebarOpen = false;
    private boolean buttonVisible = true;

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
        layeredPane.setPreferredSize(new Dimension(windowWidth, windowHeight));

        windowContentPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        windowContentPanel.setBounds(15, 0, windowWidth - 15, windowHeight);
        windowContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 10, 0, 10);

        iconLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.0;
        windowContentPanel.add(iconLabel, gbc);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        titleLabel = new JLabel("DEVICE_ERROR");
        descLabel = new JLabel("<html>CHOOSE YOUR TEXT WITH THE SIDEBAR</html>");
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 1.0;
        windowContentPanel.add(textPanel, gbc);

        okButton = new JButton("PROCEED");
        okButton.setFocusable(false);
        okButton.setEnabled(false); 
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 0.0;
        windowContentPanel.add(buttonPanel, gbc);

        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(new Color(20, 20, 20)); 
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Color.DARK_GRAY));
        sidebarPanel.setBounds(0, 0, 15, windowHeight); 

        setupSidebarButtons();

        MouseAdapter sidebarHoverEngine = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!sidebarOpen) {
                    sidebarOpen = true;
                    sidebarPanel.setBounds(0, 0, sidebarExpandedWidth, windowHeight);
                    windowContentPanel.setBounds(sidebarExpandedWidth, 0, windowWidth - sidebarExpandedWidth, windowHeight);
                    toggleSidebarComponentsVisibility(true);
                    refreshPanels();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (sidebarOpen && !sidebarPanel.getBounds().contains(e.getPoint())) {
                    sidebarOpen = false;
                    sidebarPanel.setBounds(0, 0, 15, windowHeight);
                    windowContentPanel.setBounds(15, 0, windowWidth - 15, windowHeight);
                    toggleSidebarComponentsVisibility(false);
                    refreshPanels();
                }
            }
        };
        sidebarPanel.addMouseListener(sidebarHoverEngine);

        layeredPane.add(windowContentPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(sidebarPanel, JLayeredPane.PALETTE_LAYER);
        setContentPane(layeredPane);

        applyStyles();
        setVisible(true);

        Timer glideTimer = new Timer(16, e -> {
            int dx = targetX - currentX;
            int dy = targetY - currentY;

            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                currentX += dx / GLIDE_SPEED;
                currentY += dy / GLIDE_SPEED;
                setLocation(currentX, currentY);
            }
        });
        glideTimer.start();

        Thread globalMouseThread = new Thread(() -> {
            while (true) {
                try {
                    evaluateFleeing();
                    Thread.sleep(30); 
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        globalMouseThread.setDaemon(true);
        globalMouseThread.start();
    }

    private void refreshPanels() {
        sidebarPanel.revalidate();
        sidebarPanel.repaint();
        windowContentPanel.revalidate();
        windowContentPanel.repaint();
    }

    private void setupSidebarButtons() {
        Dimension btnSize = new Dimension(160, 20);
        
        JButton btnText = createSidebarBtn("(Window Text)", btnSize);
        btnText.addActionListener(e -> openTextSettings());
        
        JButton btnBtn = createSidebarBtn("(Button Text)", btnSize);
        btnBtn.addActionListener(e -> openButtonSettings());

        JButton toggleBtnVisibility = createSidebarBtn("(Toggle Button)", btnSize);
        toggleBtnVisibility.addActionListener(e -> toggleMainButtonVisibility());

        JButton btnFont = createSidebarBtn("(Font)", btnSize);
        btnFont.addActionListener(e -> openFontSettings());

        JButton btnColor = createSidebarBtn("(Color)", btnSize);
        btnColor.addActionListener(e -> openColorSettings());

        JButton btnBg = createSidebarBtn("(BG)", btnSize);
        btnBg.addActionListener(e -> openBackgroundImagePicker());

        JButton btnSprite = createSidebarBtn("(Icon)", btnSize);
        btnSprite.addActionListener(e -> openImagePicker());

        JButton btnSound = createSidebarBtn("(Audio)", btnSize);
        btnSound.addActionListener(e -> openAudioPicker());

        sidebarPanel.add(Box.createVerticalStrut(5));
        sidebarPanel.add(btnText); sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnBtn);  sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(toggleBtnVisibility); sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnFont); sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnColor);sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnBg);   sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnSprite);sidebarPanel.add(Box.createVerticalStrut(2));
        sidebarPanel.add(btnSound);

        toggleSidebarComponentsVisibility(false);
    }

    private JButton createSidebarBtn(String label, Dimension size) {
        JButton btn = new JButton(label);
        btn.setMaximumSize(size);
        btn.setPreferredSize(size);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Courier New", Font.BOLD, 11)); 
        btn.setFocusable(false);
        
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        return btn;
    }

    private void toggleSidebarComponentsVisibility(boolean visible) {
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton) {
                c.setVisible(visible);
            }
        }
    }

    private void toggleMainButtonVisibility() {
        buttonVisible = !buttonVisible;
        okButton.setVisible(buttonVisible);
        windowContentPanel.revalidate();
        windowContentPanel.repaint();
    }

    private void applyStyles() {
        windowContentPanel.setBackground(bgColor);
        titleLabel.setForeground(titleColor);
        descLabel.setForeground(bodyColor);

        titleLabel.setFont(new Font(fontFam, Font.BOLD, baseFontSize + 4));
        descLabel.setFont(new Font(fontFam, Font.PLAIN, baseFontSize + 1));
        okButton.setFont(new Font(fontFam, Font.BOLD, baseFontSize));
    }

    private void openTextSettings() {
        String newTitle = JOptionPane.showInputDialog(this, "Title Text:", titleLabel.getText());
        if (newTitle != null) titleLabel.setText(newTitle);

        String newDesc = JOptionPane.showInputDialog(this, "Body Text:", descLabel.getText().replace("<html>", "").replace("</html>", ""));
        if (newDesc != null) descLabel.setText("<html>" + newDesc.replaceAll("\n", "<br>") + "</html>");
    }

    private void openButtonSettings() {
        String btnText = JOptionPane.showInputDialog(this, "Button Text:", okButton.getText());
        if (btnText != null) okButton.setText(btnText);
        int enabledChoice = JOptionPane.showConfirmDialog(this, "Enable button?", "Button Config", JOptionPane.YES_NO_OPTION);
        okButton.setEnabled(enabledChoice == JOptionPane.YES_OPTION);
    }

    private void openFontSettings() {
        String newFam = JOptionPane.showInputDialog(this, "Font Family Name:", fontFam);
        if (newFam != null && !newFam.trim().isEmpty()) fontFam = newFam.trim();
        String sizeStr = JOptionPane.showInputDialog(this, "Base Font Size:", String.valueOf(baseFontSize));
        try { if (sizeStr != null) baseFontSize = Integer.parseInt(sizeStr.trim()); } catch (Exception ignored) {}
        applyStyles();
    }

    private void openColorSettings() {
        Color newColor = JColorChooser.showDialog(this, "Background Tint", bgColor);
        if (newColor != null) {
            backgroundImage = null; 
            bgColor = newColor;
            applyStyles();
            windowContentPanel.repaint();
        }
    }

    private void openBackgroundImagePicker() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try { Image img = ImageIO.read(fileChooser.getSelectedFile()); if (img != null) { backgroundImage = img; windowContentPanel.repaint(); } } catch (Exception ignored) {}
        }
    }

    private void openImagePicker() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try { Image rawImg = ImageIO.read(fileChooser.getSelectedFile()); if (rawImg != null) iconLabel.setIcon(new ImageIcon(rawImg.getScaledInstance(60, 60, Image.SCALE_SMOOTH))); } catch (Exception ignored) {}
        }
    }

    private void openAudioPicker() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentAudioPath = fileChooser.getSelectedFile().getAbsolutePath();
        }
    }

    private void playSound(String path) {
        if (isSoundPlaying || path.isEmpty()) return;
        new Thread(() -> {
            try {
                File soundFile = new File(path);
                if (!soundFile.exists()) return;
                isSoundPlaying = true; 
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                clip.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP) { clip.close(); isSoundPlaying = false; } });
            } catch (Exception ex) { isSoundPlaying = false; }
        }).start();
    }

    private void evaluateFleeing() {
        if (sidebarOpen) return;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;
        Point mouseAbsolute = pointerInfo.getLocation();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int windowCenterX = currentX + (getWidth() / 2);
        int windowCenterY = currentY + (getHeight() / 2);

        double distance = Math.hypot(mouseAbsolute.x - windowCenterX, mouseAbsolute.y - windowCenterY);

        if (distance < safeDistance) {
            int moveX = (mouseAbsolute.x < windowCenterX) ? 260 : -260;
            int moveY = (mouseAbsolute.y < windowCenterY) ? 260 : -260;

            int nextTargetX = currentX + moveX;
            int nextTargetY = currentY + moveY;

            if (nextTargetX < 0 || nextTargetX + getWidth() > screenSize.width ||
                nextTargetY < 25 || nextTargetY + getHeight() > screenSize.height) {
                targetX = (screenSize.width / 2) - (getWidth() / 2) + random.nextInt(200) - 100;
                targetY = (screenSize.height / 2) - (getHeight() / 2) + random.nextInt(200) - 100;
            } else {
                targetX = nextTargetX;
                targetY = nextTargetY;
            }
            playSound(currentAudioPath);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HarmonysWindowmaker());
    }
}