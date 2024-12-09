import java.io.Serializable;

public class FileRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private int dataIndex;

    public FileRequest(String fileName, int dataIndex) {
        this.fileName = fileName;
        this.dataIndex = dataIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public int getDataIndex() {
        return dataIndex;
    }
}
