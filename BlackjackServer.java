import java.io.*;
import java.net.*;

public class BlackjackServer {
    
    public static void main(String[] args) throws Exception {
        while (true) {
            try {
                ServerSocket server_socket = new ServerSocket(2027);
                Socket s = server_socket.accept();
                new Thread(new ClientHandler(s)).start();
            } catch (SocketTimeoutException e) {
                // client trop silencieux
            } catch (IOException e) {
                // connexion cassée, client parti, etc.
                continue;
            } finally {
                // fermer le socket quoi qu'il arrive
            }
        }
    }
}
