public class GameSession {
    private Deck deck;
    private int bank;

    GameSession() {
        this.deck = new Deck();
        this.deck.shuffle();

        this.bank = 100;

        
    }

    public void startRound() {
        
    }

    public Card hit() {

        return this.deck.hit();
    }

    public void stand() {

    }

    public int bank() {
        return bank;
    }


}
