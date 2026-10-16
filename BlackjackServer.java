import java.io.*;
import java.net.*;

public class BlackjackServer {
    
    public static void main(String[] args) throws Exception {
        ServerSocket server_socket = new ServerSocket(2027);
        while (true) {
            try {
                Socket s = server_socket.accept();
                new Thread(new ClientHandler(s)).start();
            } catch (IOException e) {
                // connexion cassée, client parti, etc.
                continue;
            } finally {
                server_socket.close();
            }
        }
    }
}
