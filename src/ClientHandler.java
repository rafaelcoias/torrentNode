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
            out.flush(); // Limpar buffer, sem isto dava bugs
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            // Loop para lidar com mensagens do cliente, fica aberto até a conexão fechar
            while (true) {
                Object message = in.readObject();

                if (message instanceof String && ((String) message).startsWith("CONNECT")) {
                    String neighbor = ((String) message).split(" ")[1];
                    // Ativar ou desativar ligações diretas
                    // node.addNeighbor(neighbor);
                    // System.out.println("Novo vizinho adicionado: " + neighbor);
                    out.writeObject("OK CONNECTED");
                    out.flush();
                    continue;
                }

                if (message instanceof WordSearchMessage) {
                    handleWordSearchMessage((WordSearchMessage) message, out);
                } else if (message instanceof FileBlockRequestMessage) {
                    FileBlockRequestMessage req = (FileBlockRequestMessage) message;
                    handleFileBlockRequest(req, out);
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

    private void handleWordSearchMessage(WordSearchMessage request, ObjectOutputStream out) throws IOException {
        String searchTerm = request.getSearchTerm();
        List<FileSearchResult> localResults = searchLocalFilesForResults(searchTerm);
        List<FileSearchResult> neighborResults = searchInNeighborsForResults(request, request.getSenderNode());
        localResults.addAll(neighborResults);

        // enviar resposta com resultados
        WordSearchResponse response = new WordSearchResponse(localResults);
        out.writeObject(response);
        out.flush();
    }

    private List<FileSearchResult> searchLocalFilesForResults(String searchTerm) {
        List<FileSearchResult> results = new ArrayList<>();
        File directory = new File(fileDirectory);

        if (!directory.exists() || !directory.isDirectory())
            return results;

        File[] allFiles = directory.listFiles();
        if (allFiles == null)
            return results;

        for (File file : allFiles) {
            if (file.isFile() && file.getName().contains(searchTerm)) {
                try {
                    String hash = FileManager.computeFileHash(file);
                    results.add(new FileSearchResult(hash, file.length(), file.getName(),
                            node.getIpAddress() + ":" + node.getPort()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    private List<FileSearchResult> searchInNeighborsForResults(WordSearchMessage request, String senderNode) {
        List<FileSearchResult> allResults = new ArrayList<>();

        for (String neighbor : node.getConnectedNodes()) {
            if (neighbor.equals(senderNode))
                continue;

            String[] parts = neighbor.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(ip, port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Enviar pedido de pesquisa para o vizinho
                WordSearchMessage forwardedRequest = new WordSearchMessage(request.getSearchTerm(),
                        node.getIpAddress() + ":" + node.getPort());
                out.writeObject(forwardedRequest);
                out.flush();

                Object response = in.readObject();
                if (response instanceof WordSearchResponse) {
                    WordSearchResponse wsResponse = (WordSearchResponse) response;
                    allResults.addAll(wsResponse.getResults());
                }
            } catch (Exception e) {
                System.err.println("Erro ao comunicar com o vizinho " + neighbor + ": " + e.getMessage());
            }
        }
        return allResults;
    }

    // Funções de download

    private void handleFileBlockRequest(FileBlockRequestMessage req, ObjectOutputStream out) throws IOException {
        System.out.println("Pedido de bloco recebido");
        File file = node.getFileManager().getFileByHash(req.getFileHash());
        if (file == null) {
            System.out.println("Ficheiro não encontrado localmente.");
            out.writeObject(new FileBlockAnswerMessage(req.getFileHash(), req.getOffset(), new byte[0]));
            out.flush();
            return;
        }

        byte[] data = readFileBlock(file, req.getOffset(), req.getLength());
        System.out.println("A enviar bloco de tamanho " + data.length + " bytes");
        FileBlockAnswerMessage ans = new FileBlockAnswerMessage(req.getFileHash(), req.getOffset(), data);
        out.writeObject(ans);
        out.flush();
    }

    private byte[] readFileBlock(File file, long offset, int length) throws IOException {
        byte[] buffer = new byte[length];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            int bytesRead = raf.read(buffer);
            if (bytesRead < length) {
                byte[] smaller = new byte[bytesRead];
                System.arraycopy(buffer, 0, smaller, 0, bytesRead);
                return smaller;
            }
        }
        return buffer;
    }

    // private byte[] readData(File file, int dataIndex) throws IOException {
    //     // Tamanho padrão de bloco a rever com o prof.
    //     int dataSize = 10240; 
    //     byte[] buffer = new byte[dataSize];

    //     try (FileInputStream fileInputStream = new FileInputStream(file)) {
    //         fileInputStream.skip((long) dataIndex * dataSize);

    //         int bytesRead = fileInputStream.read(buffer);

    //         if (bytesRead == -1) {
    //             System.out.println("Fim do ficheiro atingido.");
    //             return new byte[0];
    //         }

    //         // Caso o último bloco seja menor que o tamanho padrão - sem isto dava erro
    //         if (bytesRead < dataSize) {
    //             byte[] lastData = new byte[bytesRead];
    //             System.arraycopy(buffer, 0, lastData, 0, bytesRead);
    //             return lastData;
    //         }
    //     }
    //     return buffer;
    // }

}
