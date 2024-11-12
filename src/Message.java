import java.io.Serializable;
import java.util.List;

public abstract class Message implements Serializable {
    
    // Pedido de pesquisa de ficheiros
    public static class SearchRequest extends Message {
        private String searchTerm;

        public SearchRequest(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public String getSearchTerm() {
            return searchTerm;
        }
    }

    // Resposta dos ficheiros encontrados
    public static class SearchResponse extends Message {
        private List<String> fileNames;

        public SearchResponse(List<String> fileNames) {
            this.fileNames = fileNames;
        }

        public List<String> getFileNames() {
            return fileNames;
        }
    }

    // Pedido de um ficheiro
    public static class FileRequest extends Message {
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

    // Resposta com um ficheiro
    public static class FileResponse extends Message {
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
}
