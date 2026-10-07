import java.util.Scanner;
import java.util.List;

public class TerminalUserInterface implements UserInterface{
    
    Scanner scanner;

    TerminalUserInterface() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public Action getNextAction() {
        
        return Action.BANK;
    }

    public void showCards(List<Card> player_cards, List<Card> dealer_cards) {
        System.out.println("");
    }

    public void closeScanner() {
        this.scanner.close();
    }
}
