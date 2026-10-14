public class GameSession {
    private Deck deck;
    private int bank;
    private Hand player_hand;
    private Hand dealer_hand;
    private boolean is_in_round;
    private int current_bet;


    GameSession() {
        this.deck = new Deck();
        deck.shuffle();

        this.bank = 100;

        this.player_hand = new Hand();
        this.dealer_hand = new Hand();

        this.is_in_round = false;
    }

    //METHODS THAT VERIFY THAT THE ACTION IS POSSIBLE IN CURRENT STATE, MODIFY THE STATE AND RETURN WITHOUT DISPLAYING ANYTHING

    public Response bet(int size) {

        Response response;
        
        if (size > bank) {
            response = Response.error("INSUFFICIENT FUNDS");
            return response;
        } else if (is_in_round == true) {
            response = Response.error("ROUND IN PROGRESS");
            return response;
        } else {
            this.is_in_round = true;
            current_bet = size;
            bank -= size;
            player_hand = new Hand();
            dealer_hand = new Hand();

            deck.hit(player_hand, true);
            deck.hit(player_hand, true);
            deck.hit(dealer_hand, true);
            deck.hit(dealer_hand, false);

            int player_value = player_hand.getValueEvenIfHidden();
            int dealer_value = dealer_hand.getValueEvenIfHidden();

            if (player_value == 21 && dealer_value == 21) {

                return endRound(Result.PUSH);

            } else if (player_value == 21) {

                return endRound(Result.BLACKJACK);

            } else if (dealer_value == 21) {

                return endRound(Result.LOSE);
                
            } else {
                return Response.hands(player_hand.toString(), dealer_hand.toString());
            }
        }
    }

    public Response hit() {

        Response response;

        if (deck.getSize() == 0) {
            response = Response.error("");
        } else if (is_in_round == false) {

            response = Response.error("NO ACTIVE ROUND");
            return response;

        }

        deck.hit(player_hand, true);

        int player_hand_value = player_hand.getValue();


        if (player_hand_value > 21) {

            return endRound(Result.LOSE);

        } else if (player_hand_value == 21) {

            return stand();

        }

        if (deck.getSize() == 0) {

            int dealer_hand_value = dealer_hand.getValueEvenIfHidden();

            if (player_hand_value > dealer_hand_value) {

                return endRound(Result.WIN);

            } else if (player_hand_value < dealer_hand_value) {

                return endRound(Result.LOSE);

            } else {

                return endRound(Result.PUSH);

            }

        }

        return Response.hands(player_hand.toString(), dealer_hand.toString());

    }

    public Response stand() {
        if (is_in_round == false) {
            return Response.error("NO ACTIVE ROUND");
        }
        dealer_hand.reveal(1);

        while (dealer_hand.getValue() <= 16) {

            deck.hit(dealer_hand, true);
            
            if (dealer_hand.getValue() > 21) {

                return endRound(Result.WIN);

            } else if (deck.getSize() == 0) {

                return resolveBothUnder21(true);

            }
        }
            return resolveBothUnder21(false);

    }

    public Response bank() {
        return Response.bankroll(bank);
    }


    private Response resolveBothUnder21(boolean game_over) {
        int player_hand_value = player_hand.getValueEvenIfHidden();
        int dealer_hand_value = dealer_hand.getValueEvenIfHidden();
        
        if (player_hand_value > dealer_hand_value) {

            return endRound(Result.WIN);

        } else if (player_hand_value < dealer_hand_value) {

            return endRound(Result.LOSE);

        } else {

            return endRound(Result.PUSH);

        }
    }

    private Response endRound(Result result) {

        int amount = 0;
        Response response;
        dealer_hand.reveal(1);

        switch (result) {

            case BLACKJACK:
                amount = current_bet * 3 / 2;
                bank += current_bet * 5 / 2;
                break;

            case WIN:
                amount = current_bet;
                bank += 2 * current_bet;
                break;

            case PUSH:
                amount = 0;
                bank += current_bet;
                break;

            case LOSE:
                amount = current_bet;
                break;

        }

        boolean game_over = bank == 0 || deck.getSize() < 4;
        response = Response.endRound(player_hand.toString(), dealer_hand.toString(), result, amount, bank, game_over);
        is_in_round = false;

        return response;
    }
}
