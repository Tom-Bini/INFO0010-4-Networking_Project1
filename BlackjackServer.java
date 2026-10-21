import java.io.*;
import java.net.*;

public class BlackjackServer {
    
    public static void main(String[] args) throws Exception {
        while (true) {
            try (ServerSocket server_socket = new ServerSocket(2027)) {
                Socket s = server_socket.accept();
                System.out.println("New client connected");
                new Thread(new ClientHandler(s)).start();
            } catch (IOException e) {
                System.err.println("IOException : " + e.getMessage());
            }
        }
    }
}
