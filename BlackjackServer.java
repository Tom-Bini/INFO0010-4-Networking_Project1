import java.io.*;
import java.net.*;

public class BlackjackServer {

    private ServerSocket server_socket;
    private Socket transmission_socket;
    private OutputStream out;
    private InputStream in;

    BlackjackServer() throws Exception {
        server_socket = new ServerSocket(2027);
        transmission_socket = server_socket.accept();
        out = transmission_socket.getOutputStream();
        in = transmission_socket.getInputStream();

        System.out.println("BlackjackServer up");
    }
    
    public static void main(String[] args) throws Exception {
        
    }

}
