package common;

public enum ActionType {
    START_MONITOR,  // Server ra lệnh bắt đầu giám sát
    FILE_CREATED,   // Client báo cáo file mới
    FILE_DELETED,   // Client báo cáo file bị xóa
    FILE_MODIFIED,  // Client báo cáo file bị sửa
    ERROR,          // Báo lỗi
    INFO,           // Thông báo chung
    
    REQUEST_BROWSE, // Server yêu cầu Client mở JFileChooser (Cách cũ)
    RETURN_PATH,    // Client trả về đường dẫn đã chọn

    REQUEST_LIST_DIR,  // Server yêu cầu lấy danh sách thư mục con
    RESPONSE_LIST_DIR  // Client trả về danh sách tên thư mục
}