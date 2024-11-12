public class App {
    public static void main(String[] args) {
        String ipAddress = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        String fileDirectory = args.length > 2 ? args[2] : System.getProperty("user.dir");


        System.out.println("Inicializar novo nó:");
        System.out.println("=> Endereço IP: " + ipAddress);
        System.out.println("=> Porta: " + port);
        System.out.println("=> Pasta de ficheiros: " + fileDirectory);

        new IscTorrentNode(ipAddress, port, fileDirectory);
    }
}
