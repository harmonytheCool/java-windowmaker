import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.Random;

public class HarmonysWindowmaker extends JFrame {

    private final Random rand = new Random();
    private final int safeDist = 150;

    private int curX, curY, targetX, targetY;
    private static final int SPEED = 6;

    private String audioPath = "";
    private String fontFam = "MS Sans Serif";
    private int fontSize = 11;

    private Color bgColor = new Color(192, 192, 192);
    private Color titleColor = Color.WHITE;
    private Color bodyColor = Color.BLACK;

    private Image bgImg = null;

    private final int winW = 500, winH = 190;
    private final int expW = 1000, expH = 630;

    private JLayeredPane layeredPane;
    private JPanel mainPane, boardPane, contentPaneContainer;

    private JLabel iconLbl, titleLbl, descLbl;
    private JButton okBtn;

    private boolean btnVisible = true;
    private boolean bitMode = false;
    private boolean dvdMode = false;
    private boolean runaway = true;
    private boolean stopped = false;
    private volatile boolean dragging = false;

    private int vx = 5, vy = 5;

    private Clip bgmClip;
    private Timer timer;

    private JButton[] btns = new JButton[12];
    private String[] sounds = new String[12];

    private toolbarcoolbarWindow toolbarcoolbarWindow;
    private CustomTitleBar titleBar;
    private ResizeGrip mainResizeGrip;

