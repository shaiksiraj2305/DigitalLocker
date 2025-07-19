package Locker_app;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

public class DigitalLockerSwing {
    private static final String LOCKER_DIR = "DigitalLockerFiles";
    private static final String USER_FILE = LOCKER_DIR + File.separator+"users.txt";
    private static final long MAX_FILE_SIZE = 10*1024*1024; // 10MB LIMIT
    private static final int MIN_PASSWORD_LENGTH = 8;

    private JFrame frame;
    private JTextArea fileContentArea;
    private JTextField filenameField;
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JFileChooser fileChooser;
    private String loggedInUser;
    private String sessionPassword;

public static void main(String[]args){
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch (Exception e){
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(()-> new DigitalLockerSwing().createRegistrationGUI());
    }

    // Custom rounded button class with animations 
    static class RoundedButton extends JButton{
        private float scale = 1.0f;
        private boolean isHovered = false;

        public RoundedButton(String text)
        {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(new LineBorder(getBackground(),1,true));
            setFont(new Font("Arial",Font.BOLD,14));
            setForeground(Color.WHITE);
            setFocusPainted(false);

            addMouseListener(new MouseAdapter(){
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e){
                    isHovered = false;
                    repaint();

                }
                @Override
                public void mousePressed(MouseEvent e){
                    animatePress();
                }

            });
        }
        private void animatePress()
        {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask()
            {
             float delta = -0.05f;
             @Override
             public void run()
             {
                scale += delta;
                if(scale<=0.9f)delta = 0.05f;
                if(scale >=1.0f)
                {
                    scale = 1.0f;
                    timer.cancel();
                }
                repaint();
             }   
            } ,0,20);
        }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.translate(w/2,h/2);
            g2.scale(scale,scale);
            g2.translate(-w/2, -h/2);

