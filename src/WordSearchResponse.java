import java.io.Serializable;
import java.util.List;

public class WordSearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<FileSearchResult> results;

    public WordSearchResponse(List<FileSearchResult> results) {
        this.results = results;
    }

    public List<FileSearchResult> getResults() {
        return results;
    }
}
