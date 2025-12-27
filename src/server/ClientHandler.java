package server;

import common.Message;
import common.ActionType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerApp serverUI; 
    private String clientName;

    public ClientHandler(Socket socket, ServerApp serverUI, int clientID) {
        this.socket = socket;
        this.serverUI = serverUI;
        this.clientName = "Client-" + clientID;
    }

    public void sendMonitorCommand(String folderPath) {
        sendMessage(new Message(ActionType.START_MONITOR, folderPath, "Lệnh từ Server"));
    }

    public void sendListRequest(String path) {
        sendMessage(new Message(ActionType.REQUEST_LIST_DIR, path, "Get Dir List"));
    }

    private void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            serverUI.addLog(clientName, "Lỗi Gửi", "Không thể gửi tin nhắn tới Client");
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // Log kết nối đã được xử lý ở ServerApp khi accept socket
            
            while (true) {
                try {
                    Message msg = (Message) in.readObject();
                    
                    // --- XỬ LÝ LOG VÀO BẢNG ---
                    String action = "Thông báo";
                    String desc = msg.getContent();

                    switch (msg.getType()) {
                        case RETURN_PATH:
                            action = "Chọn Đường Dẫn";
                            serverUI.updatePathField(msg.getContent());
                            break;
                        case FILE_CREATED:
                            action = "Tạo mới";
                            desc = "File: " + msg.getContent() + " | " + msg.getDetails();
                            break;
                        case FILE_DELETED:
                            action = "Đã Xóa";
                            desc = "File: " + msg.getContent() + " | " + msg.getDetails();
                            break;
                        case FILE_MODIFIED:
                            action = "Chỉnh sửa";
                            desc = "File: " + msg.getContent() + " | " + msg.getDetails();
                            break;
                        case RESPONSE_LIST_DIR:
                            action = "Duyệt File";
                            desc = "Đã trả về danh sách thư mục";
                            serverUI.showRemoteDirSelection(msg.getListData());
                            break;
                        case ERROR:
                            action = "Lỗi Client";
                            desc = msg.getContent() + " - " + msg.getDetails();
                            break;
                        case INFO:
                            action = "Thông tin";
                            break;
                        default:
                            action = msg.getType().toString();
                    }

                    // Gọi hàm addLog thay vì appendLog
                    if (msg.getType() != ActionType.RESPONSE_LIST_DIR) { // Không log rác khi duyệt file
                        serverUI.addLog(clientName, action, desc);
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            // Log ngắt kết nối xử lý ở finally/removeClient
        } finally {
            try { socket.close(); } catch (IOException e) {}
            serverUI.removeClient(this);
        }
    }
    
    public String getClientName() { return clientName; }
    @Override
    public String toString() { return clientName; }
}