import java.util.List;

public class Card {
    static List<Character> accepted_ranks = List.of('2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A');
    static List<Character> accepted_suits = List.of('S', 'H', 'D', 'C');
    
    private Character rank;
    private Character suit;

    public Card(Character rank, Character suit) {

        if(!accepted_ranks.contains(rank) || !accepted_suits.contains(suit)) {
            throw new IllegalArgumentException("Format not respected to create the card : " + rank + suit);
        }

        this.rank = rank;
        this.suit = suit;
    }

    public int getRank() { return this.rank; }

    public int getSuit() { return this.suit; }

    public int getValue(int current_score){
        if (Character.isDigit(this.rank)) {
            int value = Character.getNumericValue(this.rank);
            return value;
        } else if (accepted_ranks.subList(8, 11).contains(this.rank)) {
            return 10;
        } else if (current_score <= 11) {
            return 10;
        } else if (current_score > 11) {
            return 1;
        } else {
            throw new IllegalStateException("State not handled : " + this.rank + this.suit + " current score : " + current_score);
        }
    }
}
