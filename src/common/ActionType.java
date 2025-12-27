package common;

public enum ActionType {
    START_MONITOR,  // Server ra lệnh bắt đầu giám sát
    FILE_CREATED,   // Client báo cáo file mới
    FILE_DELETED,   // Client báo cáo file bị xóa
    FILE_MODIFIED,  // Client báo cáo file bị sửa
    ERROR,          // Báo lỗi
    INFO,           // Thông báo chung
    
    REQUEST_BROWSE, // Server yêu cầu Client mở JFileChooser
    RETURN_PATH     // Client trả về đường dẫn đã chọn
}