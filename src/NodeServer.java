import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NodeServer {
    // Atributos
    private int port;
    private ServerSocket serverSocket;

    public NodeServer(int port) {
        this.port = port;
    }

    // Métodos privados

    private void connect() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo nó conectado: " + clientSocket.getRemoteSocketAddress());
                
                // Nova thread que representa o cliente conectado
                new Thread(new ClientHandler(clientSocket)).start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) 
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void acceptConnections() {
        new Thread(this::connect).start();
    }


    // Métodos publicos

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("Servidor iniciado na porta " + port);
            acceptConnections();
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