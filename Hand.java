import java.util.List;
import java.util.ArrayList;

public class Hand {

    List<Card> hand;
    List<Boolean> status; // False == hidden, True == revealed

    Hand() {
        hand = new ArrayList<Card>();
        status = new ArrayList<Boolean>();
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

    public int getValue() {

        int hand_value = 0;
        int i = 0;

        for(Card card : hand) {

            if(status.get(i) == true) {
                hand_value += card.getValue(hand_value);
            }

            i++;
        }

        return hand_value;
    }

    public Card reveal(int index) {
        status.set(index, true);
        return hand.get(index);
    }

    public String toString() {
        String str = "";
        for(int i = 0; i < hand.size(); i++) {

            if (status.get(i) == true) {
                str += " " + hand.get(i).toString();
            } else {
                str += " ??";
            }

        }
        return str;
    }
}
