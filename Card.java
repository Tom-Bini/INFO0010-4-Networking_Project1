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

    public Character getRank() { return this.rank; }

    public Character getSuit() { return this.suit; }

    public String toString() {
        return Character.toString(getRank()) + Character.toString(getSuit());
    }
}
