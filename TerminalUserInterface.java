import java.util.Scanner;
import java.util.List;

public class TerminalUserInterface implements UserInterface{
    TerminalUserInterface() {
        Scanner scanner = new Scanner(System.in);
    }

    @Override
    public Action getNextAction() {
        
        return Action.BANK;
    }

    public void showCards(List<Card> player_cards, List<Card> dealer_cards) {
        System.out.println("")
    }
}
