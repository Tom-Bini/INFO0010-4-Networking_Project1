import java.util.Scanner;

public class TerminalUserInterface implements UserInterface{
    
    Scanner scanner;

    TerminalUserInterface() {
        this.scanner = new Scanner(System.in);
    }

    @Override 
    public void showCards(String player_cards, String dealer_cards) {
        System.out.println(player_cards);
        System.out.println(dealer_cards);
    }

    @Override 
    public Action showActionsBetweenRounds() {
        while(true) {
            System.out.println("1 : BET");
            System.out.println("2 : BANK");
            System.out.println("3 : QUIT");
            System.out.println("===============");
            int actionChosen = readIntBetweenRounds(scanner, "Choose an action by typing the corresponding number : ");
            
            switch (actionChosen) {
                case 1: return Action.BET;
                case 2: return Action.BANK;
                case 3: return Action.QUIT;
                default: System.out.println("Invalid number, try again.");
                    break;
            }
        }
    }

    private int readIntBetweenRounds(Scanner scanner, String question) {
        while (true) {
            System.out.print(question);
            String input = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(input);
                if (number != 1 && number != 2 && number != 3) {
                    continue;
                } else {
                    return readIntBetweenRounds(scanner, "Wrong input, retry : ");
                }
            } catch (NumberFormatException e) {
                return readIntBetweenRounds(scanner, "Wrong input, retry : ");
            }
        }
    }

    @Override
    public void showBankValue(int bank) {
        System.out.println("Bank : " + bank + " chips");
    }

    @Override 
    public Action showActionsDuringRound() {
        while(true) {
            System.out.println("1 : HIT");
            System.out.println("2 : STAND");
            System.out.println("3 : BANK");
            System.out.println("4 : QUIT");
            System.out.println("===============");
            int actionChosen = readIntDuringRounds(scanner, "Choose an action by typing the corresponding number : ");
            
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

    private int readIntDuringRounds(Scanner scanner, String question) {
        while (true) {
            System.out.print(question);
            String input = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(input);
                if (number != 1 && number != 2 && number != 3 && number != 4) {
                    continue;
                } else {
                    return readIntDuringRounds(scanner, "Wrong input, retry : ");
                }
            } catch (NumberFormatException e) {
                return readIntDuringRounds(scanner, "Wrong input, retry : ");
            }
        }
    }

    @Override 
    public int askBetSize() {
        while(true) {
            System.out.println("How many chips do you want to bet ?");
            String input = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(input);
                return number;
            } catch (NumberFormatException e) {
                continue;
            }
        }
    }


    @Override 
    public void showServerError(String error) {
        System.out.println(error);
    }

    @Override 
    public void showRoundResult(Result result, int amount) {
        switch (result) {
            case WIN:
                System.out.println("You Won " + amount + "chips !");
                break;
            case LOSE:
                System.out.println("You Lost " + amount + "chips !");
                break;
            case PUSH:
                System.out.println("It's a push !");
                break;
            case BLACKJACK:
                System.out.println("BLACKJACK !!!");
                System.out.println("You Won " + amount + "chips !");
                break;
        
            default:
                break;
        }
    }

    @Override
    public void showGameOver(int final_bankroll) {
        System.out.println("Session terminée avec " + Integer.valueOf(final_bankroll) + " chips !");
    }

    @Override 
    public void showGoodbyeMessage() {
        System.out.println("Thanks for playing ! Have a nice day.");
        this.scanner.close();
    }
}
