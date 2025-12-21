package client;

import common.ActionType;
import common.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

// Class này chịu trách nhiệm giám sát thư mục VÀ CÁC THƯ MỤC CON (Recursive Watcher)
public class DirectoryWatcher implements Runnable {

    private final Path startPath;
    private final ObjectOutputStream out;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keys; // Lưu map giữa Key và Đường dẫn để biết sự kiện xảy ra ở đâu
    private boolean isRunning = true;

    public DirectoryWatcher(String pathString, ObjectOutputStream out) {
        this.startPath = Paths.get(pathString);
        this.out = out;
        this.keys = new HashMap<>();
        
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo WatchService", e);
        }
    }

    public void stopWatcher() {
        this.isRunning = false;
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm đăng ký giám sát cho một thư mục cụ thể
    private void register(Path dir) throws IOException {
        // Đăng ký nghe 3 sự kiện: Tạo, Xóa, Sửa
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        
        // Lưu vào map để sau này tra cứu ngược lại (từ Key -> ra Path)
        keys.put(key, dir);
    }

    // Hàm duyệt cây thư mục để đăng ký tất cả thư mục con (Đệ quy)
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        try {
            // 1. Kiểm tra đường dẫn gốc
            if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
                sendToServer(new Message(ActionType.ERROR, startPath.toString(), "Thư mục không tồn tại."));
                return;
            }

            // 2. Đăng ký giám sát cho thư mục gốc và TẤT CẢ thư mục con hiện có
            sendToServer(new Message(ActionType.INFO, startPath.toString(), "Đang quét và đăng ký giám sát toàn bộ cây thư mục..."));
            registerAll(startPath);
            sendToServer(new Message(ActionType.INFO, startPath.toString(), "Đã bắt đầu giám sát (bao gồm thư mục con)."));

            // 3. Vòng lặp xử lý sự kiện
            while (isRunning) {
                WatchKey key;
                try {
                    key = watchService.take(); // Chờ sự kiện (Blocking)
                } catch (InterruptedException x) {
                    return;
                }

                // Lấy ra đường dẫn thư mục xảy ra sự kiện từ Map
                Path dir = keys.get(key);
                if (dir == null) {
                    System.err.println("WatchKey không được nhận diện!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) continue;

                    // Lấy tên file/folder bị tác động
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name); // Tạo đường dẫn đầy đủ

                    // Log ra console client để debug
                    // System.out.format("%s: %s\n", event.kind().name(), child);

                    // Xử lý gửi tin về Server
                    ActionType msgType = null;
                    if (kind == ENTRY_CREATE) {
                        msgType = ActionType.FILE_CREATED;
                        
                        // QUAN TRỌNG: Nếu cái vừa tạo ra là Thư Mục, phải đăng ký giám sát nó ngay!
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            try {
                                registerAll(child);
                                sendToServer(new Message(ActionType.INFO, child.toString(), "Đã thêm thư mục con mới vào giám sát."));
                            } catch (IOException x) {
                                sendToServer(new Message(ActionType.ERROR, child.toString(), "Lỗi không thể giám sát thư mục mới."));
                            }
                        }
                    } 
                    else if (kind == ENTRY_DELETE) msgType = ActionType.FILE_DELETED;
                    else if (kind == ENTRY_MODIFY) msgType = ActionType.FILE_MODIFIED;

                    if (msgType != null) {
                        // Gửi đường dẫn tương đối hoặc tuyệt đối tùy nhu cầu
                        sendToServer(new Message(msgType, child.getFileName().toString(), "Tại: " + dir.toString()));
                    }
                }

                // Reset key, nếu false nghĩa là thư mục đó không còn truy cập được (vd bị xóa)
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    // Nếu thư mục gốc bị xóa thì dừng luôn
                    if (keys.isEmpty()) { 
                        break; 
                    }
                }
            }
        } catch (IOException e) {
            sendToServer(new Message(ActionType.ERROR, startPath.toString(), "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private void sendToServer(Message msg) {
        try {
            // Synchronized để tránh xung đột nếu nhiều event xảy ra cùng lúc
            synchronized (out) { 
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Mất kết nối tới Server: " + e.getMessage());
            isRunning = false;
        }
    }
}