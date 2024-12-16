import java.io.Serializable;

public class FileBlockRequestMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileHash;
    private long offset;
    private int length;

    public FileBlockRequestMessage(String fileHash, long offset, int length) {
        this.fileHash = fileHash;
        this.offset = offset;
        this.length = length;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }
}