package server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// ServerUI: Đóng vai trò là View và Controller chính của Server
public class ServerApp extends JFrame {
    private JTextArea logArea;
    private JList<ClientHandler> clientList;
    private Vector<ClientHandler> connectedClients;
    private JTextField pathField;
    private ServerSocket serverSocket;

    public ServerApp() {
        super("Server Monitor System");
        connectedClients = new Vector<>();
        initUI();
        startServerThread();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Panel trái: Danh sách Client
        clientList = new JList<>(connectedClients);
        // Renderer để hiển thị tên Client trong JList thay vì mã hash object
        clientList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getClientName());
            label.setOpaque(true);
            if (isSelected) label.setBackground(Color.LIGHT_GRAY);
            else label.setBackground(Color.WHITE);
            return label;
        });
        add(new JScrollPane(clientList), BorderLayout.WEST);

        // Panel giữa: Log hoạt động
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Panel dưới: Điều khiển
        JPanel controlPanel = new JPanel(new BorderLayout());
        pathField = new JTextField("D:\\TestFolder"); // Mặc định để test
        pathField.setBorder(BorderFactory.createTitledBorder("Nhập đường dẫn trên máy Client cần giám sát:"));
        JButton btnMonitor = new JButton("Bắt đầu giám sát");

        btnMonitor.addActionListener(e -> startMonitoring());

        controlPanel.add(pathField, BorderLayout.CENTER);
        controlPanel.add(btnMonitor, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Logic: Khởi động Server Socket lắng nghe kết nối
    private void startServerThread() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(9999); // Port 9999
                appendLog("Server đang chạy tại port 9999...");
                while (true) {
                    Socket socket = serverSocket.accept();
                    // Tạo thread riêng cho client mới (ClientHandler đã viết ở phần trước)
                    ClientHandler handler = new ClientHandler(socket, this, connectedClients.size() + 1);
                    SwingUtilities.invokeLater(() -> {
                        connectedClients.add(handler);
                        clientList.updateUI();
                    });
                    handler.start();
                }
            } catch (IOException e) {
                appendLog("Lỗi Server: " + e.getMessage());
            }
        }).start();
    }

    // Logic: Gửi lệnh giám sát
    private void startMonitoring() {
        ClientHandler selected = clientList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Client trong danh sách!");
            return;
        }
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đường dẫn thư mục!");
            return;
        }
        
        // Gọi hàm gửi lệnh trong ClientHandler
        selected.sendMonitorCommand(path);
        appendLog("Đã gửi yêu cầu giám sát '" + path + "' tới " + selected.getClientName());
    }

    // Các hàm tiện ích công khai để ClientHandler gọi cập nhật UI
    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> logArea.append(text + "\n"));
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