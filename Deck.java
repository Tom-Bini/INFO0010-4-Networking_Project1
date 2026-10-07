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
    private Deque<Card> discard_pile = new ArrayDeque<>();

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
        List<Card> temp = new ArrayList<Card>();

        Collections.shuffle(temp);

        deck.clear();

        for (Card element : temp) {
            deck.push(element);
        }
    }

    public Card hit(Hand hand) {
        if (!deck.isEmpty()) {
            Card hit = deck.pop();
            discard_pile.push(hit);
            hand.addCard(hit, null);
            return hit;
        } else {
            System.out.println("Stack is empty! Cannot pop.");
            return null;
        }
    }

    public void discardToDeck() {
        while(!discard_pile.isEmpty()) {
            deck.push(discard_pile.pop());
        }
    }
}
