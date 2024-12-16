import java.io.Serializable;

public class FileSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileHash;
    private long fileSize;
    private String fileName;
    private String nodeAddress;

    public FileSearchResult(String fileHash, long fileSize, String fileName, String nodeAddress) {
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.nodeAddress = nodeAddress;
    }

    // Getters

    public String getFileHash() {
        return fileHash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }
}
