public class Request {
    private Action action;
    private int bet_amount;

    Request(Action action, int bet_amount) {
        this.action = action;
        this.bet_amount = bet_amount;
    }

    public Action getAction() {
        return this.action;
    }

    public int getBetAmount() {
        return this.bet_amount;
    }

}
