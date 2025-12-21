package client;

import common.ActionType;
import common.Message;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private JTextArea logArea;
    private JTextField hostField;
    private JButton btnConnect;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DirectoryWatcher currentWatcher; // Giữ tham chiếu để stop nếu cần

    public ClientApp() {
        super("Client Monitor Node");
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Panel trên: Cấu hình kết nối
        JPanel configPanel = new JPanel();
        hostField = new JTextField("localhost", 15);
        btnConnect = new JButton("Kết nối tới Server");
        configPanel.add(new JLabel("Server IP:"));
        configPanel.add(hostField);
        configPanel.add(btnConnect);
        add(configPanel, BorderLayout.NORTH);

        // Panel giữa: Log
        logArea = new JTextArea("Đang chờ kết nối...\n");
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        btnConnect.addActionListener(e -> connectToServer());

        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void connectToServer() {
        String host = hostField.getText();
        new Thread(() -> {
            try {
                socket = new Socket(host, 9999);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Đã kết nối tới Server!\n");
                    btnConnect.setEnabled(false);
                });

                // Lắng nghe lệnh từ Server
                listenForCommands();

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> logArea.append("Lỗi kết nối: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    private void listenForCommands() {
        try {
            while (true) {
                // Đọc tin nhắn từ Server
                Message msg = (Message) in.readObject();
                
                SwingUtilities.invokeLater(() -> logArea.append("Nhận lệnh: " + msg.toString() + "\n"));

                // Xử lý lệnh START_MONITOR
                if (msg.getType() == ActionType.START_MONITOR) {
                    startWatcher(msg.getContent());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            SwingUtilities.invokeLater(() -> logArea.append("Mất kết nối với Server.\n"));
        }
    }

    // Khởi tạo luồng giám sát file
    private void startWatcher(String path) {
        // Nếu đang chạy cái cũ thì tắt đi
        if (currentWatcher != null) {
            currentWatcher.stopWatcher();
        }

        // Tạo mới DirectoryWatcher (Class đã viết ở phần trước)
        currentWatcher = new DirectoryWatcher(path, out);
        new Thread(currentWatcher).start();
        
        SwingUtilities.invokeLater(() -> logArea.append(">> Đang giám sát thư mục: " + path + "\n"));
    }

    public static void main(String[] args) {
        new ClientApp();
    }
}