package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ActionType type;
    private String content; // Đường dẫn folder hoặc thông báo
    private String details; // Thông tin thêm
    
    private String[] listData; 

    public Message(ActionType type, String content, String details) {
        this.type = type;
        this.content = content;
        this.details = details;
    }

    public Message(ActionType type, String[] listData, String details) {
        this.type = type;
        this.listData = listData;
        this.details = details;
    }

    public ActionType getType() { return type; }
    public String getContent() { return content; }
    public String getDetails() { return details; }
    
    public String[] getListData() { return listData; }

    @Override
    public String toString() {
        return "[" + type + "] " + content + " (" + details + ")";
    }
}