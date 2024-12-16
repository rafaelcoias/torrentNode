import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            ExecutorService pool = Executors.newFixedThreadPool(5);
            
            // Thread que aceita conexões de clientes
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        pool.submit(new ClientHandler(clientSocket, fileDirectory, node));
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
