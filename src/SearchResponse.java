import java.io.Serializable;
import java.util.List;

public class SearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> fileNames;

    public SearchResponse(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public List<String> getFileNames() {
        return fileNames;
    }
}
