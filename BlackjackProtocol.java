public class BlackjackProtocol {

    String current_msg;

    BlackjackProtocol() {

    }

/* CLIENT'S METHODS */

    public Boolean send() {
        return false;
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

/* SERVER'S METHODS */

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

                case NATURAL_BLACKJACK:
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

}
