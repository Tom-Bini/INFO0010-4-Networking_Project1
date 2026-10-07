public interface UserInterface {
    void showCards(Hand player_cards, Hand dealer_cards);
    Action showActionsBetweenRounds();
    Action showActionsDuringRound();
    void showGameResultWin(int earnings, int new_bank_value);
    void showGameResultTie(int earnings, int new_bank_value);
    void showGameResultLose(int earnings, int new_bank_value);
    void showGoodbyeMessage();
}
