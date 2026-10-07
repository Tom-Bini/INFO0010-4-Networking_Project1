import java.util.Scanner;

public class TerminalUserInterface implements UserInterface{
    
    Scanner scanner;

    TerminalUserInterface() {
        this.scanner = new Scanner(System.in);
    }

    @Override 
    public void showCards(Hand player_cards, Hand dealer_cards) {
        String str_player_cards = player_cards.toString();
        String str_dealer_cards = dealer_cards.toString();
        System.out.println("Your cards :" + str_player_cards);
        System.out.println("Dealer's cards :" + str_dealer_cards);
    }

    @Override 
    public Action showActionsBetweenRounds() {
        while(true) {
            System.out.println("1 : BET");
            System.out.println("2 : BANK");
            System.out.println("3 : QUIT");
            System.out.println("===============");
            System.out.println("Choose an action by typing the corresponding number : ");
            int actionChosen = scanner.nextInt();
            
            switch (actionChosen) {
                case 1: return Action.BET;
                case 2: return Action.BANK;
                case 3: return Action.QUIT;
                default: System.out.println("Invalid number, try again.");
                    break;
            }
        }
    }

    @Override 
    public Action showActionsDuringRound() {
        while(true) {
            System.out.println("1 : HIT");
            System.out.println("2 : STAND");
            System.out.println("3 : BANK");
            System.out.println("4 : QUIT");
            System.out.println("===============");
            System.out.println("Choose an action by typing the corresponding number : ");
            int actionChosen = scanner.nextInt();
            
            switch (actionChosen) {
                case 1: return Action.HIT;
                case 2: return Action.STAND;
                case 3: return Action.BANK;
                case 4: return Action.QUIT;
                default: System.out.println("Invalid number, try again.");
                    break;
            }
        }
    }

    @Override
    public void showGameResultWin(int earnings, int new_bank_value) {
        System.out.println("You Won " + earnings + "chips !");
        System.out.println("Your updated bank value : " + new_bank_value);
    }

    @Override
    public void showGameResultTie(int earnings, int new_bank_value) {
        System.out.println("It's a push !");
        System.out.println("Your bank value didn't change : " + new_bank_value);
    }

    @Override
    public void showGameResultLose(int bet, int new_bank_value) {
        System.out.println("You Lost " + bet + "chips !");
        System.out.println("Your updated bank value : " + new_bank_value);
    }

    @Override 
    public void showGoodbyeMessage() {
        System.out.println("Thanks for playing ! Have a nice day.");
        this.scanner.close();
    }
}