    public HarmonysWindowmaker() {
        setUndecorated(true);
        setSize(winW, winH);
        setLocationRelativeTo(null);
        setAlwaysOnTop(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Point p = getLocation();
        curX = targetX = p.x;
        curY = targetY = p.y;

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        contentPaneContainer = new JPanel(new BorderLayout());
        contentPaneContainer.setBounds(0, 0, winW, winH);
        contentPaneContainer.setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128), 1));

        titleBar = new CustomTitleBar("DEVICE_ERROR.EXE", this, true);
        contentPaneContainer.add(titleBar, BorderLayout.NORTH);

        JPanel innerContainer = new JPanel(null);
        innerContainer.setBackground(bgColor);

        mainPane = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImg != null) {
                    g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        mainPane.setBounds(0, 0, winW, winH - 26);
        mainPane.setBackground(bgColor);
        mainPane.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, Color.WHITE, new Color(128, 128, 128)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 0, 5);

        iconLbl = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        mainPane.add(iconLbl, gbc);

        JPanel txtPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        txtPanel.setOpaque(false);

        titleLbl = new JLabel("DEVICE_ERROR") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(Color.BLACK);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), 0, fm.getAscent());
                g2.dispose();
            }
        };

        descLbl = new JLabel("<html>CHOOSE YOUR TEXT WITH THE COOLBAR</html>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(Color.BLACK);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText().replaceAll("<[^>]*>", ""), 0, fm.getAscent());
                g2.dispose();
            }
        };

        txtPanel.add(titleLbl);
        txtPanel.add(descLbl);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        mainPane.add(txtPanel, gbc);

        okBtn = new JButton("OK") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(192, 192, 192));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(Color.WHITE);
                g2.drawLine(0, 0, getWidth() - 1, 0);
                g2.drawLine(0, 0, 0, getHeight() - 1);
                
                g2.setColor(new Color(128, 128, 128));
                g2.drawLine(1, getHeight() - 1, getWidth() - 1, getHeight() - 1);
                g2.drawLine(getWidth() - 1, 1, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(Color.BLACK);
                g2.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        okBtn.setFocusable(false);
        okBtn.setEnabled(false);
        okBtn.setForeground(Color.BLACK);
        okBtn.setContentAreaFilled(false);
        okBtn.setBorderPainted(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridheight = 1;
        gbc.weighty = 0.0;
        mainPane.add(btnPanel, gbc);

        boardPane = new JPanel(new GridLayout(3, 4, 5, 5));
        boardPane.setOpaque(true);
        boardPane.setBackground(new Color(192, 192, 192));
        boardPane.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, Color.WHITE, new Color(128, 128, 128)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
        );
        boardPane.setVisible(false);

        for (int i = 0; i < 12; i++) {
            final int idx = i;
            JButton b = new JButton("Slot " + (i + 1)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(Color.WHITE);
                    g2.drawLine(0, 0, getWidth() - 1, 0);
                    g2.drawLine(0, 0, 0, getHeight() - 1);

                    g2.setColor(new Color(128, 128, 128));
                    g2.drawLine(1, getHeight() - 2, getWidth() - 2, getHeight() - 2);
                    g2.drawLine(getWidth() - 2, 1, getWidth() - 2, getHeight() - 2);

                    g2.setColor(Color.BLACK);
                    g2.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
                    g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);

                    super.paintComponent(g2);
                    g2.dispose();
                }
            };

            b.setFocusable(false);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setBackground(new Color(192, 192, 192));
            b.setForeground(Color.BLACK);
            b.setFont(new Font("MS Sans Serif", Font.PLAIN, 11));

            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showMenu(e, idx);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        play(sounds[idx]);
                    }
                }
            });

            btns[i] = b;
            boardPane.add(b);
        }

        innerContainer.add(mainPane);
        innerContainer.add(boardPane);
        contentPaneContainer.add(innerContainer, BorderLayout.CENTER);

        mainResizeGrip = new ResizeGrip(this, 200, 100);
        layeredPane.add(mainResizeGrip, JLayeredPane.PALETTE_LAYER);

        layeredPane.add(contentPaneContainer);
        contentPaneContainer.setBounds(0, 0, winW, winH);
        mainResizeGrip.setBounds(winW - 16, winH - 16, 16, 16);

        setContentPane(layeredPane);

        applyTheme();
        setVisible(true);

        toolbarcoolbarWindow = new toolbarcoolbarWindow();
        toolbarcoolbarWindow.setVisible(true);
        toolbarcoolbarWindow.toFront();

        timer = new Timer(16, e -> {
            if (stopped || dragging) return;

            if (dvdMode) {
                Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
                int actualW = getWidth();
                int actualH = getHeight();

                curX += vx;
                curY += vy;

                if (curX <= 0 || curX + actualW >= scr.width) {
                    vx = -vx;
                    curX = Math.max(0, Math.min(curX, scr.width - actualW));
                }

                if (curY <= 0 || curY + actualH >= scr.height) {
                    vy = -vy;
                    curY = Math.max(0, Math.min(curY, scr.height - actualH));
                }

                setLocation(curX, curY);
            } else {
                int dx = targetX - curX;
                int dy = targetY - curY;

                if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                    curX += dx / SPEED;
                    curY += dy / SPEED;
                    setLocation(curX, curY);
                }
            }
        });

        timer.start();

        Thread fleeThread = new Thread(() -> {
            while (true) {
                try {
                    checkFlee();
                    Thread.sleep(30);
                } catch (Exception ignored) {
                    break;
                }
            }
        });

        fleeThread.setDaemon(true);
        fleeThread.start();
    }

    public void updateLayoutBounds(int w, int h) {
        contentPaneContainer.setBounds(0, 0, w, h);
        int contentH = h - 26;
        mainPane.setBounds(0, 0, w, contentH);
        boardPane.setBounds(0, 0, w, contentH);
        if (mainResizeGrip != null) {
            mainResizeGrip.setBounds(w - 16, h - 16, 16, 16);
        }
        mainPane.revalidate();
        boardPane.revalidate();
    }

    private void toggleBit() {
        bitMode = !bitMode;
        int w = bitMode ? expW : winW;
        int h = bitMode ? expH : winH;

        setSize(w, h);
        updateLayoutBounds(w, h);

        mainPane.setVisible(!bitMode);
        boardPane.setVisible(bitMode);
        redraw();
    }

    private void showMenu(MouseEvent e, int idx) {
        JPopupMenu m = new JPopupMenu();

        JMenuItem audio = new JMenuItem("Assign WAV Sound");
        audio.addActionListener(ev -> setSlotAudio(idx));

        JMenuItem lbl = new JMenuItem("Set Custom Label");
        lbl.addActionListener(ev -> setSlotLabel(idx));

        m.add(audio);
        m.add(lbl);
        m.show(e.getComponent(), e.getX(), e.getY());
    }

    private void setSlotAudio(int i) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            sounds[i] = f.getAbsolutePath();
            btns[i].setText(f.getName());
        }
    }

    private void setSlotLabel(int i) {
        String val = JOptionPane.showInputDialog(this, "Enter Button Label:", btns[i].getText());
        if (val != null && !val.trim().isEmpty()) {
            btns[i].setText(val);
        }
    }

    private void checkFlee() {
        if (stopped || !runaway || dvdMode || dragging || bitMode) return;

        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi == null) return;

        Point m = pi.getLocation();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        Point loc = getLocation();

        int cx = loc.x + (getWidth() / 2);
        int cy = loc.y + (getHeight() / 2);

        if (Math.hypot(m.x - cx, m.y - cy) < safeDist && !dragging) {
            int mx = (m.x < cx) ? 260 : -260;
            int my = (m.y < cy) ? 260 : -260;
            int nx = loc.x + mx;
            int ny = loc.y + my;

            if (nx < 0 || nx + getWidth() > scr.width || ny < 25 || ny + getHeight() > scr.height) {
                targetX = (scr.width / 2) - (getHeight() / 2) + rand.nextInt(200) - 100;
                targetY = (scr.height / 2) - (getHeight() / 2) + rand.nextInt(200) - 100;
            } else {
                targetX = nx;
                targetY = ny;
            }

            play(audioPath);
        }
    }

    private void play(String path) {
        if (stopped || path == null || path.isEmpty()) return;

        new Thread(() -> {
            try {
                File f = new File(path);
                if (!f.exists() || stopped) return;

                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            } catch (Exception ignored) {}
        }).start();
    }

    private void applyTheme() {
        Font f = new Font(fontFam, Font.PLAIN, fontSize);
        Font tf = new Font(fontFam, Font.BOLD, fontSize + 2);

        if (titleLbl != null) {
            titleLbl.setFont(tf);
            titleLbl.repaint();
        }

        if (descLbl != null) {
            descLbl.setFont(f);
            descLbl.repaint();
        }

        if (mainPane != null) {
            mainPane.setBackground(bgColor);
        }

        if (toolbarcoolbarWindow != null) {
            toolbarcoolbarWindow.applytoolbarcoolbarTheme();
        }
    }

    private void redraw() {
        mainPane.revalidate();
        mainPane.repaint();
        boardPane.revalidate();
        boardPane.repaint();
    }

    private void stopDVD() {
        dvdMode = false;
        if (bgmClip != null) {
            if (bgmClip.isRunning()) bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HarmonysWindowmaker().setVisible(true));
    }

    static class ResizeGrip extends JComponent {
        private final JFrame targetFrame;
        private Point pressPt;
        private final int minW;
        private final int minH;

        public ResizeGrip(JFrame target, int minW, int minH) {
            this.targetFrame = target;
            this.minW = minW;
            this.minH = minH;
            setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (targetFrame instanceof HarmonysWindowmaker && ((HarmonysWindowmaker) targetFrame).stopped) return;
                    pressPt = e.getLocationOnScreen();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressPt = null;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (pressPt == null) return;
                    if (targetFrame instanceof HarmonysWindowmaker && ((HarmonysWindowmaker) targetFrame).stopped) return;
                    
                    Point cur = e.getLocationOnScreen();
                    int dx = cur.x - pressPt.x;
                    int dy = cur.y - pressPt.y;

                    Rectangle bounds = targetFrame.getBounds();
                    bounds.width = Math.max(minW, bounds.width + dx);
                    bounds.height = Math.max(minH, bounds.height + dy);
                    targetFrame.setBounds(bounds);

                    pressPt = cur;

                    if (targetFrame instanceof HarmonysWindowmaker) {
                        ((HarmonysWindowmaker) targetFrame).updateLayoutBounds(bounds.width, bounds.height);
                    } else if (targetFrame instanceof toolbarcoolbarWindow) {
                        ((toolbarcoolbarWindow) targetFrame).updatetoolbarcoolbarLayout(bounds.width, bounds.height);
                    }
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(128, 128, 128));
            for (int i = 2; i < getWidth(); i += 4) {
                for (int j = 2; j < getHeight(); j += 4) {
                    if (i + j >= getWidth() - 2) {
                        g2.fillRect(i, j, 1, 1);
                    }
                }
            }
            g2.setColor(Color.WHITE);
            for (int i = 3; i < getWidth(); i += 4) {
                for (int j = 3; j < getHeight(); j += 4) {
                    if (i + j >= getWidth() - 1) {
                        g2.fillRect(i, j, 1, 1);
                    }
                }
            }
            g2.dispose();
        }
    }

    static class CustomTitleBar extends JPanel {
        private String title;
        private final JFrame parentFrame;
        private Point pressPt;
        private JLabel titleLabel;

        private Color gradStartColor = Color.BLACK;
        private Color gradEndColor = new Color(150, 0, 75);

        public CustomTitleBar(String title, JFrame parent, boolean includeControls) {
            this.title = title;
            this.parentFrame = parent;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(0, 26));

            titleLabel = new JLabel("  " + title);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(new Font("MS Sans Serif", Font.BOLD, 11));
            add(titleLabel, BorderLayout.CENTER);

            if (includeControls) {
                JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
                controlsPanel.setOpaque(false);

                JButton minBtn = createWin98TitleButton("_");
                JButton maxBtn = createWin98TitleButton("□");
                JButton closeBtn = createWin98TitleButton("X");

                minBtn.addActionListener(e -> parent.setState(JFrame.ICONIFIED));
                maxBtn.addActionListener(e -> {
                    if (parent instanceof HarmonysWindowmaker) {
                        ((HarmonysWindowmaker) parent).toggleBit();
                    }
                });
                closeBtn.addActionListener(e -> System.exit(0));

                controlsPanel.add(minBtn);
                controlsPanel.add(maxBtn);
                controlsPanel.add(closeBtn);

                add(controlsPanel, BorderLayout.EAST);
            }

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    pressPt = e.getLocationOnScreen();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (pressPt == null) return;
                    Point cur = e.getLocationOnScreen();
                    int nx = parent.getX() + (cur.x - pressPt.x);
                    int ny = parent.getY() + (cur.y - pressPt.y);
                    parent.setLocation(nx, ny);
                    pressPt = cur;
                    
                    if (parent instanceof HarmonysWindowmaker) {
                        ((HarmonysWindowmaker) parent).curX = ((HarmonysWindowmaker) parent).targetX = nx;
                        ((HarmonysWindowmaker) parent).curY = ((HarmonysWindowmaker) parent).targetY = ny;
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, gradStartColor, getWidth(), 0, gradEndColor);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        public void setGradientColors(Color start, Color end) {
            this.gradStartColor = start;
            this.gradEndColor = end;
            repaint();
        }

        private JButton createWin98TitleButton(String text) {
            JButton b = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    
                    g2.setColor(new Color(192, 192, 192));
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(Color.WHITE);
                    g2.drawLine(0, 0, getWidth() - 1, 0);
                    g2.drawLine(0, 0, 0, getHeight() - 1);

                    g2.setColor(new Color(128, 128, 128));
                    g2.drawLine(1, getHeight() - 2, getWidth() - 2, getHeight() - 2);
                    g2.drawLine(getWidth() - 2, 1, getWidth() - 2, getHeight() - 2);

                    g2.setColor(Color.BLACK);
                    g2.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
                    g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);

                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent() - 1;
                    
                    g2.drawString(text, x, y);
                    g2.dispose();
                }
            };
            b.setPreferredSize(new Dimension(16, 14));
            b.setFont(new Font("Monospaced", Font.BOLD, 10));
            b.setFocusable(false);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setForeground(Color.BLACK);
            return b;
        }

        public void setTitleText(String t) {
            this.title = t;
            if (titleLabel != null) {
                titleLabel.setText("  " + t);
            }
            repaint();
        }
    }

    class toolbarcoolbarWindow extends JFrame {

        private final JLayeredPane toolbarcoolbarLayeredPane;
        private final JPanel toolbarcoolbarContent;
        private final JScrollPane toolbarcoolbarScroll;
        private final JPanel toolbarcoolbarContainer;
        private final ResizeGrip toolbarcoolbarResizeGrip;
        private final CustomTitleBar toolbarcoolbarTitle;
        
        private final Color toolbarcoolbarBgColor = Color.BLACK;
        private final Color toolbarcoolbarFgColor = Color.WHITE;
        private final Font toolbarcoolbarFont = new Font("MS Sans Serif", Font.PLAIN, 11);

        toolbarcoolbarWindow() {
            setUndecorated(true);
            setSize(620, 280);
            setLocationRelativeTo(HarmonysWindowmaker.this);
            setAlwaysOnTop(false);
            setResizable(false);

            toolbarcoolbarLayeredPane = new JLayeredPane();
            toolbarcoolbarLayeredPane.setLayout(null);

            toolbarcoolbarContainer = new JPanel(new BorderLayout());
            toolbarcoolbarContainer.setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128), 1));
            toolbarcoolbarContainer.setBounds(0, 0, 620, 280);
            
            toolbarcoolbarTitle = new CustomTitleBar("Toolbar Java Test Function / COOLBAR", this, true);
            toolbarcoolbarContainer.add(toolbarcoolbarTitle, BorderLayout.NORTH);

            toolbarcoolbarContent = new JPanel(new GridLayout(4, 4, 4, 4));
            toolbarcoolbarContent.setBackground(toolbarcoolbarBgColor);

            toolbarcoolbarScroll = new JScrollPane(toolbarcoolbarContent);
            toolbarcoolbarScroll.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
            toolbarcoolbarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            toolbarcoolbarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

            toolbarcoolbarContainer.add(toolbarcoolbarScroll, BorderLayout.CENTER);

            toolbarcoolbarResizeGrip = new ResizeGrip(this, 200, 100);
            
            toolbarcoolbarLayeredPane.add(toolbarcoolbarContainer, JLayeredPane.DEFAULT_LAYER);
            toolbarcoolbarLayeredPane.add(toolbarcoolbarResizeGrip, JLayeredPane.PALETTE_LAYER);
            toolbarcoolbarResizeGrip.setBounds(620 - 16, 280 - 16, 16, 16);

            setContentPane(toolbarcoolbarLayeredPane);

            buildtoolbarcoolbar();
            applytoolbarcoolbarTheme();
        }

        public void updatetoolbarcoolbarLayout(int w, int h) {
            toolbarcoolbarContainer.setBounds(0, 0, w, h);
            toolbarcoolbarResizeGrip.setBounds(w - 16, h - 16, 16, 16);
            toolbarcoolbarContainer.revalidate();
        }

        private void buildtoolbarcoolbar() {
            addtoolbarcoolbarButton("Bit Mode", e -> toggleBit());

            addtoolbarcoolbarButton("Window Text", e -> {
                String t = JOptionPane.showInputDialog(HarmonysWindowmaker.this, "Enter Title:", titleLbl.getText());
                if (t != null && !t.trim().isEmpty()) {
                    titleLbl.setText(t);
                    if (titleBar != null) titleBar.setTitleText(t);
                }
                String d = JOptionPane.showInputDialog(HarmonysWindowmaker.this, "Enter Body:", descLbl.getText());
                if (d != null && !d.trim().isEmpty()) {
                    descLbl.setText("<html>" + d + "</html>");
                }
            });

            addtoolbarcoolbarButton("Button Text", e -> {
                String txt = JOptionPane.showInputDialog(HarmonysWindowmaker.this, "Enter Button Text:", okBtn.getText());
                if (txt != null && !txt.trim().isEmpty()) {
                    okBtn.setText(txt);
                }
            });

            addtoolbarcoolbarButton("Toggle Btn", e -> {
                btnVisible = !btnVisible;
                okBtn.setVisible(btnVisible);
                mainPane.revalidate();
                mainPane.repaint();
            });

            addtoolbarcoolbarButton("Font", e -> {
                String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                String sel = (String) JOptionPane.showInputDialog(HarmonysWindowmaker.this, "Select Font:", "Font", JOptionPane.QUESTION_MESSAGE, null, fonts, fontFam);
                if (sel != null) {
                    fontFam = sel;
                    applyTheme();
                }
            });

            addtoolbarcoolbarButton("Color", e -> {
                Color c = JColorChooser.showDialog(HarmonysWindowmaker.this, "Choose Background", bgColor);
                if (c != null) {
                    bgColor = c;
                    applyTheme();
                }
            });

            addtoolbarcoolbarButton("Titlebar Gradient", e -> {
                Color start = JColorChooser.showDialog(HarmonysWindowmaker.this, "Choose Gradient Start Color (Left)", Color.BLACK);
                if (start != null) {
                    Color end = JColorChooser.showDialog(HarmonysWindowmaker.this, "Choose Gradient End Color (Right)", new Color(150, 0, 75));
                    if (end != null) {
                        if (titleBar != null) titleBar.setGradientColors(start, end);
                        if (toolbarcoolbarTitle != null) toolbarcoolbarTitle.setGradientColors(start, end);
                    }
                }
            });

            addtoolbarcoolbarButton("Audio File", e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav"));

                if (fc.showOpenDialog(HarmonysWindowmaker.this) == JFileChooser.APPROVE_OPTION) {
                    audioPath = fc.getSelectedFile().getAbsolutePath();
                }
            });

            addtoolbarcoolbarButton("DVD Mode", e -> {
                JButton source = (JButton) e.getSource();

                if (!dvdMode) {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav"));

                    if (fc.showOpenDialog(HarmonysWindowmaker.this) == JFileChooser.APPROVE_OPTION) {
                        audioPath = fc.getSelectedFile().getAbsolutePath();
                        dvdMode = true;
                        source.setText("DVD: ON");

                        try {
                            File f = new File(audioPath);
                            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                            bgmClip = AudioSystem.getClip();
                            bgmClip.open(ais);
                            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                            bgmClip.start();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    dvdMode = false;
                    stopDVD();
                    source.setText("DVD Mode");
                }
            });

            addtoolbarcoolbarButton("Runaway", e -> {
                runaway = !runaway;
                JButton source = (JButton) e.getSource();
                source.setText(runaway ? "Runaway: ON" : "Runaway: OFF");
            });

            addtoolbarcoolbarButton("STOP", e -> {
                stopped = true;
                if (timer != null) timer.stop();
                stopDVD();

                if (toolbarcoolbarWindow != null) {
                    toolbarcoolbarWindow.dispose();
                }
                dispose();
            });

            addtoolbarcoolbarButton("Exit", e -> System.exit(0));
        }

        private JButton addtoolbarcoolbarButton(String text, java.awt.event.ActionListener listener) {
            JButton b = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(new Color(120, 120, 120));
                    g2.drawLine(0, 0, getWidth() - 1, 0);
                    g2.drawLine(0, 0, 0, getHeight() - 1);

                    g2.setColor(Color.DARK_GRAY);
                    g2.drawLine(1, getHeight() - 1, getWidth() - 1, getHeight() - 1);
                    g2.drawLine(getWidth() - 1, 1, getWidth() - 1, getHeight() - 1);

                    super.paintComponent(g2);
                    g2.dispose();
                }
            };

            b.setPreferredSize(new Dimension(130, 30));
            b.setFont(toolbarcoolbarFont);
            b.setFocusable(false);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setForeground(toolbarcoolbarFgColor);
            b.addActionListener(listener);

            toolbarcoolbarContent.add(b);
            return b;
        }

        private void applytoolbarcoolbarTheme() {
            if (toolbarcoolbarContent != null) {
                toolbarcoolbarContent.setBackground(toolbarcoolbarBgColor);
                toolbarcoolbarContent.revalidate();
                toolbarcoolbarContent.repaint();
            }
        }
    }
}