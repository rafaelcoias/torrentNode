import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IscTorrentNode {

    // Atributos
    private String ipAddress;
    private int port;
    private String fileDirectory;
    private List<String> connectedNodes;
    private FileManager fileManager;
    private List<FileSearchResult> lastSearchResults;

    public IscTorrentNode(String ipAddress, int port, String fileDirectory) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileDirectory = fileDirectory;
        this.connectedNodes = new ArrayList<>();
        this.fileManager = new FileManager(fileDirectory);

        initializeInterface();

        NodeServer server = new NodeServer(port, fileDirectory, this);
        server.startServer();
    }

    // Métodos publicos

    public synchronized void addNeighbor(String neighbor) {
        if (!connectedNodes.contains(neighbor)) {
            connectedNodes.add(neighbor);
            System.out.println("Vizinho adicionado: " + neighbor);
            System.out.println("Lista de vizinhos atualizada: " + connectedNodes);
        } else {
            System.out.println("Vizinho já existe: " + neighbor);
        }
    }

    // Getters

    public synchronized List<String> getConnectedNodes() {
        return new ArrayList<>(connectedNodes);
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public String getFileDirectory() {
        return this.fileDirectory;
    }

    public FileManager getFileManager() {
        return this.fileManager;
    }

    // Métodos privados

    private void initializeInterface() {
        // Base da interface
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

        // Painel central
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

        frame.setVisible(true);
    }

    private void connectNode() {
        String nodeAddress = JOptionPane.showInputDialog("Por favor, insira o endereço do nó (IP:PORTA):");
        if (nodeAddress == null || nodeAddress.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Endereço inválido.");
            return;
        }

        String[] parts = nodeAddress.split(":");
        String neighborIp = parts[0];
        int neighborPort = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(neighborIp, neighborPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("CONNECT " + this.ipAddress + ":" + this.port);
            out.flush();

            Object response = in.readObject();
            if (response instanceof String && ((String) response).contains("CONNECTED")) {
                addNeighbor(nodeAddress);
            } else {
                JOptionPane.showMessageDialog(null, "Erro ao ligar ao nó.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao ligar ao nó: " + e.getMessage());
        }

    }

    private void searchFiles(String searchTerm, DefaultListModel<String> searchResults) {
        System.out.println("Pesquisar por: " + searchTerm);
        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(new WordSearchMessage(searchTerm, ipAddress + ":" + port));
                out.flush();

                Object response = in.readObject();
                if (response instanceof WordSearchResponse) {
                    List<FileSearchResult> fileResults = ((WordSearchResponse) response).getResults();
                    this.lastSearchResults = fileResults;

                    SwingUtilities.invokeLater(() -> {
                        searchResults.clear();
                        // Agrupar total de resultados por ficheiro
                        Map<String, Integer> fileCountMap = new HashMap<>();
                        for (FileSearchResult ele : fileResults) {
                            fileCountMap.put(ele.getFileName(), fileCountMap.getOrDefault(ele.getFileName(), 0) + 1);
                        }

                        System.out.println("A Pesquisa retornou " + fileResults.size() + " resultados");

                        // Adicionar resultados à lista na GUI
                        for (Map.Entry<String, Integer> entry : fileCountMap.entrySet()) {
                            String displayName = entry.getKey() + "<" + entry.getValue() + ">";
                            searchResults.addElement(displayName);
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Erro na pesquisa: " + e.getMessage());
            }
        }).start();
    }

    private void downloadFile(String displayedName) {
        if (lastSearchResults == null || lastSearchResults.isEmpty())
            return;

        long startTime = System.currentTimeMillis();
        String fileName = displayedName.substring(0, displayedName.indexOf("<"));

        // Encontrar todos os FileSearchResult para este ficheiro
        List<FileSearchResult> fileResults = new ArrayList<>();
        for (FileSearchResult ele : lastSearchResults) {
            if (ele.getFileName().equals(fileName)) {
                fileResults.add(ele);
            }
        }

        if (fileResults.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhum resultado encontrado para o ficheiro selecionado");
            return;
        }

        String fileHash = fileResults.get(0).getFileHash();
        long fileSize = fileResults.get(0).getFileSize();

        System.out.println("===============================");
        System.out.println("A inicar download do ficheiro:\nnome=" + fileName + "\nhash=" + fileHash + "\ntamanho=" + fileSize + "\nDe " + fileResults.size() + " vizinhos");
        DownloadTasksManager downloader = new DownloadTasksManager(fileHash, fileName, fileSize);

        // Threads de descarregamento (uma por nó com esse ficheiro)
        downloadFileByNeighbors(downloader, fileResults);

        // Esperar pela conclusão do download e mostrar os resultados
        awaitDownloadCompletion(downloader, startTime);
    }

    private void downloadFileByNeighbors(DownloadTasksManager downloader, List<FileSearchResult> fileResults) {
        for (FileSearchResult fileResult : fileResults) {
            new Thread(() -> {
                System.out.println("A iniciar download do ficheiro " + downloader.getFileName() + " a partir do nó " + fileResult.getNodeAddress());

                String[] parts = fileResult.getNodeAddress().split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                try (Socket socket = new Socket(ip, port);
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    while (true) {
                        int blockIndex = downloader.getNextBlockToDownloadIndex();
                        if (blockIndex == -1) {
                            System.out.println("O nó " + fileResult.getNodeAddress() + " não tem mais blocos para enviar.");
                            break;
                        }

                        FileBlockRequestMessage req = downloader.getNextBlockToDownload(blockIndex);
                        System.out.println("Pedir bloco " + blockIndex + ": offset=" + req.getOffset() + ", length=" + req.getLength() + ", nó=" + fileResult.getNodeAddress());
                        out.writeObject(req);
                        out.flush();

                        Object response = in.readObject();
                        if (response instanceof FileBlockAnswerMessage) {
                            FileBlockAnswerMessage ans = (FileBlockAnswerMessage) response;
                            System.out.println("Recebido bloco " + blockIndex + " do nó "
                                    + fileResult.getNodeAddress() + " com tamanho de " + ans.getData().length + " bytes");
                            downloader.storeBlockData(blockIndex, ans.getData(), fileResult.getNodeAddress());
                        }
                    }

                    System.out.println("Download do ficheiro " + downloader.getFileName() + " a partir do nó " + fileResult.getNodeAddress() + " concluído.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void awaitDownloadCompletion(DownloadTasksManager downloader, long startTime) {
        new Thread(() -> {
            try {
                downloader.waitForCompletion();
                downloader.writeToFile(fileDirectory);
                long elapsedTime = System.currentTimeMillis() - startTime;
                Map<String, Integer> blocksMap = downloader.getBlocksPerNode();
                StringBuilder stringBuilder = new StringBuilder();
                System.out.println("Download concluído, tempo: " + elapsedTime + " ms");
                stringBuilder.append("Ficheiro descarregado em ").append(elapsedTime).append("ms\n");
                for (Map.Entry<String, Integer> entry : blocksMap.entrySet()) {
                    System.out.println(
                            "" + entry.getKey() + " deu " + entry.getValue() + " blocos.");
                    stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append(" blocos\n");
                }
                JOptionPane.showMessageDialog(null, stringBuilder.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
