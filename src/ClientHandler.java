import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

@Override
public void run() {
    try {
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();  // Limpar o buffer de saída, para evitar bug de ligações
        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

        System.out.println("Objetos inicializados com sucesso para o cliente: " + clientSocket.getRemoteSocketAddress());

        while (true) {
            Message message = (Message) in.readObject();
            System.out.println("Mensagem recebida: " + message.getClass().getSimpleName());

            if (message instanceof Message.SearchRequest) {
                handleSearchRequest((Message.SearchRequest) message, out);
            } else if (message instanceof Message.FileRequest) {
                handleFileRequest((Message.FileRequest) message, out);
            }
        }
    } catch (IOException | ClassNotFoundException e) {
        System.err.println("Erro de comunicação com o cliente: " + e.getMessage());
    } finally {
        try {
            clientSocket.close();
            System.out.println("Conexão fechada com o cliente: " + clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


    // Funções de pesquisa 
    private void handleSearchRequest(Message.SearchRequest request, ObjectOutputStream out) throws IOException {
        String searchTerm = request.getSearchTerm();
        List<String> foundFiles = searchLocalFiles(searchTerm);

        Message.SearchResponse response = new Message.SearchResponse(foundFiles);
        out.writeObject(response);
        out.flush();
    }

    private List<String> searchLocalFiles(String searchTerm) {
        List<String> filesFound = new ArrayList<>();
        File directory = new File(System.getProperty("user.dir")); 

        for (File file : directory.listFiles())
            if (file.isFile() && file.getName().contains(searchTerm)) 
                filesFound.add(file.getName());
        return filesFound;
    }

    // Funções de download
    private void handleFileRequest(Message.FileRequest request, ObjectOutputStream out) throws IOException {
        String fileName = request.getFileName();
        int dataIndex = request.getDataIndex();

        File file = new File(fileName);
        if (file.exists()) {
            byte[] dataData = readData(file, dataIndex);
            Message.FileResponse response = new Message.FileResponse(dataData, dataIndex);
            out.writeObject(response);
            out.flush();
        }
    }

    private byte[] readData(File file, int dataIndex) throws IOException {
        int dataSize = 1024;  // TODO: Qual o tamanho de ideal dos dados?
        byte[] buffer = new byte[dataSize];

        try (var fileInputStream = new java.io.FileInputStream(file)) {
            fileInputStream.skip((long) dataIndex * dataSize);
            int bytesRead = fileInputStream.read(buffer);

            // Caso o último bloco seja menor que o tamanho do buffer 
            if (bytesRead < dataSize) {
                byte[] lastData = new byte[bytesRead];
                System.arraycopy(buffer, 0, lastData, 0, bytesRead);
                return lastData;
            }
        }
        return buffer;
    }
}
