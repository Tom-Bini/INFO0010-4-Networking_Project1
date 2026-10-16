import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

public class BlackjackProtocol {

    BlackjackProtocol() {

    }

    public List<String> readMessage(BufferedReader in) throws IOException {
        List<String> msg_lines = new ArrayList<String>();
        int number_lines = 0;
        while(true) {
            if (number_lines > 9) {
                throw new IOException("Too much lines in message (>9)");
            }
            String line = in.readLine();
            if (line == null) {
                return msg_lines;
            } else {
                msg_lines.add(line);
            }
            number_lines ++;
        }
    }

    public void writeMessage(OutputStream out, List<String> lines) throws IOException {
        String msg = "";

        for (String line : lines) {
            msg += line + "\r\n";
        }

        msg += "\r\n";
        out.write(msg.getBytes("US-ASCII"));
        out.flush();

        return;
    }

    /* ENCODING REQUEST (CLIENT SIDE) */

    public List<String> encodeRequest(Request request) {
        List<String> msg = new ArrayList<String>();
        switch (request.getAction()) {
            case BET:
                msg.add("BET " + request.getBetAmount());
            case HIT:
                msg.add("HIT");
            case STAND:
                msg.add("STAND");
            case BANK:
                msg.add("BANK");
            case QUIT:
                msg.add("QUIT");
        }
        return msg;
    }

    /* DECODING REQUEST (SERVER SIDE) */

    public Request decodeRequest(List<String> lines) throws ProtocolException {
        if (lines.size() != 1) {
            throw new ProtocolException("WRONG");
        }
        return decodeRequestLine(lines.get(0));
    }

    public Request decodeRequestLine(String line) throws ProtocolException{
        switch (line) {
            case "HIT":
                return new Request(Action.HIT);
            case "STAND":
                return new Request(Action.STAND);
            case "BANK":
                return new Request(Action.BANK);
            case "QUIT":
                return new Request(Action.QUIT);
    
            default:
                String[] words = line.split(" ", -1);
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

    public List<String> encodeResponse(Response response) {

        List<String> msg = new ArrayList<String>();

        if (response.getError() != null) {
            msg.add(response.getError());
            return msg;
        }

        if (response.getPlayerHand() != null) {
            msg.add(response.getPlayerHand());
        }

        if (response.getResult() != null) {
            msg.add(response.getResult().toString());
        }

        if (response.isGameOver() == true) {
            msg.add("GAME OVER");
        }

        if (response.getBankroll() != null) {
            msg.add("BANKROLL " + response.getBankroll());
        }

        return msg;
    }

    private String statusLine(Result result, int amount) {
        String line;
        switch (result) {
            case BLACKJACK:
                return "BLACKJACK " + amount;
            
            case WIN:
                return "WIN " + amount;

            case PUSH:
                return "PUSH " + amount;

            case LOSE:
                return "LOSE " + amount;
        }
    }

    public String roundResult(Result result, int amount) {
        String msg;
        switch (result) {

                case BLACKJACK:
                    msg = "BLACKJACK ";
                    break;

                case WIN:
                    msg = "WIN ";
                    break;

                case PUSH:
                    msg = "PUSH ";
                    break;

                case LOSE:
                    msg = "LOSE ";
                    break;

                default:
                    return "";
            }
        return msg + amount   ;
    }

    /* DECODING RESPONSE (CLIENT SIDE) */

    public Response decodeResponse(String response_str) throws Exception {
        throw new UnsupportedOperationException();
    }
}
