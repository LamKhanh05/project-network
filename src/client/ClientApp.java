package client;

import common.ActionType;
import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private JTextArea logArea;
    private JTextField hostField;
    private JTextField portField; // MỚI: Ô nhập Port
    private JButton btnConnect;
    private JLabel statusLabel;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DirectoryWatcher currentWatcher;

    public ClientApp() {
        super("Client Node - Máy Trạm Giám Sát");
        initUI();
    }

    private void initUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- PANEL TRÊN: CẤU HÌNH KẾT NỐI (SỬA) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        TitledBorder topBorder = BorderFactory.createTitledBorder("Cấu hình kết nối Server");
        topBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
        topPanel.setBorder(topBorder);

        JLabel lblHost = new JLabel("IP Server:");
        hostField = new JTextField("localhost", 10);
        
        JLabel lblPort = new JLabel("Port:");
        portField = new JTextField("9999", 5); // Default port

        btnConnect = new JButton("🔗 Kết nối");
        btnConnect.setBackground(new Color(70, 130, 180));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);

        topPanel.add(lblHost);
        topPanel.add(hostField);
        topPanel.add(lblPort);     // Thêm label Port
        topPanel.add(portField);   // Thêm field Port
        topPanel.add(btnConnect);
        add(topPanel, BorderLayout.NORTH);

        // --- PANEL GIỮA: LOG ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Trạng thái hoạt động"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setText(">>> Nhập IP & Port rồi kết nối...\n");
        centerPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- PANEL DƯỚI: THANH TRẠNG THÁI ---
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" Trạng thái: Chưa kết nối");
        statusLabel.setForeground(Color.BLUE);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);

        btnConnect.addActionListener(e -> connectToServer());

        setSize(650, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        // Validate cơ bản
        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ IP và Port!");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên!");
            return;
        }

        btnConnect.setEnabled(false);
        hostField.setEditable(false);
        portField.setEditable(false);
        updateStatus("Đang kết nối tới " + host + ":" + port + "...");

        new Thread(() -> {
            try {
                // SỬA: Sử dụng port từ người dùng nhập
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                SwingUtilities.invokeLater(() -> {
                    logArea.append(">>> ✅ Đã kết nối thành công tới Server!\n");
                    btnConnect.setText("Đã kết nối");
                    btnConnect.setBackground(new Color(40, 167, 69));
                    updateStatus("Đã kết nối. Đang chờ lệnh...");
                });

                listenForCommands();

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(">>> ❌ Lỗi kết nối: " + ex.getMessage() + "\n");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("🔗 Kết nối");
                    hostField.setEditable(true);
                    portField.setEditable(true);
                    updateStatus("Lỗi kết nối.");
                });
            }
        }).start();
    }

    private void listenForCommands() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                
                if (msg.getType() == ActionType.START_MONITOR) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("\n[SERVER] Bắt đầu giám sát: " + msg.getContent() + "\n");
                        updateStatus("Đang giám sát: " + msg.getContent());
                    });
                    startWatcher(msg.getContent());
                } 
                else if (msg.getType() == ActionType.REQUEST_BROWSE) {
                    SwingUtilities.invokeLater(this::openDirectoryChooser);
                }
                else if (msg.getType() == ActionType.REQUEST_LIST_DIR) {
                    handleListDirRequest(msg.getContent());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            SwingUtilities.invokeLater(() -> {
                logArea.append("\n>>> ⚠️ Mất kết nối Server.\n");
                btnConnect.setEnabled(true);
                btnConnect.setText("🔗 Kết nối lại");
                hostField.setEditable(true);
                portField.setEditable(true);
            });
        }
    }

    private void handleListDirRequest(String path) {
        File[] files;
        if (path == null || path.trim().isEmpty()) {
            files = File.listRoots();
        } else {
            File dir = new File(path);
            files = (dir.exists() && dir.isDirectory()) ? dir.listFiles(File::isDirectory) : null;
        }

        String[] names = null;
        if (files != null) {
            names = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                names[i] = (path == null || path.trim().isEmpty()) ? files[i].getAbsolutePath() : files[i].getName();
            }
        }
        
        sendMessage(new Message(ActionType.RESPONSE_LIST_DIR, names, "List Dir Result"));
        String finalPath = path;
        SwingUtilities.invokeLater(() -> logArea.append("[SERVER] Đang duyệt: " + (finalPath.isEmpty() ? "Danh sách ổ đĩa" : finalPath) + "\n"));
    }

    private void openDirectoryChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sendMessage(new Message(ActionType.RETURN_PATH, chooser.getSelectedFile().getAbsolutePath(), "Đã chọn"));
        }
    }

    private void updateStatus(String text) { statusLabel.setText(" Trạng thái: " + text); }

    private void sendMessage(Message msg) {
        try {
            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void startWatcher(String path) {
        if (currentWatcher != null) currentWatcher.stopWatcher();
        currentWatcher = new DirectoryWatcher(path, out);
        new Thread(currentWatcher).start();
    }

    public static void main(String[] args) { new ClientApp(); }
}