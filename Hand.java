import java.util.List;
import java.util.ArrayList;

public class Hand {

    List<Card> hand;
    List<Boolean> status; // False == hidden, True == revealed

    Hand() {
        hand = new ArrayList<Card>();
        status = new ArrayList<Boolean>();
    }

    Hand(List<Card> cards, List<Boolean> status) {
        this.hand = cards;
        this.status = status;
    }

    public void addCard(Card card, Boolean status) {
        this.hand.add(card);
        this.status.add(status);
    }

    public Boolean allCardsVisibles() {
        for (Boolean bool : this.status) {
            if(bool == false) {
                return false;
            }
        }
        return true;
    }

    public int getValueEvenIfHidden() {
        int score = 0;
        int nombre_as = 0;

            for (Card card : this.hand) {
                Character rank = card.getRank();

                if (Character.isDigit(rank)) {
                    int value = Character.getNumericValue(rank);
                    score += value;
                } else if (rank == 'T' || rank == 'J' || rank == 'Q' || rank == 'K') {
                    score += 10;
                } else if (rank == 'A') {
                    nombre_as++;
                    score += 11;
                }
            }

            while (score > 21 && nombre_as > 0) {
                score -= 10;
                nombre_as--;
            }

            return score;
    }

    public int getValue() { // if -1, it's hidden (= "??")
        if (!this.allCardsVisibles()) {
            return -1;
        }
        return getValueEvenIfHidden();
    }

    public int getSize() {
        return this.hand.size();
    }

    public Card reveal(int index) {
        status.set(index, true);
        return hand.get(index);
    }
    
    @Override 
    public String toString() {
        String str = "";
        for(int i = 0; i < hand.size(); i++) {

            if (status.get(i) == true) {
                str += " " + hand.get(i).toString();
            } else {
                str += " ??";
            }
        }

        int value = this.getValue();

        if (value == -1) {
            str += " ??";
        } else {
            str += " " + String.valueOf(value);
        }

        return str;
    }
}
