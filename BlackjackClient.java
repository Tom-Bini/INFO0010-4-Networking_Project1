import java.io.*;
import java.net.*;
import java.util.List;
import java.util.NoSuchElementException;

public class BlackjackClient {

    private Socket socket;
    private BufferedReader in;
    private OutputStream out;
    private BlackjackProtocol protocol;
    private UserInterface ui;
    private boolean in_round;


    BlackjackClient() throws IOException {
        this.protocol = new BlackjackProtocol();
        this.socket = new Socket("127.0.0.1", 2027);
        this.ui = new TerminalUserInterface();
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
            return;
        }
        if (response.getPlayerHand() != null && response.getDealerHand() != null) {
            ui.showCards(response.getPlayerHand(), response.getDealerHand());
        }
        if (response.getResult() != null) {
            ui.showRoundResult(response.getResult(), response.getAmount());
        }
        if (response.isGameOver() == true) {
            ui.showGameOver(response.getBankroll());
        }
        if (response.getBankroll() != null && response.isGameOver() == false) {
            ui.showBankValue(response.getBankroll());
        }
    }

    public boolean isInRound() {
        return this.in_round;
    }
    
    public static void main(String[] args) throws Exception {
        BlackjackClient bjc = null;
        try {

            bjc = new BlackjackClient();

            while(true) {

                Action action;
                if (bjc.isInRound() == false) {
                    action = bjc.ui.showActionsBetweenRounds();
                } else {
                    action = bjc.ui.showActionsDuringRound();
                }

                Request request;

                if (action == Action.BET) {
                    request = new Request(action, bjc.ui.askBetSize());
                } else {
                    request = new Request(action);
                }

                Response response = bjc.send(request);

                if (action == Action.QUIT) {
                    bjc.ui.showGoodbyeMessage();
                    break;
                }

                bjc.display(response);

                if(response.getPlayerHand() != null && response.getDealerHand() != null && response.getResult() == null) {
                    bjc.in_round = true;
                } else if (response.getPlayerHand() != null && response.getDealerHand() != null && response.getResult() != null) {
                    bjc.in_round = false;
                }

                if ("NO ACTIVE ROUND".equals(response.getError())) {
                    bjc.in_round = false;
                } else if ("ROUND IN PROGRESS".equals(response.getError())) {
                    bjc.in_round = true;
                }

                if (response.isGameOver() == true) {
                    break;
                }

                
            }
        } catch (ConnectException e) {
            System.out.println("Connexion au serveur impossible");
        } catch (ProtocolException e) {
            System.out.println("Réponse invalide du serveur");
        } catch (IOException e) {
            System.out.println("Connexion perdue");
        } catch (NoSuchElementException e) {
            System.out.println("Entrée fermée, arrêt du client.");
        } finally {
            if (bjc != null) {
                bjc.socket.close();
            }
        }
    }
}
