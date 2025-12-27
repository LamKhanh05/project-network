package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class ServerApp extends JFrame {
    private JTable logTable;
    private DefaultTableModel tableModel;
    
    private JList<ClientHandler> clientList;
    private Vector<ClientHandler> connectedClients;
    private JTextField pathField;
    private JTextField portField; 
    private JButton btnMonitor;
    private JButton btnBrowse;
    private JButton btnStartServer; 

    public ServerApp() {
        super("Hệ Thống Giám Sát Tập Tin - Server Center");
        connectedClients = new Vector<>();
        initUI();
    }

    private void initUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- PANEL TRÁI: DANH SÁCH CLIENT ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Danh sách Client"));
        clientList = new JList<>(connectedClients);
        leftPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        
        // Panel con cấu hình Server (Port + Start)
        JPanel serverConfigPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        serverConfigPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        serverConfigPanel.add(new JLabel("Port lắng nghe:"));
        portField = new JTextField("9999");
        btnStartServer = new JButton("Khởi động Server");
        serverConfigPanel.add(portField);
        serverConfigPanel.add(btnStartServer);
        
        leftPanel.add(serverConfigPanel, BorderLayout.SOUTH);
        
        leftPanel.setPreferredSize(new Dimension(200, 0));
        add(leftPanel, BorderLayout.WEST);

        // --- PANEL GIỮA: LOG (BẢNG) ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Nhật ký giám sát tập tin"));

        String[] columns = {"Thời gian", "Client", "Hành động", "Mô tả chi tiết"};
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho edit
            }
        };
        
        logTable = new JTable(tableModel);
        logTable.setFillsViewportHeight(true);
        logTable.setRowHeight(30);
        
        // Căn chỉnh độ rộng cột
        logTable.getColumnModel().getColumn(0).setPreferredWidth(90);  // Thời gian
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Client
        logTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Hành động
        logTable.getColumnModel().getColumn(3).setPreferredWidth(400); // Mô tả 

        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                                                         boolean isSelected, boolean hasFocus, 
                                                         int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                String action = (String) table.getModel().getValueAt(row, 2);
                
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }

                if ("Tạo mới".equals(action)) {
                    c.setForeground(new Color(0, 153, 51));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } 
                else if ("Đã Xóa".equals(action)) {
                    c.setForeground(Color.RED); 
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } 
                else if ("Chỉnh sửa".equals(action)) {
                    c.setForeground(Color.BLUE);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } 
                else if ("Lỗi Client".equals(action) || "Lỗi Server".equals(action)) {
                    c.setForeground(new Color(204, 0, 0)); 
                }
                else {
                    c.setForeground(Color.BLACK); 
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                
                return c;
            }
        });

        centerPanel.add(new JScrollPane(logTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- PANEL DƯỚI: ĐIỀU KHIỂN ---
        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Bảng điều khiển"));

        JPanel pathPanel = new JPanel(new BorderLayout(10, 0));
        pathPanel.add(new JLabel("Đường dẫn:"), BorderLayout.WEST);
        pathField = new JTextField();
        btnBrowse = new JButton("📂 Duyệt file từ xa");
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);

        btnMonitor = new JButton("⚡ BẮT ĐẦU GIÁM SÁT NGAY ⚡");
        btnMonitor.setBackground(new Color(0, 153, 76));
        btnMonitor.setForeground(Color.WHITE);
        btnMonitor.setFont(new Font("Arial", Font.BOLD, 14));
        btnMonitor.setEnabled(false); // Chưa start server thì chưa cho bấm

        controlPanel.add(pathPanel);
        controlPanel.add(btnMonitor);
        add(controlPanel, BorderLayout.SOUTH);

        // --- EVENTS ---
        btnStartServer.addActionListener(e -> startServerAction());
        btnMonitor.addActionListener(e -> startMonitoring());
        btnBrowse.addActionListener(e -> requestRemoteBrowse());

        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void startServerAction() {
        String portStr = portField.getText().trim();
        try {
            int port = Integer.parseInt(portStr);
            startServerThread(port);
            btnStartServer.setEnabled(false);
            portField.setEditable(false);
            btnMonitor.setEnabled(true);
            addLog("System", "Khởi động", "Server đang lắng nghe tại port " + port);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!");
        }
    }

    private void startServerThread(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, this, connectedClients.size() + 1);
                    SwingUtilities.invokeLater(() -> {
                        connectedClients.add(handler);
                        clientList.updateUI();
                        addLog(handler.getClientName(), "Kết nối", "Đã kết nối vào hệ thống");
                    });
                    handler.start();
                }
            } catch (IOException e) {
                addLog("System", "Lỗi Server", e.getMessage());
            }
        }).start();
    }

    public void addLog(String clientName, String action, String description) {
        SwingUtilities.invokeLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            tableModel.addRow(new Object[]{time, clientName, action, description});
            
            // Tự động cuộn xuống cuối bảng
            logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount()-1, 0, true));
        });
    }

    private void requestRemoteBrowse() {
        ClientHandler selected = clientList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn Client!");
            return;
        }
        String currentPath = pathField.getText().trim();
        selected.sendListRequest(currentPath);
        addLog(selected.getClientName(), "Yêu cầu", "Duyệt thư mục: " + (currentPath.isEmpty() ? "Gốc" : currentPath));
    }

    public void showRemoteDirSelection(String[] folders) {
        SwingUtilities.invokeLater(() -> {
            if (folders == null || folders.length == 0) {
                JOptionPane.showMessageDialog(this, "Thư mục trống hoặc không thể truy cập!");
                return;
            }
            String selected = (String) JOptionPane.showInputDialog(
                this, "Chọn thư mục:", "Duyệt File Từ Xa", JOptionPane.QUESTION_MESSAGE, null, folders, folders[0]
            );
            if (selected != null) {
                String current = pathField.getText().trim();
                String newPath = current.isEmpty() ? selected : (current.endsWith(File.separator) ? current + selected : current + File.separator + selected);
                pathField.setText(newPath);
                requestRemoteBrowse(); 
            }
        });
    }

    private void startMonitoring() {
        ClientHandler selected = clientList.getSelectedValue();
        
        if (selected == null) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng chọn một Client trong danh sách bên trái!", 
                "Chưa chọn Client", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập đường dẫn hoặc yêu cầu Client chọn file trước khi bắt đầu!", 
                "Thiếu đường dẫn giám sát", 
                JOptionPane.ERROR_MESSAGE); 
            pathField.requestFocus(); 
            return;
        }

        selected.sendMonitorCommand(path);
        
        addLog(selected.getClientName(), "Lệnh Giám sát", "Bắt đầu theo dõi: " + path);
    }

    public void updatePathField(String path) {
        SwingUtilities.invokeLater(() -> pathField.setText(path));
    }

    public void removeClient(ClientHandler client) {
        SwingUtilities.invokeLater(() -> {
            connectedClients.remove(client);
            clientList.updateUI();
            addLog(client.getClientName(), "Ngắt kết nối", "Client đã thoát.");
        });
    }

    public static void main(String[] args) { new ServerApp(); }
}