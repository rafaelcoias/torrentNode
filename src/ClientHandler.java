import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    // Atributos
    private Socket clientSocket;
    private String fileDirectory;
    private IscTorrentNode node;

    public ClientHandler(Socket clientSocket, String fileDirectory, IscTorrentNode node) {
        this.clientSocket = clientSocket;
        this.fileDirectory = fileDirectory;
        this.node = node;
    }

    // Funções públicas

    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            // Loop para lidar com mensagens do cliente, fica aberto até a conexão ser fechada
            while (true) {
                Object message = in.readObject();

                // Se a mensagem for uma string e começar com "CONNECT", é um pedido de conexão
                if (message instanceof String && ((String) message).startsWith("CONNECT")) {
                    String neighbor = ((String) message).split(" ")[1];
                    node.addNeighbor(neighbor);
                    System.out.println("Novo vizinho adicionado: " + neighbor);
                    // Avisar o vizinho que a conexão foi bem sucedida
                    out.writeObject("OK CONNECTED");
                    out.flush();
                    continue;
                }

                if (message instanceof SearchRequest) {
                    handleSearchRequest((SearchRequest) message, out);
                } else if (message instanceof FileRequest) {
                    handleFileRequest((FileRequest) message, out);
                } else {
                    System.err.println("Mensagem inválida recebida: " + message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // System.err.println("Erro de comunicação com o cliente.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Funções privadas

    // Funções de pesquisa

    private void handleSearchRequest(SearchRequest request, ObjectOutputStream out) throws IOException {
        String searchTerm = request.getSearchTerm();
        // Procurar ficheiros locais
        List<String> foundFiles = searchLocalFiles(searchTerm);
        System.out.println("Ficheiros locais encontrados: " + foundFiles);

        // Propagar o pedido de pesquisa para os vizinhos
        List<String> neighborFiles = searchInNeighbors(request, request.getSenderNode());
        System.out.println("Ficheiros encontrados nos vizinhos: " + neighborFiles);

        // Juntar os resultados locais e os dos vizinhos
        foundFiles.addAll(neighborFiles);

        // Enviar a resposta de volta
        SearchResponse response = new SearchResponse(foundFiles);
        out.writeObject(response);
        out.flush();
        System.out.println("Resposta de pesquisa enviada: " + foundFiles);

    }

    private List<String> searchLocalFiles(String searchTerm) {
        List<String> filesFound = new ArrayList<>();
        File directory = new File(fileDirectory);

        // Verificar se o diretório existe
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Diretório não encontrado ou inválido: " + directory.getAbsolutePath());
            return filesFound;
        }

        File[] allFiles = directory.listFiles();
        if (allFiles != null) {
            System.out.println("Ficheiros no diretório fornecido:");
            for (File file : allFiles)
                System.out.println(" - " + file.getName());
        } else {
            System.out.println("Nenhum ficheiro encontrado no diretório fornecido.");
        }

        // Procurar ficheiros que contenham o termo de pesquisa
        for (File file : allFiles) {
            if (file.isFile() && file.getName().contains(searchTerm)) {
                filesFound.add(file.getName());
            }
        }

        System.out.println("Ficheiros encontrados com o termo '" + searchTerm + "': " + filesFound);
        return filesFound;
    }

    private List<String> searchInNeighbors(SearchRequest request, String senderNode) {
        System.out.println("Iniciar a pesquisa nos vizinhos. Lista de vizinhos atual: " + node.getConnectedNodes());
        List<String> allFiles = new ArrayList<>();

        for (String neighbor : node.getConnectedNodes()) {
            if (neighbor.equals(senderNode))
                continue;
            try {
                String[] parts = neighbor.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                try (Socket socket = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    // Forward the request with updated senderNode
                    SearchRequest forwardedRequest = new SearchRequest(request.getSearchTerm(), node.getIpAddress() + ":" + node.getPort());
                    out.writeObject(forwardedRequest);
                    out.flush();
                    System.out.println("Pedido de pesquisa enviado para vizinho: " + neighbor);

                    Object response = in.readObject();
                    if (response instanceof SearchResponse) {
                        SearchResponse searchResponse = (SearchResponse) response;
                        allFiles.addAll(searchResponse.getFileNames());
                        System.out.println(
                                "Resposta recebida do vizinho " + neighbor + ": " + searchResponse.getFileNames());
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao comunicar com o vizinho " + neighbor + ": " + e.getMessage());
            }
        }
        return allFiles;
    }

    // Funções de download

    private void handleFileRequest(FileRequest request, ObjectOutputStream out) throws IOException {
        String fileName = request.getFileName();
        int dataIndex = request.getDataIndex();
        File file = new File(fileDirectory, fileName);

        // Se o ficheiro existir localmente, processa normalmente
        if (file.exists() && file.isFile()) {
            byte[] fileData = readData(file, dataIndex);

            if (fileData.length == 0) {
                System.out.println("Fim do ficheiro detectado no servidor para " + fileName);
            }

            FileResponse response = new FileResponse(fileData, dataIndex);
            out.writeObject(response);
            out.flush();
        } else {
            System.out.println("Ficheiro " + fileName + " não encontrado localmente. Encaminhando para os vizinhos...");
            forwardRequestToNeighbors(request, out);
        }
    }

    private void forwardRequestToNeighbors(FileRequest request, ObjectOutputStream clientOut) {
        for (String neighbor : node.getConnectedNodes()) {
            try {
                String[] parts = neighbor.split(":");
                String neighborIp = parts[0];
                int neighborPort = Integer.parseInt(parts[1]);

                try (Socket socket = new Socket(neighborIp, neighborPort);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    // Enviar o pedido de ficheiro para o vizinho
                    out.writeObject(request);
                    out.flush();

                    // Receber a resposta do vizinho
                    Object response = in.readObject();
                    if (response instanceof FileResponse) {
                        clientOut.writeObject(response);
                        clientOut.flush();
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao encaminhar pedido para vizinho " + neighbor + ": " + e.getMessage());
            }
        }

        // Se nenhum vizinho tiver o ficheiro, informa o cliente
        try {
            clientOut.writeObject(new FileResponse(new byte[0], request.getDataIndex()));
            clientOut.flush();
        } catch (IOException e) {
            System.err.println("Erro ao informar cliente sobre falta do ficheiro: " + e.getMessage());
        }
    }

    private byte[] readData(File file, int dataIndex) throws IOException {
        // Tamanho padrão de bloco a rever com o prof.
        int dataSize = 10240; 
        byte[] buffer = new byte[dataSize];

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.skip((long) dataIndex * dataSize);

            int bytesRead = fileInputStream.read(buffer);

            if (bytesRead == -1) {
                System.out.println("Fim do ficheiro atingido.");
                return new byte[0];
            }

            // Caso o último bloco seja menor que o tamanho padrão - sem isto dava erro
            if (bytesRead < dataSize) {
                byte[] lastData = new byte[bytesRead];
                System.arraycopy(buffer, 0, lastData, 0, bytesRead);
                return lastData;
            }
        }
        return buffer;
    }

}
