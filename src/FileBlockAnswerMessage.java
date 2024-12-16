import java.io.Serializable;

public class FileBlockAnswerMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileHash;
    private long offset;
    private byte[] data;

    public FileBlockAnswerMessage(String fileHash, long offset, byte[] data) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.data = data;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }
}
