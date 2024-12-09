import java.io.Serializable;
import java.util.List;

public class UpdateNeighborsRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> neighbors;

    public UpdateNeighborsRequest(List<String> neighbors) {
        this.neighbors = neighbors;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }
}
