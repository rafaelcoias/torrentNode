import java.io.Serializable;

public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L; // Evita problemas de versão
    private String searchTerm;

    public SearchRequest(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm;
    }
}
