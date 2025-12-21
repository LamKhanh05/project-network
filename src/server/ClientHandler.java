package server;

import common.Message;
import common.ActionType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Class này đại diện cho 1 Client đang kết nối tới Server
// Nó chạy song song (Thread) để Server có thể quản lý nhiều Client cùng lúc [cite: 15]
public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerApp serverUI; // Tham chiếu đến giao diện để cập nhật log
    private String clientName;

    public ClientHandler(Socket socket, ServerApp serverUI, int clientID) {
        this.socket = socket;
        this.serverUI = serverUI;
        this.clientName = "Client-" + clientID;
    }

    // Hàm để Server UI gọi khi muốn gửi lệnh giám sát xuống Client này
    // [cite: 11] Chọn client cần giám sát, chọn thư mục
    public void sendMonitorCommand(String folderPath) {
        try {
            Message msg = new Message(ActionType.START_MONITOR, folderPath, "Yêu cầu từ Server");
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            serverUI.appendLog("Lỗi gửi lệnh tới " + clientName);
        }
    }

    @Override
    public void run() {
        try {
            // Setup luồng vào/ra
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            serverUI.appendLog(clientName + " đã kết nối!");
            
            // Lắng nghe liên tục các báo cáo từ Client gửi lên
            while (true) {
                try {
                    Message msg = (Message) in.readObject();
                    // Khi nhận tin, hiển thị lên Server UI
                    serverUI.appendLog("[" + clientName + "] " + msg.toString());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            serverUI.appendLog(clientName + " đã ngắt kết nối.");
        } finally {
            // Dọn dẹp kết nối
            try { socket.close(); } catch (IOException e) {}
            serverUI.removeClient(this); // Xóa khỏi danh sách quản lý
        }
    }
    
    public String getClientName() {
        return clientName;
    }
}