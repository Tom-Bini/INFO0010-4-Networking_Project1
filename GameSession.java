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

    public Boolean play() { // Return true when we need to urge quit
        while (bank > 0 && deck.getSize() >= 4) {

            int bet;

            while (true) {
                bet = tui.askBetSize();
                if (bet <= this.bank) {
                    break;
                } else {
                    tui.showErrorMsg();
                }
            }

            bank -= bet;

            Result result = startRound(bet);
            switch (result) {

                case NATURAL_BLACKJACK:
                    bank += bet * 5 / 2;
                    break;

                case WIN:
                    bank += bet * 2;
                    break;

                case TIE:
                    bank += bet;
                    break;

                case LOSE:
                    break;

                case QUIT:
                    return true;

                case ERROR:
                    break;
            
                default:
                    break;
            }
        }
        return;
    }

    public Result startRound(int bet) {
        if(bet > 0 && bet <= this.bank) {

            this.deck.hit(player_hand, true);
            this.deck.hit(player_hand, true);
            this.deck.hit(dealer_hand, true);
            this.deck.hit(dealer_hand, false);

            if(player_hand.getValue() == 21) {
                return Result.NATURAL_BLACKJACK;
            }

            tui.showCards(player_hand, dealer_hand);

            Boolean endPlayerTurn = false;

            // Player's turn

            while (endPlayerTurn == false) {
            
                Action action = tui.showActionsDuringRound();

                switch (action) {
                    case HIT:
                        this.deck.hit(player_hand, true);
                        if(this.deck.isEmpty() == true) {
                            endPlayerTurn = true;
                        }
                        break;

                    case STAND:
                        endPlayerTurn = true;
                        break;

                    case BANK: 
                        tui.showBankValue(bank);
                        break;

                    case QUIT:
                        return Result.QUIT;
                
                    default:break;
                }

                tui.showCards(player_hand, dealer_hand);

                if(player_hand.getValue() > 21) {
                    return Result.LOSE;
                } else if (player_hand.getValue() == 21) {
                    break;
                }
            }

            //Dealer's turn

            dealer_hand.reveal(1);

            while (dealer_hand.getValue() <= 16 && this.deck.isEmpty() == false) {
                this.deck.hit(dealer_hand, true);
            }

            if (player_hand.getValue() > dealer_hand.getValue()) {
                return Result.WIN;
            } else if (player_hand.getValue() == dealer_hand.getValue()) {
                return Result.TIE;
            } else {
                return Result.LOSE;
            }
        } else {
            System.out.println("Bet impossible.");
            return Result.ERROR;
        }
    }
}
