public class GameSession {
    private Deck deck;
    private int bank;
    private Hand player_hand;
    private Hand dealer_hand;
    private TerminalUserInterface tui;


    GameSession() {
        this.deck = new Deck();
        this.deck.shuffle();

        this.bank = 100;

        this.tui = new TerminalUserInterface();
        
    }

    public void play() {
        while (true) {
            while (true) {
                int bet = tui.askBetSize();
                if (bet <= this.bank) {
                    break;
                } else {
                    tui.showErrorMsg();
                }
            }

            this.deck.hit(player_hand, true);
            this.deck.hit(player_hand, true);
            this.deck.hit(dealer_hand, true);
            this.deck.hit(dealer_hand, false);

            tui.showCards(player_hand, dealer_hand);

            while (player_hand.getValue() >= 21) {
            
                Action action = tui.showActionsDuringRound();

                switch (action) {
                    case HIT:
                        this.deck.hit(player_hand, true);
                        break;

                    case STAND:
                        break;

                    case BANK: 
                        tui.showBankValue(bank);
                        break;

                    case QUIT:
                    
                        break;
                
                    default:
                        break;
                }

                tui.showCards(player_hand, dealer_hand);
            }
        }
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
