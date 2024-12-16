import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DownloadTasksManager {
    private String fileHash;
    private String fileName;
    private long fileSize;
    private int blockSize = 10240;
    private int totalBlocks;
    private byte[][] blocksData;
    private int nextBlockIndex = 0;
    private Map<String, Integer> blocksPerNode = new HashMap<>();
    private int blocksDownloaded = 0;

    public DownloadTasksManager(String fileHash, String fileName, long fileSize) {
        this.fileHash = fileHash;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.totalBlocks = (int) ((fileSize + blockSize - 1) / blockSize);
        this.blocksData = new byte[totalBlocks][];
    }

    // Getters

    public String getFileName() {
        return fileName;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public Map<String, Integer> getBlocksPerNode() {
        return blocksPerNode;
    }

    public synchronized int getNextBlockToDownloadIndex() {
        if (nextBlockIndex < totalBlocks) {
            int index = nextBlockIndex++;
            System.out.println("Bloco " + index + " para download");
            return index;
        }
        return -1;
    }

    public FileBlockRequestMessage getNextBlockToDownload(int blockIndex) {
        int offset = blockIndex * blockSize;
        int length = (int) Math.min(fileSize - offset, blockSize);
        return new FileBlockRequestMessage(fileHash, offset, length);
    }

    public void writeToFile(String directoryPath) throws IOException {
        File outFile = new File(directoryPath, fileName);
        try (FileOutputStream file = new FileOutputStream(outFile)) {
            for (byte[] block : blocksData) {
                file.write(block);
            }
        }
    }

    // Funções de controle de download com sincronização

    public synchronized void waitForCompletion() throws InterruptedException {
        while (blocksDownloaded < totalBlocks) 
            wait();
    }

    public synchronized void storeBlockData(int blockIndex, byte[] data, String nodeAddress) {
        blocksData[blockIndex] = data;
        blocksDownloaded++;
        blocksPerNode.put(nodeAddress, blocksPerNode.getOrDefault(nodeAddress, 0) + 1);
        System.out.println("Bloco " + blockIndex + " guardado, tamanho=" + data.length + " bytes do nó " + nodeAddress + "\nTotal de blocos descarregados: " + blocksDownloaded + "/" + totalBlocks);
        if (blocksDownloaded == totalBlocks) {
            System.out.println("Todos os blocos foram descarregados");
            notifyAll();
        }
    }

}
