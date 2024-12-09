public class App {
    public static void main(String[] args) {
        // Guardar em variaveis os argumentos fornecidos, ou usar valores padrão
        String ipAddress = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        String fileDirectory = args.length > 2 ? args[2] : System.getProperty("user.dir");

        System.out.println("A inicializar novo nó:");
        System.out.println("=> Endereço IP: " + ipAddress);
        System.out.println("=> Porta: " + port);
        System.out.println("=> Pasta de ficheiros: " + fileDirectory);

        // Criar um novo nó
        new IscTorrentNode(ipAddress, port, fileDirectory);
    }
}
