public interface UserInterface {
    void showCards(String player_hand, String dealer_hand);
    Action showActionsBetweenRounds();
    Action showActionsDuringRound();
    int askBetSize();
    void showBankValue(int bank);
    void showRoundResult(Result result, int amount);
    void showServerError(String error);
    void showGameOver(int final_bankroll);
    void showGoodbyeMessage();
}
