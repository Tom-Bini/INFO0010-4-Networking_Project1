import java.io.*;
import java.net.*;

public class BlackjackClient {

    private Socket socket;
    private OutputStream out;
    private InputStream in;

    BlackjackClient() throws Exception {
        this.socket = new Socket("127.0.0.1", 2027);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();

        System.out.println("BlackjackClient up");
    }
    
    public static void main(String[] args) throws Exception {
        
    }
}
