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
        try {
            Message msg = new Message(ActionType.START_MONITOR, folderPath, "Lệnh từ Server");
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            serverUI.appendLog("Lỗi gửi lệnh tới " + clientName);
        }
    }

    // Hàm MỚI: Gửi yêu cầu mở JFileChooser
    public void sendBrowseRequest() {
        try {
            Message msg = new Message(ActionType.REQUEST_BROWSE, "", "");
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            serverUI.appendLog("Lỗi gửi yêu cầu chọn thư mục.");
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            serverUI.appendLog(clientName + " đã kết nối!");
            
            while (true) {
                try {
                    Message msg = (Message) in.readObject();
                    
                    // Nếu là tin nhắn trả về đường dẫn thư mục
                    if (msg.getType() == ActionType.RETURN_PATH) {
                        serverUI.appendLog("[" + clientName + "] Đã chọn thư mục: " + msg.getContent());
                        // Cập nhật lên UI của Server
                        serverUI.updatePathField(msg.getContent());
                    } else {
                        // Các tin nhắn thông thường (File thay đổi, v.v.)
                        serverUI.appendLog("[" + clientName + "] " + msg.toString());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            serverUI.appendLog(clientName + " đã ngắt kết nối.");
        } finally {
            try { socket.close(); } catch (IOException e) {}
            serverUI.removeClient(this);
        }
    }
    
    public String getClientName() {
        return clientName;
    }
}