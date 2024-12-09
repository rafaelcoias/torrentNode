import java.io.Serializable;

public class FileResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] fileData;
    private int dataIndex;

    public FileResponse(byte[] fileData, int dataIndex) {
        this.fileData = fileData;
        this.dataIndex = dataIndex;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public int getDataIndex() {
        return dataIndex;
    }
}
