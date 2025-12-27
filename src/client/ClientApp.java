package client;

import common.ActionType;
import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private JTextArea logArea;
    private JTextField hostField;
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
        // Cài đặt Look and Feel hiện đại (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
           // Fallback
        }

        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- PANEL TRÊN: CẤU HÌNH KẾT NỐI ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        TitledBorder topBorder = BorderFactory.createTitledBorder("Cấu hình kết nối Server");
        topBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
        topPanel.setBorder(topBorder);

        JLabel lblHost = new JLabel("Địa chỉ IP Server:");
        lblHost.setFont(new Font("Arial", Font.PLAIN, 13));
        hostField = new JTextField("localhost", 15);
        hostField.setFont(new Font("Arial", Font.PLAIN, 13));

        btnConnect = new JButton("🔗 Kết nối");
        btnConnect.setFont(new Font("Arial", Font.BOLD, 13));
        btnConnect.setFocusPainted(false);
        btnConnect.setBackground(new Color(70, 130, 180));
        btnConnect.setForeground(Color.WHITE);

        topPanel.add(lblHost);
        topPanel.add(hostField);
        topPanel.add(btnConnect);
        add(topPanel, BorderLayout.NORTH);


        // --- PANEL GIỮA: LOG ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        TitledBorder centerBorder = BorderFactory.createTitledBorder("Trạng thái hoạt động của Client");
        centerBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
        centerPanel.setBorder(centerBorder);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(new Color(240, 240, 245));
        logArea.setText(">>> Vui lòng nhập IP Server và bấm Kết nối...\n");
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(null);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- PANEL DƯỚI: THANH TRẠNG THÁI ---
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        statusLabel = new JLabel(" Trạng thái: Chưa kết nối");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);


        btnConnect.addActionListener(e -> connectToServer());

        setSize(600, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void connectToServer() {
        String host = hostField.getText();
        btnConnect.setEnabled(false);
        btnConnect.setText("Đang kết nối...");
        updateStatus("Đang thử kết nối tới " + host + "...");

        new Thread(() -> {
            try {
                socket = new Socket(host, 9999);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                SwingUtilities.invokeLater(() -> {
                    logArea.append(">>> ✅ Đã kết nối thành công tới Server!\n");
                    btnConnect.setText("Đã kết nối");
                    btnConnect.setBackground(new Color(40, 167, 69)); // Màu xanh lá
                    hostField.setEditable(false);
                    updateStatus("Đã kết nối tới " + host + ". Đang chờ lệnh...");
                });

                listenForCommands();

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(">>> ❌ Lỗi kết nối: " + ex.getMessage() + "\nTry again.\n");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("🔗 Kết nối");
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
                        logArea.append("\n[SERVER COMMAND] Yêu cầu bắt đầu giám sát thư mục:\n -> " + msg.getContent() + "\n");
                        updateStatus("Đang giám sát: " + msg.getContent());
                    });
                    startWatcher(msg.getContent());
                } 
                else if (msg.getType() == ActionType.REQUEST_BROWSE) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("\n[SERVER COMMAND] Yêu cầu chọn thư mục...\n");
                        openDirectoryChooser();
                    });
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            SwingUtilities.invokeLater(() -> {
                logArea.append("\n>>> ⚠️ Đã mất kết nối với Server.\n");
                btnConnect.setEnabled(true);
                hostField.setEditable(true);
                btnConnect.setText("🔗 Kết nối lại");
                btnConnect.setBackground(new Color(70, 130, 180));
                updateStatus("Mất kết nối.");
            });
        }
    }

    private void openDirectoryChooser() {
        // Sử dụng JFileChooser với giao diện hệ thống để trông tự nhiên nhất
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục để Server giám sát");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = chooser.showOpenDialog(this);

        // Trả lại giao diện Nimbus cho ứng dụng chính
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}


        if (result == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            sendMessage(new Message(ActionType.RETURN_PATH, path, "Người dùng đã chọn thư mục"));
            logArea.append(">>> Đã chọn: " + path + ". Đang gửi về Server...\n");
        } else {
            sendMessage(new Message(ActionType.INFO, "N/A", "Người dùng hủy chọn thư mục"));
            logArea.append(">>> Người dùng đã hủy chọn thư mục.\n");
        }
    }

    private void updateStatus(String text) {
        statusLabel.setText(" Trạng thái: " + text);
    }

    private void sendMessage(Message msg) {
        try {
            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startWatcher(String path) {
        if (currentWatcher != null) {
            currentWatcher.stopWatcher();
        }
        currentWatcher = new DirectoryWatcher(path, out);
        new Thread(currentWatcher).start();
    }

    public static void main(String[] args) {
        new ClientApp();
    }
}