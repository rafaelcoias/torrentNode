import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeServer {
    // Atributos
    private int port;
    private final String ipAddress = "127.0.0.1";
    private ServerSocket serverSocket;
    private String fileDirectory;
    private IscTorrentNode node;

    public NodeServer(int port, String fileDirectory, IscTorrentNode node) {
        this.port = port;
        this.fileDirectory = fileDirectory;
        this.node = node;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress));
            System.out.println("Servidor iniciado na porta " + port);
            
            // Thread que aceita conexões de clientes
            // Cria uma nova thread para lidar com cada cliente conectado
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Novo nó conectado: " + clientSocket.getLocalSocketAddress());

                        // Passa o socket e a pasta ao ClientHandler, que vai lidar com as mensagens do cliente
                        // ClientHandler lida com pedidos de SEARCH, DOWNLOAD e CONNECT
                        new Thread(new ClientHandler(clientSocket, fileDirectory, node)).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed())
                            System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    public void stopServer() {
        try {
            serverSocket.close();
            System.out.println("Servidor encerrado.");
        } catch (IOException e) {
            System.err.println("Erro ao encerrar o servidor: " + e.getMessage());
        }
    }
}