            if(isHovered){
                g2.setColor(new Color (255,255,255,80));
                g2.fillRoundRect(0, 0, w, h, 20, 20);

            }
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, 20, 20);

            super.paintComponent(g2);
            g2.dispose();
        }
    }

    //Custom panel with gradient background
    static class GradientPanel extends JPanel{
        private Color color1 , color2;

        public GradientPanel(Color color1 , Color color2){
            this.color1 = color1;
            this.color2 = color2;
            setOpaque(false);
        }

        @Override 
        protected void paintComponent (Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gradient = new GradientPaint(0,0,color1,getWidth(),getHeight(),color2);
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // Custom Control panel for window controls
    private JPanel createControlPanel(JFrame frame){
        JPanel controlPanel = new JPanel (new FlowLayout(FlowLayout.RIGHT,5,5));
        controlPanel.setOpaque(false);

        JButton minimizeButton = new JButton("–");
        minimizeButton.setFont(new Font("Arial", Font.BOLD, 12));
        minimizeButton.setForeground(Color.WHITE);
        minimizeButton.setBackground(new Color(255, 165, 0)); // Yellow-orange
        minimizeButton.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        minimizeButton.setPreferredSize(new Dimension(30, 20));
        minimizeButton.addActionListener(e -> frame.setState(Frame.ICONIFIED));

        JButton maximizeRestoreButton = new JButton("🗖");
        maximizeRestoreButton.setFont(new Font("Arial", Font.BOLD, 12));
        maximizeRestoreButton.setForeground(Color.WHITE);
        maximizeRestoreButton.setBackground(new Color(50, 205, 50)); // Green
        maximizeRestoreButton.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        maximizeRestoreButton.setPreferredSize(new Dimension(30, 20));
        maximizeRestoreButton.addActionListener(e -> {
            if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                frame.setExtendedState(Frame.NORMAL);
                maximizeRestoreButton.setText("🗖");
            } else {
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                maximizeRestoreButton.setText("🗗");
            }
        });

        JButton closeButton = new JButton("X");
        closeButton.setFont(new Font("Arial", Font.BOLD, 12));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(255, 50, 50)); // Red
        closeButton.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        closeButton.setPreferredSize(new Dimension(30, 20));
        closeButton.addActionListener(e -> System.exit(0));

        controlPanel.add(minimizeButton);
        controlPanel.add(maximizeRestoreButton);
        controlPanel.add(closeButton);
        return controlPanel;
    }

    // Variables for window dragging 
    private Point initialClick;

    // Method to enable window dragging
    private void enableWindowDragging(JFrame frame , JPanel dragArea){
      dragArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                frame.getComponentAt(initialClick);
            }
        });

        dragArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int thisX = frame.getLocation().x;
                int thisY = frame.getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                frame.setLocation(X, Y);
            }
        });  
    }

    private void createRegistrationGUI(){
        JFrame regFrame = new JFrame("Digital Locker - Register");
        regFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        regFrame.setSize(400, 300);
        regFrame.setLocationRelativeTo(null);
        regFrame.setUndecorated(true);
        regFrame.setOpacity(0.0f);

        GradientPanel mainPanel = new GradientPanel(new Color(20, 40, 60), new Color(60, 20, 80));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2, true));

        // Top panel for dragging and controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(400, 30));
        JLabel titleLabel = new JLabel("Digital Locker - Register", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 255, 255));
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(createControlPanel(regFrame), BorderLayout.EAST);
        enableWindowDragging(regFrame, topPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel subtitleLabel = new JLabel("Create Account", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        subtitleLabel.setForeground(new Color(0, 255, 255));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPanel.add(subtitleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        contentPanel.add(userLabel, gbc);

        JTextField userField = new JTextField(15);
        userField.setFont(new Font("Arial", Font.PLAIN, 16));
        userField.setBackground(new Color(40, 50, 60));
        userField.setForeground(Color.WHITE);
        userField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        gbc.gridx = 1;
        gbc.gridy = 1;
        contentPanel.add(userField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(15);
        passField.setFont(new Font("Arial", Font.PLAIN, 16));
        passField.setBackground(new Color(40, 50, 60));
        passField.setForeground(Color.WHITE);
        passField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        gbc.gridx = 1;
        gbc.gridy = 2;
        contentPanel.add(passField, gbc);

        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        confirmPassLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        contentPanel.add(confirmPassLabel, gbc);

        JPasswordField confirmPassField = new JPasswordField(15);
        confirmPassField.setFont(new Font("Arial", Font.PLAIN, 16));
        confirmPassField.setBackground(new Color(40, 50, 60));
        confirmPassField.setForeground(Color.WHITE);
        confirmPassField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        gbc.gridx = 1;
        gbc.gridy = 3;
        contentPanel.add(confirmPassField, gbc);

        RoundedButton registerBtn = new RoundedButton("Register");
        registerBtn.setBackground(new Color(0, 200, 200));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(registerBtn, gbc);

        JButton loginLink = new JButton("Already have an account? Login");
        loginLink.setFont(new Font("Arial", Font.PLAIN, 14));
        loginLink.setBackground(new Color(0, 0, 0, 0));
        loginLink.setForeground(new Color(255, 105, 180));
        loginLink.setBorderPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        contentPanel.add(loginLink, gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        regFrame.add(mainPanel);

        Timer fadeTimer = new Timer();
        fadeTimer.scheduleAtFixedRate(new TimerTask() {
            float opacity = 0.0f;
            @Override
            public void run() {
                opacity += 0.05f;
                regFrame.setOpacity(Math.min(opacity, 1.0f));
                if (opacity >= 1.0f) fadeTimer.cancel();
            }
        }, 0, 50);

        registerBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String confirmPassword = new String(confirmPassField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(regFrame, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(regFrame, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (username.contains(":")) {
                JOptionPane.showMessageDialog(regFrame, "Username cannot contain ':'.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!isPasswordStrong(password)) {
                JOptionPane.showMessageDialog(regFrame, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long and contain a digit and special character.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                if (userExists(username)) {
                    JOptionPane.showMessageDialog(regFrame, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                saveUser(username, password);
                JOptionPane.showMessageDialog(regFrame, "Account created successfully! Please login.");
                regFrame.dispose();
                createLoginGUI();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(regFrame, "Error saving user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginLink.addActionListener(e -> {
            regFrame.dispose();
            createLoginGUI();
        });

        confirmPassField.addActionListener(e -> registerBtn.doClick());

        regFrame.setVisible(true);
    }

    private void createLoginGUI(){
        JFrame loginFrame = new JFrame("Digital Locker - Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(400, 250);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setUndecorated(true);
        loginFrame.setOpacity(0.0f);

        GradientPanel mainPanel = new GradientPanel(new Color(20, 40, 60), new Color(60, 20, 80));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2, true));

        // Top panel for dragging and controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(400, 30));
        JLabel titleLabel = new JLabel("Digital Locker - Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 255, 255));
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(createControlPanel(loginFrame), BorderLayout.EAST);
        enableWindowDragging(loginFrame, topPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel subtitleLabel = new JLabel("Login to Digital Locker", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        subtitleLabel.setForeground(new Color(0, 255, 255));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPanel.add(subtitleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        contentPanel.add(userLabel, gbc);

        JTextField userField = new JTextField(15);
        userField.setFont(new Font("Arial", Font.PLAIN, 16));
        userField.setBackground(new Color(40, 50, 60));
        userField.setForeground(Color.WHITE);
        userField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        gbc.gridx = 1;
        gbc.gridy = 1;
        contentPanel.add(userField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(15);
        passField.setFont(new Font("Arial", Font.PLAIN, 16));
        passField.setBackground(new Color(40, 50, 60));
        passField.setForeground(Color.WHITE);
        passField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        gbc.gridx = 1;
        gbc.gridy = 2;
        contentPanel.add(passField, gbc);

        RoundedButton loginBtn = new RoundedButton("Login");
        loginBtn.setBackground(new Color(0, 200, 200));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(loginBtn, gbc);

        JButton registerLink = new JButton("Need an account? Register");
        registerLink.setFont(new Font("Arial", Font.PLAIN, 14));
        registerLink.setBackground(new Color(0, 0, 0, 0));
        registerLink.setForeground(new Color(255, 105, 180));
        registerLink.setBorderPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 4;
        contentPanel.add(registerLink, gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        loginFrame.add(mainPanel);

        Timer fadeTimer = new Timer();
        fadeTimer.scheduleAtFixedRate(new TimerTask() {
            float opacity = 0.0f;
            @Override
            public void run() {
                opacity += 0.05f;
                loginFrame.setOpacity(Math.min(opacity, 1.0f));
                if (opacity >= 1.0f) fadeTimer.cancel();
            }
        }, 0, 50);

        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            try {
                if (verifyCredentials(username, password)) {
                    loggedInUser = username;
                    sessionPassword = password;
                    loginFrame.dispose();
                    showMainGUI();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passField.setText("");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(loginFrame, "Error reading user data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerLink.addActionListener(e -> {
            loginFrame.dispose();
            createRegistrationGUI();
        });

        passField.addActionListener(e -> loginBtn.doClick());

        loginFrame.setVisible(true);
    }

    private boolean userExists(String username) throws IOException{
        File file = new File(USER_FILE);
        if(!file.exists()) return false;
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while((line = br.readLine())!=null){
                String[] parts = line.split(":",2);
                if(parts.length == 2 && parts[0].equals(username)){
                    return true;
                }
            }
        }
        return false;
    }
    private void saveUser(String username, String password) throws IOException {
        File dir = new File(LOCKER_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + LOCKER_DIR);
        }
        String hashedPassword = hashPassword(password);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE, true))) {
            bw.write(username + ":" + hashedPassword);
            bw.newLine();
        }
    }

    private boolean verifyCredentials(String username, String password) throws IOException {
        File file = new File(USER_FILE);
        if (!file.exists()) return false;
        String hashedPassword = hashPassword(password);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(hashedPassword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    private boolean isPasswordStrong(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) return false;
        boolean hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasDigit && hasSpecial;
    }

    private void showMainGUI() throws IOException {
        File dir = new File(LOCKER_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + LOCKER_DIR);
        }

        frame = new JFrame("Digital Locker System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setOpacity(0.0f);

        GradientPanel mainPanel = new GradientPanel(new Color(20, 40, 60), new Color(60, 20, 80));
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2, true));

        // Top panel for dragging, title, and controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(800, 50));

        // Title and user info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel headerLabel = new JLabel("🔒 Digital Locker - " + loggedInUser, JLabel.LEFT);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(new Color(0, 255, 255));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        topPanel.add(headerPanel, BorderLayout.CENTER);
        topPanel.add(createControlPanel(frame), BorderLayout.EAST);
        enableWindowDragging(frame, topPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center panel for file list and content
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Arial", Font.PLAIN, 14));
        fileList.setBackground(new Color(40, 50, 60));
        fileList.setForeground(Color.WHITE);
        fileList.setSelectionBackground(new Color(0, 200, 200));
        refreshFileList();

        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setPreferredSize(new Dimension(200, 400));
        listScroll.setBorder(new LineBorder(new Color(100, 100, 100), 1));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fileContentArea = new JTextArea();
        fileContentArea.setFont(new Font("Arial", Font.PLAIN, 14));
        fileContentArea.setBackground(new Color(40, 50, 60));
        fileContentArea.setForeground(Color.WHITE);
        fileContentArea.setBorder(new LineBorder(new Color(100, 100, 100), 1));
        JScrollPane contentScroll = new JScrollPane(fileContentArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, contentScroll);
        splitPane.setDividerLocation(200);
        splitPane.setBackground(new Color(0, 0, 0, 0));
        splitPane.setBorder(null);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom panel for filename input and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filenamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filenamePanel.setOpaque(false);
        JLabel filenameLabel = new JLabel("📄 Filename:");
        filenameLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        filenameLabel.setForeground(Color.WHITE);
        filenameField = new JTextField(20);
        filenameField.setFont(new Font("Arial", Font.PLAIN, 16));
        filenameField.setBackground(new Color(50, 60, 70));
        filenameField.setForeground(Color.WHITE);
        filenameField.setBorder(new LineBorder(new Color(100, 100, 100), 1, true));
        filenamePanel.add(filenameLabel);
        filenamePanel.add(filenameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        RoundedButton uploadBtn = new RoundedButton("📤 Upload Files");
        RoundedButton saveBtn = new RoundedButton("💾 Save File");
        RoundedButton loadBtn = new RoundedButton("📥 Load File");
        RoundedButton deleteBtn = new RoundedButton("🗑️ Delete File");
        RoundedButton refreshBtn = new RoundedButton("🔄 Refresh List");
        RoundedButton logoutBtn = new RoundedButton("🚪 Logout");

        uploadBtn.setBackground(new Color(0, 200, 200));
        saveBtn.setBackground(new Color(0, 200, 200));
        loadBtn.setBackground(new Color(255, 105, 180));
        deleteBtn.setBackground(new Color(255, 50, 50));
        refreshBtn.setBackground(new Color(108, 117, 125));
        logoutBtn.setBackground(new Color(255, 165, 0));

        buttonPanel.add(uploadBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(logoutBtn);

        bottomPanel.add(filenamePanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(mainPanel);

        Timer fadeTimer = new Timer();
        fadeTimer.scheduleAtFixedRate(new TimerTask() {
            float opacity = 0.0f;
            @Override
            public void run() {
                opacity += 0.05f;
                frame.setOpacity(Math.min(opacity, 1.0f));
                if (opacity >= 1.0f) fadeTimer.cancel();
            }
        }, 0, 50);

        uploadBtn.addActionListener(e -> uploadFiles());
        saveBtn.addActionListener(e -> {
            if (verifyPassword()) saveFile();
        });
        loadBtn.addActionListener(e -> {
            if (verifyPassword()) loadFile();
        });
        deleteBtn.addActionListener(e -> {
            if (verifyPassword()) deleteFile();
        });
        refreshBtn.addActionListener(e -> refreshFileList());
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                frame.dispose();
                loggedInUser = null;
                sessionPassword = null;
                createLoginGUI();
            }
        });

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    filenameField.setText(selectedFile);
                    if (verifyPassword()) loadFile();
                }
            }
        });

        frame.setVisible(true);
    }
    private boolean verifyPassword() {
        String input = JOptionPane.showInputDialog(frame, "Enter your password:", "Security Check", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return false;
        if (!input.equals(sessionPassword)) {
            JOptionPane.showMessageDialog(frame, "Incorrect password!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void uploadFiles() {
        if (!verifyPassword()) return;

        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            if (files.length == 0) {
                JOptionPane.showMessageDialog(frame, "No files selected.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            for (File file : files) {
                try {
                    if (file.length() > MAX_FILE_SIZE) {
                        JOptionPane.showMessageDialog(frame, "File '" + file.getName() + "' exceeds 10MB limit.", "Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }

                    File destFile = new File(LOCKER_DIR + File.separator + file.getName());
                    if (destFile.exists()) {
                        int choice = JOptionPane.showConfirmDialog(frame,
                            "File '" + file.getName() + "' already exists. Overwrite?",
                            "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                        if (choice != JOptionPane.YES_OPTION) continue;
                    }

                    byte[] content = Files.readAllBytes(file.toPath());
                    byte[] encryptedContent = encryptDecrypt(content, sessionPassword);
                    Files.write(destFile.toPath(), encryptedContent);

                    JOptionPane.showMessageDialog(frame, "File '" + file.getName() + "' uploaded and encrypted successfully!");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, "Error uploading file '" + file.getName() + "': " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            refreshFileList();
        }
    }

    private void saveFile() {
        String filename = filenameField.getText().trim();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a filename.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (filename.contains(File.separator) || filename.contains("/") || filename.contains("\\") ||
            filename.contains(":") || filename.contains("*") || filename.contains("?") ||
            filename.contains("\"") || filename.contains("<") || filename.contains(">")) {
            JOptionPane.showMessageDialog(frame, "Filename contains invalid characters.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(LOCKER_DIR + File.separator + filename);
        try {
            String content = fileContentArea.getText();
            byte[] encryptedContent = encryptDecrypt(content.getBytes(), sessionPassword);
            Files.write(file.toPath(), encryptedContent);
            JOptionPane.showMessageDialog(frame, "File saved and encrypted successfully!");
            refreshFileList();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void loadFile() {
        String filename = filenameField.getText().trim();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a filename.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(LOCKER_DIR + File.separator + filename);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(frame, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            byte[] encryptedContent = Files.readAllBytes(file.toPath());
            byte[] decryptedContent = encryptDecrypt(encryptedContent, sessionPassword);
            fileContentArea.setText(new String(decryptedContent));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteFile() {
        String filename = filenameField.getText().trim();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a filename.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(LOCKER_DIR + File.separator + filename);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(frame, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete \"" + filename + "\"?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (file.delete()) {
                    JOptionPane.showMessageDialog(frame, "File deleted successfully.");
                    fileContentArea.setText("");
                    filenameField.setText("");
                    refreshFileList();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete the file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SecurityException e) {
                JOptionPane.showMessageDialog(frame, "Permission denied to delete file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void refreshFileList() {
        fileListModel.clear();
        File dir = new File(LOCKER_DIR);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !f.getName().equals("users.txt")) {
                    fileListModel.addElement(f.getName());
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Error accessing directory.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] encryptDecrypt(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return result;
    }


}
