import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileManager {
    private Map<String, File> filesByHash = new HashMap<>();

    public FileManager(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    try {
                        String hash = computeFileHash(file);
                        filesByHash.put(hash, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static String computeFileHash(File f) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(f.getPath()));
        MessageDigest msg = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = msg.digest(fileBytes);
        StringBuilder stringBuilder = new StringBuilder();
        for (byte ele : hashBytes) {
            stringBuilder.append(String.format("%02x", ele));
        }
        return stringBuilder.toString();
    }

    // Getters

    public File getFileByHash(String hash) {
        return filesByHash.get(hash);
    }

    public Map<String, File> getFilesByHash() {
        return filesByHash;
    }
}
