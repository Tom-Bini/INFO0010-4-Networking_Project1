import java.io.*;
import java.net.*;
import java.util.List;

public class ClientHandler implements Runnable{
    private Socket socket;
    private BlackjackProtocol protocol;
    private GameSession session;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.protocol = new BlackjackProtocol();
        this.session = new GameSession();
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

                    protocol.writeMessage(out, protocol.encodeResponse(response));

                    if (response.isGameOver() == true) {
                        break;
                    }


                } catch (ProtocolException e) {

                    protocol.writeMessage(out, protocol.encodeResponse(Response.error(e.getProtocolMessage())));
                    continue;

                }

            }

        } catch (SocketTimeoutException e) {
            System.err.println("SocketTimeoutException : " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException : " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("RuntimeException : " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // TODO: handle exception
            }
        }
    }

    private Response handle(Request request) {
        Response response;

        switch (request.getAction()) {
            case BET:
                response = session.bet(request.getBetAmount());
                break;
            case HIT:
                response = session.hit();
                break;
            case STAND:
                response = session.stand();
                break;
            case BANK:
                response = session.bank();
                break;
            default:
                return null;
        }
        return response;
    }
    
}
