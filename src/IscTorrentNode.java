import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IscTorrentNode {

    // Atributos
    private String ipAddress;
    private int port;
    private String fileDirectory;
    private List<String> connectedNodes;
    private Set<String> processedRequests = new HashSet<>();

    // Métodos publicos

    public IscTorrentNode(String ipAddress, int port, String fileDirectory) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileDirectory = fileDirectory;
        this.connectedNodes = new ArrayList<>();

        // Inicializa a interface gráfica deste nó
        initializeInterface();

        // Inicializa o servidor deste nó
        NodeServer server = new NodeServer(port, fileDirectory, this);
        server.startServer();
    }

    public synchronized void addNeighbor(String neighbor) {
        if (!connectedNodes.contains(neighbor)) {
            connectedNodes.add(neighbor);
            System.out.println("Vizinho adicionado: " + neighbor);
            System.out.println("Lista de vizinhos atualizada: " + connectedNodes);
        } else {
            System.out.println("Vizinho já existe: " + neighbor);
        }
    }

    // Método para obter a lista de vizinhos
    public synchronized List<String> getConnectedNodes() {
        return new ArrayList<>(connectedNodes);
    }
    
    public String getIpAddress() {
        return this.ipAddress;
    }
    
    public int getPort() {
        return this.port;
    }
    

    // Métodos privados

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
                    return;
                }
                searchFiles(searchTerm, searchResults);
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
                if (selectedFile == null) {
                    JOptionPane.showMessageDialog(frame, "Por favor, selecione um ficheiro para descarregar.");
                    return;
                }
                downloadFile(selectedFile);
            }
        });

        // Mostrar a interface gráfica após a configuração estar completa
        frame.setVisible(true);
    }

    // Nova thread para enviar o pedido de pesquisa e esperar pela resposta dos
    // clientes
    private void searchFiles(String searchTerm, DefaultListModel<String> searchResults) {        
        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Enviar o pedido de pesquisa
                out.writeObject(new SearchRequest(searchTerm, this.ipAddress + ":" + this.port));
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

    // Conectar a um nó
    private void connectNode() {
        String noInfo = JOptionPane.showInputDialog("Insira o endereço e a porta do nó (formato IP:Porta)");
        if (noInfo != null && noInfo.contains(":")) {
            String[] parts = noInfo.split(":");
            String newIp = parts[0];
            int newPort = Integer.parseInt(parts[1]);

            // Iniciar uma nova thread para conectar ao nó especificado
            new Thread(() -> {
                try (Socket socket = new Socket(newIp, newPort);
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    // Enviar pedido de conexão
                    String newAddress = this.ipAddress + ":" + this.port;
                    out.writeObject("CONNECT " + newAddress);
                    out.flush();
                    System.out.println("Pedido de conexão enviado para " + newIp + ":" + newPort);

                    // Receber a resposta do nó conectado
                    String response = (String) in.readObject();
                    System.out.println("Resposta do nó conectado: " + response);

                    // Verificar se a conexão foi bem sucedida
                    if (!response.contains("CONNECTED")) {
                        System.err.println("Erro ao conectar ao nó: " + response);
                        return;
                    }

                    // Adicionar o nó conectado à lista de vizinhos
                    addNeighbor(newIp + ":" + newPort);

                    // Mostrar pop-up de sucesso
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "Conectado com sucesso ao nó " + newIp + ":" + newPort,
                            "Conexão Estabelecida",
                            JOptionPane.INFORMATION_MESSAGE));
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
            boolean fileFound = false;

            try (FileOutputStream downloadedFile = new FileOutputStream(new File(fileDirectory, fileName))) {
                while (!fileComplete) {
                    for (String neighbor : connectedNodes) { // Iterar sobre os vizinhos
                        try (Socket socket = new Socket(neighbor.split(":")[0],
                                Integer.parseInt(neighbor.split(":")[1]));
                                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                            if (!fileFound) {
                                // Enviar um pedido de verificação se o arquivo existe no vizinho
                                out.writeObject(new SearchRequest(fileName, this.ipAddress + ":" + this.port));
                                out.flush();

                                Object response = in.readObject();
                                if (response instanceof SearchResponse) {
                                    SearchResponse searchResponse = (SearchResponse) response;
                                    if (searchResponse.getFileNames().contains(fileName)) {
                                        fileFound = true;
                                        System.out.println("Arquivo encontrado no vizinho: " + neighbor);
                                    }
                                }
                            }

                            // Caso o arquivo esteja no vizinho, enviar o pedido de bloco
                            if (fileFound) {
                                FileRequest request = new FileRequest(fileName, dataIndex);
                                out.writeObject(request);
                                out.flush();

                                Object response = in.readObject();
                                if (response instanceof FileResponse) {
                                    FileResponse fileResponse = (FileResponse) response;
                                    byte[] fileData = fileResponse.getFileData();

                                    if (fileData.length == 0) {
                                        System.out.println("Fim do ficheiro detectado no cliente.");
                                        fileComplete = true;
                                        break;
                                    }

                                    downloadedFile.write(fileData);
                                    System.out.println(
                                            "Bloco recebido para " + fileName + " (índice: " + dataIndex + "): "
                                                    + new String(fileData).trim());
                                    dataIndex++;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Erro ao comunicar com o vizinho " + neighbor + ": " + e.getMessage());
                        }
                    }

                    if (!fileFound) {
                        JOptionPane.showMessageDialog(null, "Arquivo não encontrado em nenhum nó vizinho.");
                        break;
                    }
                }
                if (fileComplete) {
                    JOptionPane.showMessageDialog(null, "Ficheiro descarregado com sucesso: " + fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Erro ao descarregar o ficheiro: " + e.getMessage());
            }
        }).start();
    }

}
