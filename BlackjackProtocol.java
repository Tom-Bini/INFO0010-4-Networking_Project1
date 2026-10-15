import java.io.BufferedReader;
import java.util.List;

public class BlackjackProtocol {

    String current_msg;

    BlackjackProtocol() {

    }

    public String readMessage(BufferedReader buff) {
        
    }

/* ENCODING REQUEST (CLIENT SIDE) */

    public String encodeRequest(Request request) {
        switch (request.getAction()) {
            case BET:
                return bet(request.getBetAmount());
            case HIT:
                return hit();
            case STAND:
                return stand();
            case BANK:
                return bank();
            case QUIT:
                return quit();
        }
        throw new IllegalStateException("Valeur d'enum non gérée : " + request.getAction());
    }

    public String bet(int amount) {
        String msg = "BET " + amount + "\r\n\r\n";
        return msg;
    }

    public String hit() {
        String msg = "HIT\r\n\r\n";
        return msg;
    }

    public String stand() {
        String msg = "STAND\r\n\r\n";
        return msg;
    }

    public String bank() {
        String msg = "BANK\r\n\r\n";
        return msg;
    }

    public String quit() {
        String msg = "QUIT\r\n\r\n";
        return msg;
    }

    /* DECODING REQUEST (SERVER SIDE) */

    public Request decodeRequest(String request_str) throws ProtocolException {
        switch (request_str) {
            case "HIT":
                return new Request(Action.HIT);
            case "STAND":
                return new Request(Action.STAND);
            case "BANK":
                return new Request(Action.BANK);
            case "QUIT":
                return new Request(Action.QUIT);
    
            default:
                String[] words = request_str.split(" ", -1);
                if (words[0].equals("BET")) {

                    if (words.length != 2 || !words[1].matches("[1-9]\\d*")) {

                        throw new ProtocolException("INVALID BET");

                    } else {

                        int bet;
                        try {
                            bet = Integer.parseInt(words[1]);
                        } catch (NumberFormatException e) {
                            throw new ProtocolException("INSUFFICIENT FUNDS");
                        }

                        return new Request(Action.BET, bet);
                    }

                } else {
                    throw new ProtocolException("WRONG");
                }
        }
    }

/* ENCODING RESPONSE (SERVER SIDE) */

    public String encodeResponse(Response response) {

    }

    public String playerHand(Hand hand) {
        String msg = "PLAYER" + hand.toString() + "\r\n";

        return msg;
    }

    public String dealerHand(Hand hand) {
        String msg = "DEALER" + hand.toString() + "\r\n";
        return msg;
    }

    public String roundStatus(Result result, int bet) {
        String msg;
        switch (result) {

                case BLACKJACK:
                    msg = "BLACKJACK " + bet * 3 / 2;
                    break;

                case WIN:
                    msg = "WIN " + bet;
                    break;

                case PUSH:
                    msg = "PUSH 0";
                    break;

                case LOSE:
                    msg = "LOSE " + bet;
                    break;

                default:
                    return "";
            }
        return msg;
    }

    public String gameOver(int bank) {
        return "GAME OVER\r\nBANKROLL " + String.valueOf(bank) + "\r\n\r\n";
    }

    public String notANumber() {
        return "INVALID BET";
    }

    public String betExceedsBank() {
        return "INSUFFICIENT FUNDS";
    }

    public String roundInProgress() {
        return "ROUND IN PROGRESS";
    }

    public String noActiveRound() {
        return "NO ACTIVE ROUND";
    }

    public String wrong() {
        return "WRONG";
    }

    /* DECODING RESPONSE (CLIENT SIDE) */

    public Response decodeResponse(String response_str) {
        
    }
}
