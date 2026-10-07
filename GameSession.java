public class GameSession {
    private Deck deck;
    private int bank;
    private Hand player_hand;
    private Hand dealer_hand;

    GameSession() {
        this.deck = new Deck();
        this.deck.shuffle();

        this.bank = 100;

        
    }

    public void startRound(int bet) {
        int player_score = 0;
        int dealer_score = 0;
        if(bet > 0 && bet <= this.bank) {
            //Start of the player's turn
            while(player_score <= 21) {

            }

            //Start of the dealer's turn
            while(dealer_score <= 16) {//vérif

            }
        }
    }

    public Card playerHit() { return this.deck.hit(player_hand); }
    public Card dealerHit() { return this.deck.hit(dealer_hand); }

    public void stand() {

    }

    public int bank() {
        return bank;
    }


}
