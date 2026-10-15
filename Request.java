public class Request {
    private Action action;
    private Integer bet_amount;

    Request(Action action, int bet_amount) {
        this.action = action;
        this.bet_amount = bet_amount;
    }

    Request(Action action) {
        this.action = action;
        this.bet_amount = null;
    }

    public Action getAction() {
        return this.action;
    }

    public int getBetAmount() {
        return this.bet_amount;
    }

}
