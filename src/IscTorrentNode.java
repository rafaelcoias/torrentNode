import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class IscTorrentNode {

    private String ipAddress;
    private int port;
    private String fileDirectory;

    public IscTorrentNode(String ipAddress, int port, String fileDirectory) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileDirectory = fileDirectory;

        // Inicializa a interface gráfica deste nó
        initializeInterface();

        // Inicializa o servidor deste nó
        NodeServer server = new NodeServer(port);
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

        // Pesquisa de ficheiros
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Procurar");
        searchPanel.add(new JLabel("Texto a procurar:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        frame.add(searchPanel, BorderLayout.CENTER);

        DefaultListModel<String> searchResults = new DefaultListModel<>();
        JList<String> searchResultsList = new JList<>(searchResults);
        frame.add(new JScrollPane(searchResultsList), BorderLayout.CENTER);

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
                String searchTerm = searchField.getText();
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
        try (Socket socket = new Socket(ipAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Envia um pedido de pesquisa
            Message.SearchRequest request = new Message.SearchRequest(searchTerm);
            out.writeObject(request);
            out.flush();

            // Recebe a resposta da pesquisa
            Message.SearchResponse response = (Message.SearchResponse) in.readObject();
            List<String> fileNames = response.getFileNames();

            // Atualiza a lista de resultados
            searchResults.clear();
            for (String fileName : fileNames)
                searchResults.addElement(fileName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro na pesquisa: " + e.getMessage());
        }
    }

    // Conectar a um nó
    private void connectNode() {
        String noInfo = JOptionPane.showInputDialog("Insira o endereço e a porta do nó (formato: IP:Porta):");
        if (noInfo != null && noInfo.contains(":")) {
            String[] parts = noInfo.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            try {
                Socket socket = new Socket(ip, port);
                System.out.println("Conectado ao nó: " + ip + ":" + port);
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao conectar ao nó: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Formato inválido. Use: IP:Porta");
        }
    }

    // Download de um ficheiro
    private void downloadFile(String fileName) {
        int dataIndex = 0;
        boolean fileComplete = false;

        try (FileOutputStream downloadedFile = new FileOutputStream(new File(fileDirectory, fileName))) {
            while (!fileComplete) {
                try (Socket socket = new Socket(ipAddress, port);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    // Envia pedido de bloco de ficheiro
                    Message.FileRequest request = new Message.FileRequest(fileName, dataIndex);
                    out.writeObject(request);
                    out.flush();

                    // Recebe o bloco de dados
                    Message.FileResponse response = (Message.FileResponse) in.readObject();
                    byte[] fileData = response.getFileData();
                    dataIndex = response.getDataIndex();

                    downloadedFile.write(fileData);

                    // Verifica se o bloco lido é menor do que o tamanho padrão, indicando o fim do ficheiro
                    fileComplete = fileData.length < 1024;
                }
            }
            System.out.println("Download completo para o ficheiro: " + fileName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro no download: " + e.getMessage());
        }
    }
}
