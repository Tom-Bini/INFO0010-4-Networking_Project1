import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;

public class Deck {
    static List<Character> accepted_ranks = List.of('2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A');
    static List<Character> accepted_suits = List.of('S', 'H', 'D', 'C');
    static int deck_size = accepted_ranks.size() * accepted_suits.size();

    private Deque<Card> deck = new ArrayDeque<>();

    public Deck() {

        for (Character rank : accepted_ranks) {

            for(Character suit : accepted_suits) {

                deck.push(new Card(rank, suit));
            
            }
        }
    }

    public int getSize() { return this.deck.size(); }

    public boolean isEmpty() { return this.deck.isEmpty(); }

    public boolean isFull() { return (this.deck.size() == deck_size); }

    public void shuffle() {
        List<Card> temp = new ArrayList<Card>(deck);

        Collections.shuffle(temp);

        deck.clear();

        deck.addAll(temp);
    }

    public Card hit(Hand hand, Boolean status_card) {
        Card hit = deck.pop();
        hand.addCard(hit, status_card);
        return hit;
    }
}
