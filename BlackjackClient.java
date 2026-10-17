import java.io.*;
import java.net.*;
import java.util.List;

public class BlackjackClient {

    private Socket socket;
    private BufferedReader in;
    private OutputStream out;
    private BlackjackProtocol protocol;
    private UserInterface ui;
    private boolean in_round;


    BlackjackClient() throws Exception {
        this.socket = new Socket("127.0.0.1", 2027);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
        this.out = socket.getOutputStream();
        this.in_round = false;

        System.out.println("BlackjackClient up");
    }

    public Response send(Request request) throws IOException, ProtocolException {

        protocol.writeMessage(out, protocol.encodeRequest(request));

        if (request.getAction() == Action.QUIT) {
            return null;
        }

        List<String> lines = protocol.readMessage(in);

        if (lines == null) {
            throw new IOException("Le serveur a fermé la connexion");
        }

        return protocol.decodeResponse(lines);
    }

    private void display(Response response) {
        if (response.getError() != null) {
            ui.showServerError(response.getError());
        } else if (response.getResult() != null) {
            ui.showRoundResult(response.getResult(), response.getAmount());
        } else if (response.getPlayerHand() != null && response.getDealerHand() != null) {
            ui.showCards(response.getPlayerHand(), response.getDealerHand());
        } else if (response.isGameOver() == true) {
            ui.showGameOver(response.getBankroll());
        } else if (response.getBankroll() != null) {
            ui.showBankValue(response.getBankroll());
        }
    }

    public boolean isInRound() {
        return this.in_round;
    }
    
    public static void main(String[] args) throws Exception {
        BlackjackClient bjc = new BlackjackClient();
        UserInterface ui = new TerminalUserInterface();
        try {
            while(true) {
                Action action;
                if (bjc.isInRound() == false) {
                    action = ui.showActionsBetweenRounds();
                } else {
                    action = ui.showActionsDuringRound();
                }
                Request request;

                if (action == Action.BET) {
                    request = new Request(action, ui.askBetSize());
                } else {
                    request = new Request(action);
                }

                Response response = bjc.send(request);

                if (action == Action.QUIT) {
                    ui.showGoodbyeMessage();
                    break;
                }

                bjc.display(response);

                if(response.getPlayerHand() != null && response.getDealerHand() != null && response.getResult() == null) {
                    bjc.in_round = true;
                } else if (response.getPlayerHand() != null && response.getDealerHand() != null && response.getResult() != null) {
                    bjc.in_round = false;
                }

                if (response.getError() == "NO ACTIVE ROUND") {
                    bjc.in_round = false;
                } else if (response.getError() == "ROUND IN PROGRESS") {
                    bjc.in_round = true;
                }

                if (response.isGameOver() == true) {
                    break;
                }

                
            }
        } catch (ProtocolException e) {
            System.out.println("Réponse invalide du serveur");
        } catch (IOException e) {
            System.out.println("Connexion perdue");
        } finally {
            bjc.socket.close();
        }
    }
}
