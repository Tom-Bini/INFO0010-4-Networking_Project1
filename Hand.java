import java.util.List;

public class Hand {

    List<Card> hand;
    List<Boolean> status; // False == hidden, True == revealed

    Hand() {

    }

    Hand(List<Card> cards) {
        this.hand = cards;
        for(int i = 0; i < hand.size(); i++) {
            status.set(i, true);
        }
    }

    Hand(List<Card> cards, List<Boolean> status) {
        this.hand = cards;
        this.status = status;
    }

    public void addCard(Card card, Boolean status) {
        this.hand.add(card);
        this.status.add(status);
    }
}
