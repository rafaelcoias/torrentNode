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

    // Métodos publicos

    // Inicia o servidor com a porta especificada, fica à espera de conexões
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("Servidor iniciado na porta " + port);
            acceptConnections();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    // Encerra o servidor quando não for necessário ou quando o programa terminar
    public void stopServer() {
        try {
            serverSocket.close();
            System.out.println("Servidor encerrado.");
        } catch (IOException e) {
            System.err.println("Erro ao encerrar o servidor: " + e.getMessage());
        }
    }

    // Métodos privados

    // Thread que aceita conexões de clientes
    // Cria uma nova thread para lidar com cada cliente conectado
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

    // Cria nova thread que vai esperar e aceitar conexões de outros nós
    private void acceptConnections() {
        new Thread(this::connect).start();
    }
}
