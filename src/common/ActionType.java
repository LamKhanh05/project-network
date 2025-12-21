package common;

import java.io.Serializable;


// Enum xác định loại hành động (Thêm, Xóa, Sửa, hoặc Lỗi/Thông báo)
//: Cần thông báo xóa/thêm/chỉnh sửa
public enum ActionType {
    START_MONITOR,  // Server ra lệnh bắt đầu giám sát
    FILE_CREATED,   // Client báo cáo file mới
    FILE_DELETED,   // Client báo cáo file bị xóa
    FILE_MODIFIED,  // Client báo cáo file bị sửa
    ERROR,          // Báo lỗi (ví dụ: không tìm thấy folder)
    INFO            // Thông báo chung
}

