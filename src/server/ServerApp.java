package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ServerApp extends JFrame {
    private JTextArea logArea;
    private JList<ClientHandler> clientList;
    private Vector<ClientHandler> connectedClients;
    private JTextField pathField;
    private ServerSocket serverSocket;
    private JButton btnMonitor;
    private JButton btnBrowse;

    public ServerApp() {
        super("Hệ Thống Giám Sát Tập Tin - Server Center");
        connectedClients = new Vector<>();
        initUI();
        startServerThread();
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
            // Fallback to default if Nimbus fails
        }

        // Layout chính có khoảng cách
        setLayout(new BorderLayout(15, 15));
        // Padding xung quanh cửa sổ chính
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- PANEL TRÁI: DANH SÁCH CLIENT ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        TitledBorder leftBorder = BorderFactory.createTitledBorder("Danh sách Client kết nối");
        leftBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
        leftPanel.setBorder(leftBorder);

        clientList = new JList<>(connectedClients);
        clientList.setFont(new Font("Arial", Font.PLAIN, 14));
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Tùy chỉnh cách hiển thị item trong list
        clientList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("🖥️ " + value.getClientName()); // Thêm icon nhỏ bằng text
            label.setBorder(new EmptyBorder(5, 5, 5, 5));
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(new Color(51, 153, 255));
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
            return label;
        });
        JScrollPane scrollPaneLeft = new JScrollPane(clientList);
        scrollPaneLeft.setPreferredSize(new Dimension(200, 0)); // Chiều rộng cố định cho panel trái
        scrollPaneLeft.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        leftPanel.add(scrollPaneLeft, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);


        // --- PANEL GIỮA: LOG HOẠT ĐỘNG ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        TitledBorder centerBorder = BorderFactory.createTitledBorder("Nhật ký hoạt động chi tiết");
        centerBorder.setTitleFont(new Font("Arial", Font.BOLD, 12));
        centerPanel.setBorder(centerBorder);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Dùng font monospaced cho log trông chuyên nghiệp hơn
        logArea.setBackground(new Color(245, 245, 245)); // Màu nền xám nhẹ
        JScrollPane scrollPaneCenter = new JScrollPane(logArea);
        scrollPaneCenter.setBorder(null); // Xóa border mặc định của scrollpane để trông phẳng hơn
        centerPanel.add(scrollPaneCenter, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);


        // --- PANEL DƯỚI: ĐIỀU KHIỂN ---
        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 10, 10)); // 2 hàng, 1 cột, khoảng cách 10px
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Bảng điều khiển giám sát", TitledBorder.LEADING, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.DARK_GRAY
        ));

        // Hàng 1: Chọn đường dẫn
        JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
        pathPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel lblPath = new JLabel("Đường dẫn mục tiêu:");
        lblPath.setFont(new Font("Arial", Font.BOLD, 12));
        pathField = new JTextField();
        pathField.setFont(new Font("Arial", Font.PLAIN, 13));
        pathField.setToolTipText("Nhập đường dẫn hoặc yêu cầu Client chọn");

        btnBrowse = new JButton("📂 Yêu cầu Client chọn thư mục");
        btnBrowse.setMargin(new Insets(5, 10, 5, 10));
        btnBrowse.setFocusPainted(false);

        pathPanel.add(lblPath, BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);

        // Hàng 2: Nút bắt đầu
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        btnMonitor = new JButton("⚡ BẮT ĐẦU GIÁM SÁT NGAY ⚡");
        btnMonitor.setFont(new Font("Arial", Font.BOLD, 16));
        btnMonitor.setBackground(new Color(0, 153, 76));
        btnMonitor.setForeground(Color.WHITE);
        btnMonitor.setFocusPainted(false);
        btnMonitor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionPanel.add(btnMonitor, BorderLayout.CENTER);

        controlPanel.add(pathPanel);
        controlPanel.add(actionPanel);
        add(controlPanel, BorderLayout.SOUTH);

        // --- SỰ KIỆN ---
        btnMonitor.addActionListener(e -> startMonitoring());
        btnBrowse.addActionListener(e -> requestClientBrowse());

        setSize(900, 600); // Tăng kích thước mặc định
        setLocationRelativeTo(null); // Căn giữa màn hình
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void startServerThread() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(9999);
                appendLog(">>> Server đã khởi động tại port 9999. Đang chờ Client...");
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, this, connectedClients.size() + 1);
                    SwingUtilities.invokeLater(() -> {
                        connectedClients.add(handler);
                        clientList.updateUI();
                    });
                    handler.start();
                }
            } catch (IOException e) {
                appendLog("!!! Lỗi Server: " + e.getMessage());
            }
        }).start();
    }

    // Gửi yêu cầu Client mở JFileChooser
    private void requestClientBrowse() {
        ClientHandler selected = clientList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Client từ danh sách bên trái!", "Chưa chọn Client", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selected.sendBrowseRequest();
        appendLog(">>> Đã gửi yêu cầu chọn thư mục tới: " + selected.getClientName());
    }

    // Gửi lệnh giám sát chính thức
    private void startMonitoring() {
        ClientHandler selected = clientList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Client để ra lệnh!", "Chưa chọn Client", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Đường dẫn không được để trống.\nHãy nhập hoặc yêu cầu Client chọn.", "Thiếu thông tin", JOptionPane.ERROR_MESSAGE);
            pathField.requestFocus();
            return;
        }
        selected.sendMonitorCommand(path);
        appendLog(">>> Đã gửi lệnh BẮT ĐẦU GIÁM SÁT thư mục '" + path + "' tới " + selected.getClientName());
    }

    // Hàm gọi từ ClientHandler khi nhận được đường dẫn từ Client gửi về
    public void updatePathField(String path) {
        SwingUtilities.invokeLater(() -> {
            pathField.setText(path);
            pathField.setBackground(new Color(230, 255, 230)); // Highlight màu xanh nhạt báo hiệu thành công
            Timer timer = new Timer(1500, e -> pathField.setBackground(Color.WHITE)); // Trả lại màu trắng sau 1.5s
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Tự động cuộn xuống cuối
        });
    }

    public void removeClient(ClientHandler client) {
        SwingUtilities.invokeLater(() -> {
            connectedClients.remove(client);
            clientList.updateUI();
        });
    }

    public static void main(String[] args) {
        new ServerApp();
    }
}