import java.io.Serializable;

public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String searchTerm;
    private String senderNode; 

    public SearchRequest(String searchTerm, String senderNode) {
        this.searchTerm = searchTerm;
        this.senderNode = senderNode;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public String getSenderNode() {
        return senderNode;
    }
}
