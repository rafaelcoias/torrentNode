import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class IscTorrentNode {

    private String ipAddress;
    private int port;
    private String fileDirectory;
    private List<String> connectedNodes;

    public IscTorrentNode(String ipAddress, int port, String fileDirectory) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileDirectory = fileDirectory;
        this.connectedNodes = new ArrayList<>();

        // Inicializa a interface gráfica deste nó
        initializeInterface();

        // Inicializa o servidor deste nó
        NodeServer server = new NodeServer(port, fileDirectory);
        server.startServer();
    }

    private void initializeInterface() {
        // Configuração incial
        JFrame frame = new JFrame("Sistema de Partilha de Ficheiros");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Informações do nó
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("IP: " + ipAddress + " | Porta: " + port + " | Pasta: " + fileDirectory);
        infoPanel.add(infoLabel);
        frame.add(infoPanel, BorderLayout.NORTH);

        // Painel de pesquisa e resultados
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());

        // Pesquisa de ficheiros
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Procurar");
        searchPanel.add(new JLabel("Texto a procurar:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Resultados da pesquisa
        DefaultListModel<String> searchResults = new DefaultListModel<>();
        JList<String> searchResultsList = new JList<>(searchResults);
        JScrollPane scrollPane = new JScrollPane(searchResultsList);

        // Adiciona o painel de pesquisa e a lista de resultados ao painel central
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);

        // Ações
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new FlowLayout());
        JButton connectButton = new JButton("Ligar a Nó");
        JButton downloadButton = new JButton("Descarregar");
        actionPanel.add(downloadButton);
        actionPanel.add(connectButton);
        frame.add(actionPanel, BorderLayout.SOUTH);

        // Ações dos botões
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchTerm = searchField.getText().trim();
                if (searchTerm.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Por favor, insira um termo de pesquisa válido.");
                } else {
                    searchFiles(searchTerm, searchResults);
                }
            }
        });

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectNode();
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedFile = searchResultsList.getSelectedValue();
                if (selectedFile != null)
                    downloadFile(selectedFile);
                else
                    JOptionPane.showMessageDialog(frame, "Por favor, selecione um ficheiro para descarregar.");
            }
        });

        frame.setVisible(true);
    }

    // Pesquisa de ficheiros em outro nó
    private void searchFiles(String searchTerm, DefaultListModel<String> searchResults) {
        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Enviar o pedido de pesquisa
                out.writeObject(new SearchRequest(searchTerm));
                out.flush();
                System.out.println("Pedido de pesquisa enviado: " + searchTerm);

                // Receber a resposta
                Object response = in.readObject();
                if (response instanceof SearchResponse) {
                    SearchResponse searchResponse = (SearchResponse) response;
                    List<String> fileNames = searchResponse.getFileNames();

                    // Atualizar os resultados na interface
                    SwingUtilities.invokeLater(() -> {
                        searchResults.clear();
                        for (String fileName : fileNames) {
                            searchResults.addElement(fileName);
                        }
                    });
                    System.out.println("Resposta de pesquisa recebida: " + fileNames);
                } else {
                    System.err.println("Resposta inesperada do servidor: " + response.getClass().getName());
                }
            } catch (Exception e) {
                System.err.println("Erro na pesquisa: " + e.getMessage());
            }
        }).start();
    }

        public synchronized void addNeighbor(String neighbor) {
        if (!connectedNodes.contains(neighbor)) {
            connectedNodes.add(neighbor);
            System.out.println("Vizinho adicionado: " + neighbor);
            System.out.println("Lista de vizinhos atualizada: " + connectedNodes);
        }
    }

    // Método para obter a lista de vizinhos
    public synchronized List<String> getConnectedNodes() {
        return new ArrayList<>(connectedNodes);
    }

    // Conectar a um nó
    private void connectNode() {
        String noInfo = JOptionPane.showInputDialog("Insira o endereço e a porta do nó (formato: IP:Porta):");
        if (noInfo != null && noInfo.contains(":")) {
            String[] parts = noInfo.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
    
            new Thread(() -> {
                try (Socket socket = new Socket(ip, port);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
    
                    String myAddress = ipAddress + ":" + this.port;
                    out.writeObject("CONNECT " + myAddress);
                    out.flush();
                    System.out.println("Pedido de conexão enviado para " + ip + ":" + port);
    
                    String response = (String) in.readObject();
                    System.out.println("Resposta do nó conectado: " + response);
                    System.out.println("Lista de vizinhos após conectar-se ao nó " + ip + ":" + port + ": " + connectedNodes);
    
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Erro ao conectar ao nó: " + e.getMessage());
                }
            }).start();
        } else {
            JOptionPane.showMessageDialog(null, "Formato inválido. Use: IP:Porta");
        }
    }    

    // Download de um ficheiro
    private void downloadFile(String fileName) {
        new Thread(() -> {
            int dataIndex = 0;
            boolean fileComplete = false;

            try (FileOutputStream downloadedFile = new FileOutputStream(new File(fileDirectory, fileName))) {
                while (!fileComplete) {
                    try (Socket socket = new Socket(ipAddress, port);
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                        // Envia pedido de bloco de ficheiro
                        FileRequest request = new FileRequest(fileName, dataIndex);
                        out.writeObject(request);
                        out.flush();

                        // Recebe o bloco de dados
                        FileResponse response = (FileResponse) in.readObject();
                        byte[] fileData = response.getFileData();
                        dataIndex = response.getDataIndex();

                        downloadedFile.write(fileData);

                        // Verifica se o bloco lido é menor do que o tamanho padrão, indicando o fim do
                        // ficheiro
                        fileComplete = fileData.length < 1024;
                    }
                }
                System.out.println("Download completo para o ficheiro: " + fileName);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro no download: " + e.getMessage());
            }
        }).start();
    }

}
