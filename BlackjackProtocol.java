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
                return null;
            } else if (line.isEmpty()) {
                if (number_lines == 0) {
                    msg_lines.add("");
                }
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
                break;
            case HIT:
                msg.add("HIT");
                break;
            case STAND:
                msg.add("STAND");
                break;
            case BANK:
                msg.add("BANK");
                break;
            case QUIT:
                msg.add("QUIT");
                break;
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
            msg.add("PLAYER " + response.getPlayerHand());
        }

        if (response.getDealerHand() != null) {
            msg.add("DEALER " + response.getDealerHand());
        }

        if (response.getResult() != null) {
            msg.add(statusLine(response.getResult(), response.getAmount()));
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
        return null;
    }

    /* DECODING RESPONSE (CLIENT SIDE) */

    public Response decodeResponse(List<String> lines) throws ProtocolException {
        String player = null;
        String dealer = null;
        Result result = null;
        Integer amount = null;
        boolean game_over = false;
        Integer bank = null;
        String error = null;

        if (lines == null || lines.isEmpty()) {
            throw new ProtocolException("EMPTY MSG");
        }

        for (String line : lines) {
            String[] words = line.split(" ", -1);
            switch (words[0]) {
                case "PLAYER":
                    player = line.substring("PLAYER ".length());
                    continue;
                case "DEALER":
                    dealer = line.substring("DEALER ".length());
                    continue;
                case "BLACKJACK":
                    result = Result.BLACKJACK;
                    amount = Integer.valueOf(line.substring("BLACKJACK ".length()));
                    continue;
                case "WIN":
                    result = Result.WIN;
                    amount = Integer.valueOf(line.substring("WIN ".length()));
                    continue;
                case "PUSH":
                    result = Result.PUSH;
                    amount = Integer.valueOf(line.substring("PUSH ".length()));
                    continue;
                case "LOSE":
                    result = Result.LOSE;
                    amount = Integer.valueOf(line.substring("LOSE ".length()));
                    continue;
                case "GAME":
                    if (words[1].equals("OVER")) {
                        game_over = true;
                    }
                    continue;
                case "BANKROLL":
                    bank = Integer.valueOf(line.substring("BANKROLL ".length()));
                    continue;
            
                default:
                    break;
            }
            switch (line) {
                case "INVALID BET":
                    error = "INVALID BET";
                    continue;
                case "INSUFFICIENT FUNDS":
                    error = "INSUFFICIENT FUNDS";
                    continue;
                case "ROUND IN PROGRESS":
                    error = "ROUND IN PROGRESS";
                    continue;
                case "NO ACTIVE ROUND":
                    error = "NO ACTIVE ROUND";
                    continue;
                case "WRONG":
                    error = "WRONG";
                    continue;
            
                default:
                    throw new ProtocolException("UNKNOWN LINE");
            }
        }

        if (error != null && player == null && dealer == null && result == null && amount == null && game_over == false && bank == null) {

            return Response.error(error);

        } else if (bank != null && player == null && dealer == null && result == null && amount == null && game_over == false && error == null) {

            return Response.bankroll(bank);

        } else if (player != null && dealer != null && result == null && amount == null && !game_over && bank == null && error == null) {

            return Response.hands(player, dealer);

        } else if (player != null && dealer != null && result != null) {

            return Response.endRound(player, dealer, result, amount, bank, game_over);

        } else {

            throw new ProtocolException("LINES COMBINATION INADEQUATE");

        }

    }
}
