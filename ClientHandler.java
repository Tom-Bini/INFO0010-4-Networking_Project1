import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable{
    private Socket socket;
    private BlackjackProtocol protocol;
    private GameSession session;

    public ClientHandler(Socket socket, BlackjackProtocol protocol, GameSession session) {
        this.socket = socket;
        this.protocol = protocol;
        this.session = session;
    }

    public void run() {
        try {
            socket.setSoTimeout(60000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
            OutputStream out = socket.getOutputStream();

            while (true) {
                List<String> lines = protocol.readMessage(in);

                if (lines == null) {
                    break;
                }
                
                try {

                    Request request = protocol.decodeRequest(lines);
                    Response response;
                    if (request.getAction() == Action.QUIT) {
                        break;
                    }

                    response = handle(request);

                    if (response.isGameOver() == true) {
                        break;
                    }

                } catch (ProtocolException e) {

                    protocol.writeMessage(out, protocol.encodeResponse(Response.error(e.getProtocolMessage())));
                    continue;

                }


            }

        } catch (SocketTimeoutException e) {
            //log and exit
        } catch (RuntimeException e) {
            
        } finally {
            //close socket in try catch
        }
    }

    private Response handle(Request request) {
        Response response;

        switch (request.getAction()) {
            case BET:
                response = session.bet(request.getBetAmount());
            case HIT:
                response = session.hit();
            case STAND:
                response = session.stand();
            case BANK:
                response = session.bank();
            default:
                response = session.bank(); // n'est jamais appelé
                break;
        }
        return response;
    }
    
}
