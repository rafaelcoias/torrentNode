import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String fileDirectory;
    private List<String> connectedNodes = new ArrayList<>();

    public ClientHandler(Socket clientSocket, String fileDirectory) {
        this.clientSocket = clientSocket;
        this.fileDirectory = fileDirectory;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("Object streams criados para o cliente: " + clientSocket.getRemoteSocketAddress());

            while (true) {
                Object message = in.readObject();
                System.out.println("Mensagem recebida: " + message.getClass().getSimpleName());

                if (message instanceof String && ((String) message).startsWith("CONNECT")) {
                    String neighbor = ((String) message).split(" ")[1];
                    addNeighbor(neighbor);
                    System.out.println("Novo vizinho adicionado: " + neighbor);
                    out.writeObject("OK CONNECTED");
                    out.flush();
                    continue;
                }

                if (message instanceof UpdateNeighborsRequest) {
                    UpdateNeighborsRequest updateRequest = (UpdateNeighborsRequest) message;
                    List<String> newNeighbors = updateRequest.getNeighbors();
                    System.out.println("UpdateNeighborsRequest recebido com vizinhos: " + newNeighbors);
                
                    for (String neighbor : newNeighbors) {
                        addNeighbor(neighbor);
                    }
                    System.out.println("Lista de vizinhos após processamento do UpdateNeighborsRequest: " + connectedNodes);
                    continue;
                }
                            

                if (message instanceof SearchRequest) {
                    handleSearchRequest((SearchRequest) message, out);
                } else {
                    System.err.println("Mensagem não reconhecida: " + message.getClass().getSimpleName());
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
    private void handleSearchRequest(SearchRequest request, ObjectOutputStream out) throws IOException {
        String searchTerm = request.getSearchTerm();
        System.out.println("Termo de pesquisa recebido: " + searchTerm);

        // Procurar ficheiros locais
        List<String> foundFiles = searchLocalFiles(searchTerm);
        System.out.println("Ficheiros locais encontrados: " + foundFiles);

        // Propagar o pedido de pesquisa para os vizinhos
        List<String> neighborFiles = searchInNeighbors(searchTerm);
        System.out.println("Ficheiros encontrados nos vizinhos: " + neighborFiles);

        // Combinar os resultados locais e os dos vizinhos
        foundFiles.addAll(neighborFiles);

        // Enviar a resposta de volta para o cliente
        SearchResponse response = new SearchResponse(foundFiles);
        out.writeObject(response);
        out.flush();
        System.out.println("Resposta de pesquisa enviada: " + foundFiles);
    }

    private List<String> searchInNeighbors(String searchTerm) {
        System.out.println("Iniciando pesquisa nos vizinhos. Lista de vizinhos atual: " + connectedNodes);
        List<String> allFiles = new ArrayList<>();
    
        for (String neighbor : connectedNodes) {
            System.out.println("Tentando conectar ao vizinho: " + neighbor);
            try {
                String[] parts = neighbor.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
    
                try (Socket socket = new Socket(ip, port);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
    
                    out.writeObject(new SearchRequest(searchTerm));
                    out.flush();
                    System.out.println("Pedido de pesquisa enviado para vizinho: " + neighbor);
    
                    Object response = in.readObject();
                    if (response instanceof SearchResponse) {
                        SearchResponse searchResponse = (SearchResponse) response;
                        allFiles.addAll(searchResponse.getFileNames());
                        System.out.println("Resposta recebida do vizinho " + neighbor + ": " + searchResponse.getFileNames());
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao comunicar com o vizinho " + neighbor + ": " + e.getMessage());
            }
        }
        return allFiles;
    }
    

    public void addNeighbor(String ipPort) {
        System.out.println("Tentando adicionar vizinho: " + ipPort);
        if (!connectedNodes.contains(ipPort)) {
            connectedNodes.add(ipPort);
            System.out.println("Vizinho adicionado com sucesso: " + ipPort);
            System.out.println("Lista de vizinhos atualizada: " + connectedNodes);
    
            // Propagar lista atualizada de vizinhos
            for (String neighbor : connectedNodes) {
                if (!neighbor.equals(ipPort)) {
                    try (Socket socket = new Socket(neighbor.split(":")[0], Integer.parseInt(neighbor.split(":")[1]));
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
    
                        UpdateNeighborsRequest updateRequest = new UpdateNeighborsRequest(connectedNodes);
                        out.writeObject(updateRequest);
                        out.flush();
                        System.out.println("UpdateNeighborsRequest enviado para: " + neighbor);
                    } catch (IOException e) {
                        System.err.println("Erro ao enviar UpdateNeighborsRequest para " + neighbor + ": " + e.getMessage());
                    }
                }
            }
        } else {
            System.out.println("Vizinho já existe: " + ipPort);
        }
    }    

    public List<String> getConnectedNodes() {
        return new ArrayList<>(connectedNodes);
    }

    private List<String> searchLocalFiles(String searchTerm) {
        List<String> filesFound = new ArrayList<>();
        File directory = new File(fileDirectory); // Agora usa o diretório fornecido ao nó

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Diretório não encontrado ou inválido: " + directory.getAbsolutePath());
            return filesFound;
        }

        File[] allFiles = directory.listFiles();
        if (allFiles != null) {
            System.out.println("Ficheiros no diretório fornecido:");
            for (File file : allFiles) {
                System.out.println(" - " + file.getName());
            }
        } else {
            System.out.println("Nenhum ficheiro encontrado no diretório fornecido.");
        }

        for (File file : allFiles) {
            if (file.isFile() && file.getName().contains(searchTerm)) {
                filesFound.add(file.getName());
            }
        }

        System.out.println("Ficheiros encontrados com o termo '" + searchTerm + "': " + filesFound);
        return filesFound;
    }

    // Funções de download
    private void handleFileRequest(FileRequest request, ObjectOutputStream out) throws IOException {
        String fileName = request.getFileName();
        int dataIndex = request.getDataIndex();

        File file = new File(fileName);
        if (file.exists()) {
            byte[] dataData = readData(file, dataIndex);
            FileResponse response = new FileResponse(dataData, dataIndex);
            out.writeObject(response);
            out.flush();
        }
    }

    private byte[] readData(File file, int dataIndex) throws IOException {
        int dataSize = 10240;  // TODO: testar valores (10240)
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
