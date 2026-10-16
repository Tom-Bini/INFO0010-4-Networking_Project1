public class Response {
    private String player_hand;
    private String dealer_hand;

    private Result result;
    private int amount;

    private Integer bankroll;
    private boolean game_over;
    private String error;

    private Response() {}

    public static Response bankroll(int amount) {
        Response response = new Response();
        response.bankroll = amount;
        return response;
    }

    public static Response error(String error) {
        Response response = new Response();
        response.error = error;
        return response;
    }

    public static Response hands(String player_hand, String dealer_hand) {
        Response response = new Response();
        response.player_hand = player_hand;
        response.dealer_hand = dealer_hand;
        return response;
    }

    public static Response endRound(String player_hand, String dealer_hand, Result result, int amount, Integer bankroll, Boolean game_over) {
        Response response = new Response();
        response.player_hand = player_hand;
        response.dealer_hand = dealer_hand;
        response.result = result;
        response.amount = amount;
        response.bankroll = bankroll;
        response.game_over = game_over;
        return response;
    }

    public String getPlayerHand() {
        return player_hand;
    }

    public String getDealerHand() {
        return dealer_hand;
    }

    public Result getResult() {
        return result;
    }

    public int getAmount() {
        return amount;
    }

    public Integer getBankroll() {
        return bankroll;
    }

    public Boolean isGameOver() {
        return game_over;
    }

    public String getError() {
        return error;
    }
}
