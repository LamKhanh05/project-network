package common;

import java.io.Serializable;

// Class đóng gói dữ liệu truyền đi. 
// Implements Serializable để có thể gửi qua mạng (Socket).
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ActionType type;
    private String content; // Có thể là đường dẫn folder hoặc tên file thay đổi
    private String details; // Thông tin thêm (nếu cần)

    public Message(ActionType type, String content, String details) {
        this.type = type;
        this.content = content;
        this.details = details;
    }

    // Getters
    public ActionType getType() { return type; }
    public String getContent() { return content; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return "[" + type + "] " + content + " (" + details + ")";
    }
}