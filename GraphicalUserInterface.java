import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * Interface graphique (Swing) pour le client Blackjack. Elle remplace TerminalUserInterface :
 * dans BlackjackClient, remplace  new TerminalUserInterface()  par  new GraphicalUserInterface().
 *
 * ATTENTION : ce fichier sort des consignes du projet (java.awt, javax.swing et java.util.concurrent
 * ne font pas partie des packages autorises). A GARDER EN DEHORS DU ZIP de remise.
 *
 * Test sans serveur :   javac *.java && java GraphicalUserInterface   (petite partie simulee)
 *
 * Fonctionnement : les methodes bloquantes de UserInterface (showActions..., askBetSize) sont appelees
 * par le thread du client. Elles attendent ici qu'un clic arrive (file inbox) ; tout le dessin se fait
 * sur le thread Swing (EDT). Le thread client et l'EDT ne partagent que la file et le montant de la mise.
 *
 * Raccourcis : Entree = DEAL, H = HIT, S = STAND, B = BANK, Q = QUIT, Retour arriere = effacer la mise.
 */
public class GraphicalUserInterface implements UserInterface {

    /* ---------------------------------------------------------------- palette */
    private static final Color FELT_LIGHT = new Color(0x15885A);
    private static final Color FELT_DARK = new Color(0x03241A);
    private static final Color GOLD = new Color(0xE9BE5B);
    private static final Color GOLD_DARK = new Color(0xB8862B);
    private static final Color INK = new Color(0x0A1A14);
    private static final Color RED_SUIT = new Color(0xC8102E);
    private static final Color BLACK_SUIT = new Color(0x15181C);
    private static final Color WIN_COL = new Color(0x3DDC84);
    private static final Color LOSE_COL = new Color(0xFF5C5C);
    private static final Color PUSH_COL = new Color(0xB9C4CF);

    private static final Pattern CARD = Pattern.compile("[2-9TJQKA][SHDC]");
    private static final long DEAL_MS = 380;
    private static final long FLIP_MS = 420;

    private enum Mode { IDLE, BETWEEN, DURING }

    /* ---------------------------------------------------------------- etat partage avec le thread client */
    private final LinkedBlockingQueue<Action> inbox = new LinkedBlockingQueue<>();
    private volatile int pendingBet = 0;

    /* ---------------------------------------------------------------- etat Swing (EDT uniquement) */
    private JFrame frame;
    private Table table;
    private Controls controls;

    private Mode mode = Mode.IDLE;
    private Mode layoutMode = Mode.BETWEEN;

    private final List<CardView> playerCards = new ArrayList<>();
    private final List<CardView> dealerCards = new ArrayList<>();
    private int playerTotal = -1;
    private int dealerTotal = -1;

    private int bank = 100;          // miroir d'affichage : la banque de depart du serveur est de 100, BANK la resynchronise
    private double bankShown = 100;
    private int betAmount = 0;       // mise en cours de construction
    private int lastBet = 0;
    private int tableBet = 0;        // mise engagee dans le round
    private boolean betPending = false;
    private long bankPulseAt = -10_000;

    private String bannerText = null;
    private Color bannerColor = GOLD;
    private long bannerAt = 0;

    private String toastText = null;
    private long toastAt = 0;

    private String veilTitle = null;
    private String veilSub = "";
    private String veilHint = "";
    private long veilAt = 0;

    public GraphicalUserInterface() {
        try {
            SwingUtilities.invokeAndWait(this::build);
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'ouvrir la fenetre", e);
        }
    }

    /* ================================================================ UserInterface */

    @Override
    public void showCards(String player_hand, String dealer_hand) {
        SwingUtilities.invokeLater(() -> {
            betPending = false;
            boolean fresh = playerCards.isEmpty() && dealerCards.isEmpty();
            playerTotal = updateHand(playerCards, player_hand, false, fresh);
            dealerTotal = updateHand(dealerCards, dealer_hand, true, fresh);
            table.repaint();
        });
    }

    @Override
    public Action showActionsBetweenRounds() {
        inbox.clear();
        SwingUtilities.invokeLater(() -> {
            mode = Mode.BETWEEN;
            layoutMode = Mode.BETWEEN;
            betAmount = Math.min(lastBet, bank);
            controls.refresh();
        });
        return waitForAction();
    }

    @Override
    public Action showActionsDuringRound() {
        inbox.clear();
        SwingUtilities.invokeLater(() -> {
            mode = Mode.DURING;
            layoutMode = Mode.DURING;
            controls.refresh();
        });
        return waitForAction();
    }

    @Override
    public int askBetSize() {
        // le montant a deja ete choisi avec les jetons : DEAL a renvoye Action.BET
        return pendingBet;
    }

    @Override
    public void showBankValue(int new_bank) {
        SwingUtilities.invokeLater(() -> {
            bank = new_bank;
            bankPulseAt = System.currentTimeMillis();
            if (mode == Mode.BETWEEN) {
                betAmount = Math.min(betAmount, bank);
                controls.refresh();
            }
            table.repaint();
        });
    }

    @Override
    public void showRoundResult(Result result, int amount) {
        SwingUtilities.invokeLater(() -> {
            betPending = false;
            long now = System.currentTimeMillis();
            switch (result) {
                case BLACKJACK:
                    bank += tableBet + amount;
                    bannerText = "BLACKJACK  +" + amount;
                    bannerColor = GOLD;
                    break;
                case WIN:
                    bank += tableBet + amount;
                    bannerText = "YOU WIN  +" + amount;
                    bannerColor = WIN_COL;
                    break;
                case PUSH:
                    bank += tableBet;
                    bannerText = "PUSH";
                    bannerColor = PUSH_COL;
                    break;
                case LOSE:
                    bannerText = "YOU LOSE  -" + amount;
                    bannerColor = LOSE_COL;
                    break;
                default:
                    break;
            }
            tableBet = 0;
            bannerAt = now + 450;   // laisse le temps aux dernieres cartes d'arriver
            bankPulseAt = now + 450;
            table.repaint();
        });
    }

    @Override
    public void showServerError(String error) {
        SwingUtilities.invokeLater(() -> {
            if (betPending) {          // la mise a ete refusee : on la rend
                bank += tableBet;
                tableBet = 0;
                betPending = false;
            }
            toastText = prettyError(error);
            toastAt = System.currentTimeMillis();
            table.repaint();
        });
    }

    @Override
    public void showGameOver(int final_bankroll) {
        SwingUtilities.invokeLater(() -> {
            mode = Mode.IDLE;
            bank = final_bankroll;
            tableBet = 0;
            veilTitle = "GAME OVER";
            veilSub = "Final bankroll : " + final_bankroll + " chips";
            veilHint = "Close the window to leave the table";
            veilAt = System.currentTimeMillis() + 1600;   // on laisse voir les dernieres cartes
            controls.refresh();
            table.repaint();
        });
    }

    @Override
    public void showGoodbyeMessage() {
        SwingUtilities.invokeLater(() -> {
            mode = Mode.IDLE;
            veilTitle = "SEE YOU SOON";
            veilSub = "Thanks for playing ! Have a nice day.";
            veilHint = "";
            veilAt = System.currentTimeMillis();
            controls.refresh();
            table.repaint();
            Timer close = new Timer(1700, e -> {
                frame.dispose();
                System.exit(0);
            });
            close.setRepeats(false);
            close.start();
        });
    }

    /* ================================================================ thread client <-> clics */

    private Action waitForAction() {
        try {
            return inbox.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Action.QUIT;
        }
    }

    /** Appele sur l'EDT : un clic a ete fait, on bloque les boutons en attendant le serveur. */
    private void submit(Action a) {
        mode = Mode.IDLE;
        controls.refresh();
        inbox.offer(a);
    }

    private void onDeal() {
        if (mode != Mode.BETWEEN || betAmount <= 0) {
            return;
        }
        pendingBet = betAmount;
        lastBet = betAmount;
        tableBet = betAmount;
        bank -= betAmount;                 // meme logique que le serveur : la mise est retiree tout de suite
        betPending = true;
        bankPulseAt = System.currentTimeMillis();
        playerCards.clear();
        dealerCards.clear();
        playerTotal = -1;
        dealerTotal = -1;
        bannerText = null;
        toastText = null;
        submit(Action.BET);
    }

    private void onHit() {
        if (mode == Mode.DURING) {
            submit(Action.HIT);
        }
    }

    private void onStand() {
        if (mode == Mode.DURING) {
            submit(Action.STAND);
        }
    }

    private void onBank() {
        if (mode == Mode.BETWEEN || mode == Mode.DURING) {
            submit(Action.BANK);
        }
    }

    private void onQuit() {
        if (mode == Mode.BETWEEN || mode == Mode.DURING) {
            submit(Action.QUIT);
        }
    }

    private void addBet(int value) {
        if (mode == Mode.BETWEEN) {
            betAmount = Math.min(bank, betAmount + value);
            controls.refresh();
        }
    }

    private void clearBet() {
        if (mode == Mode.BETWEEN) {
            betAmount = 0;
            controls.refresh();
        }
    }

    /* ================================================================ construction de la fenetre */

    private void build() {
        frame = new JFrame("Blackjack");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        table = new Table();
        controls = new Controls();

        JPanel root = new JPanel(new BorderLayout());
        root.add(table, BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(780, 640));
        frame.pack();
        frame.setLocationRelativeTo(null);

        bind("ENTER", this::onDeal);
        bind("H", this::onHit);
        bind("S", this::onStand);
        bind("B", this::onBank);
        bind("Q", this::onQuit);
        bind("BACK_SPACE", this::clearBet);

        controls.refresh();
        new Timer(16, e -> table.tick()).start();
        frame.setVisible(true);
    }

    private void bind(String key, Runnable action) {
        JRootPane rp = frame.getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), "key-" + key);
        rp.getActionMap().put("key-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /* ================================================================ helpers de donnees */

    private static String prettyError(String error) {
        if (error == null) {
            return "";
        }
        switch (error) {
            case "INSUFFICIENT FUNDS": return "Not enough chips for this bet";
            case "ROUND IN PROGRESS": return "A round is already in progress";
            case "NO ACTIVE ROUND": return "No round in progress";
            case "INVALID BET": return "Invalid bet";
            default: return error;
        }
    }

    /** Met a jour une main a partir de la ligne du serveur ("8D 6S 14", "5H ??", "5H 9D QC 24"). Retourne le total ou -1. */
    private int updateHand(List<CardView> list, String line, boolean dealer, boolean fresh) {
        List<String> codes = new ArrayList<>();
        int total = -1;
        if (line != null) {
            for (String token : line.trim().split("\\s+")) {
                if (token.equals("??") || CARD.matcher(token).matches()) {
                    codes.add(token);
                } else if (token.matches("\\d+")) {
                    total = Integer.parseInt(token);
                }
            }
        }

        long now = System.currentTimeMillis();
        if (codes.size() < list.size()) {
            list.clear();
        }

        boolean flipped = false;
        int added = 0;
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            boolean hidden = code.equals("??");
            if (i < list.size()) {
                CardView v = list.get(i);
                if (v.hidden && !hidden) {                       // la carte cachee est revelee : on la retourne
                    v.code = code;
                    v.hidden = false;
                    v.flipAt = now;
                    flipped = true;
                } else if (!v.hidden && (hidden || !v.code.equals(code))) {
                    list.set(i, new CardView(code, hidden, now));
                }
            } else {
                long delay;
                if (fresh) {
                    delay = (i * 2L + (dealer ? 1 : 0)) * 160;   // distribution : joueur, dealer, joueur, dealer
                } else {
                    delay = (flipped ? FLIP_MS + 80 : 0) + added * 300L;
                }
                list.add(new CardView(code, hidden, now + delay));
                added++;
            }
        }
        return total;
    }

    private static class CardView {
        String code;
        boolean hidden;
        long born;
        long flipAt = -1;

        CardView(String code, boolean hidden, long born) {
            this.code = code;
            this.hidden = hidden;
            this.born = born;
        }
    }

    /* ================================================================ dessin : la table */

    private class Table extends JPanel {

        Table() {
            setPreferredSize(new Dimension(1000, 600));
            setOpaque(true);
        }

        void tick() {
            long now = System.currentTimeMillis();
            boolean moved = false;
            if (bankShown != bank) {
                bankShown += (bank - bankShown) * 0.2;
                if (Math.abs(bank - bankShown) < 0.5) {
                    bankShown = bank;
                }
                moved = true;
            }
            if (moved || busy(now)) {
                repaint();
            }
        }

        private boolean busy(long now) {
            for (CardView v : playerCards) {
                if (now < v.born + DEAL_MS + 50 || (v.flipAt >= 0 && now < v.flipAt + FLIP_MS + 50)) return true;
            }
            for (CardView v : dealerCards) {
                if (now < v.born + DEAL_MS + 50 || (v.flipAt >= 0 && now < v.flipAt + FLIP_MS + 50)) return true;
            }
            return (bannerText != null && now < bannerAt + 500)
                    || (toastText != null && now < toastAt + 3400)
                    || now < bankPulseAt + 700
                    || (veilTitle != null && now < veilAt + 700);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();
            long now = System.currentTimeMillis();

            paintFelt(g, w, h);
            paintHud(g, w, h, now);

            int cw = (int) Math.max(64, Math.min(110, Math.min(w * 0.105, h * 0.165)));
            int ch = (int) (cw * 1.4);
            paintHand(g, dealerCards, "DEALER", dealerTotal, h * 0.25, w, cw, ch, now);
            paintHand(g, playerCards, "YOU", playerTotal, h * 0.74, w, cw, ch, now);

            if (tableBet > 0) {
                paintPill(g, 40, h - 84, 150, 52, "BET", String.valueOf(tableBet), new Color(0x2F6FD6), 1.0);
            }
            paintBanner(g, w, h, now);
            paintToast(g, w, h, now);
            paintVeil(g, w, h, now);
            g.dispose();
        }

        private void paintFelt(Graphics2D g, int w, int h) {
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.45f), Math.max(w, h) * 0.75f,
                    new float[]{0f, 1f}, new Color[]{FELT_LIGHT, FELT_DARK}));
            g.fillRect(0, 0, w, h);

            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(233, 190, 91, 70));
            g.draw(new RoundRectangle2D.Double(18, 18, w - 36, h - 36, 60, 60));
            g.setColor(new Color(233, 190, 91, 30));
            g.draw(new RoundRectangle2D.Double(26, 26, w - 52, h - 52, 52, 52));

            g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
            spaced(g, "BLACKJACK PAYS 3 TO 2", w / 2.0, h * 0.5 - 4, new Color(255, 255, 255, 55), 4);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            spaced(g, "DEALER STANDS ON 17", w / 2.0, h * 0.5 + 18, new Color(255, 255, 255, 40), 3);
        }

        private void paintHud(Graphics2D g, int w, int h, long now) {
            // titre
            g.setColor(GOLD);
            drawSuit(g, 'S', 54, 56, 11);
            g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 26));
            g.drawString("Blackjack", 74, 65);

            // banque
            double p = Math.max(0, Math.min(1, (now - bankPulseAt) / 600.0));
            double scale = 1 + 0.10 * Math.sin(Math.PI * p);
            int pw = 210;
            int ph = 56;
            Graphics2D c = (Graphics2D) g.create();
            c.translate(w - 40 - pw / 2.0, 34 + ph / 2.0);
            c.scale(scale, scale);
            c.translate(-pw / 2.0, -ph / 2.0);
            paintPill(c, 0, 0, pw, ph, "BANK", String.valueOf(Math.round(bankShown)), GOLD_DARK, 1.0);
            c.dispose();
        }

        /** Pastille : jeton + petit libelle + grande valeur. */
        private void paintPill(Graphics2D g, double x, double y, double w, double h, String label, String value,
                               Color chip, double alpha) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Double(x, y + 3, w, h, h, h));
            g.setColor(new Color(6, 24, 17, 235));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            g.setColor(new Color(233, 190, 91, 150));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(x + 0.8, y + 0.8, w - 1.6, h - 1.6, h, h));
            drawChip(g, x + h / 2, y + h / 2, h * 0.34, chip, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            spaced(g, label, x + h * 0.95, y + h * 0.36, new Color(255, 255, 255, 150), 2, false);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (h * 0.42)));
            g.setColor(Color.WHITE);
            g.drawString(value, (float) (x + h * 0.95), (float) (y + h * 0.78));
        }

        private void paintHand(Graphics2D g, List<CardView> list, String label, int total, double cy, int w,
                               int cw, int ch, long now) {
            // etiquette
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            spaced(g, label, w / 2.0, cy - ch / 2.0 - 16, new Color(255, 255, 255, 175), 5);
            if (list.isEmpty()) {
                // emplacement vide
                g.setColor(new Color(255, 255, 255, 40));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 7f}, 0f));
                g.draw(new RoundRectangle2D.Double(w / 2.0 - cw / 2.0, cy - ch / 2.0, cw, ch, cw * 0.14, cw * 0.14));
                return;
            }

            int n = list.size();
            double spacing = cw * 0.8;
            double maxRow = w - 260;
            if (cw + (n - 1) * spacing > maxRow && n > 1) {
                spacing = (maxRow - cw) / (n - 1);
            }
            double rowW = cw + (n - 1) * spacing;
            double left = (w - rowW) / 2.0;

            boolean settled = true;
            boolean anyHidden = false;
            for (int i = 0; i < n; i++) {
                CardView v = list.get(i);
                drawCardView(g, v, left + i * spacing, cy - ch / 2.0, cw, ch, now);
                if (now < v.born + DEAL_MS || (v.flipAt >= 0 && now < v.flipAt + FLIP_MS)) {
                    settled = false;
                }
                if (v.hidden) {
                    anyHidden = true;
                }
            }

            if (settled) {
                String txt = null;
                Color bg = new Color(0, 0, 0, 130);
                Color fg = Color.WHITE;
                if (anyHidden) {
                    txt = "?";
                } else if (total > 21) {
                    txt = "BUST";
                    bg = LOSE_COL;
                } else if (total == 21) {
                    txt = "21";
                    bg = GOLD;
                    fg = INK;
                } else if (total >= 0) {
                    txt = String.valueOf(total);
                }
                if (txt != null) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 20));
                    FontMetrics fm = g.getFontMetrics();
                    double bw = Math.max(46, fm.stringWidth(txt) + 28);
                    double bx = left + rowW + 28;
                    double by = cy - 20;
                    g.setColor(bg);
                    g.fill(new RoundRectangle2D.Double(bx, by, bw, 40, 40, 40));
                    g.setColor(new Color(255, 255, 255, 70));
                    g.setStroke(new BasicStroke(1.4f));
                    g.draw(new RoundRectangle2D.Double(bx, by, bw, 40, 40, 40));
                    g.setColor(fg);
                    g.drawString(txt, (float) (bx + (bw - fm.stringWidth(txt)) / 2), (float) (by + 27));
                }
            }
        }

        private void drawCardView(Graphics2D g, CardView v, double slotX, double slotY, int cw, int ch, long now) {
            double t = (now - v.born) / (double) DEAL_MS;
            if (t < 0) {
                return;                      // pas encore distribuee
            }
            t = Math.min(1, t);
            double e = 1 - Math.pow(1 - t, 3);
            double shoeX = getWidth() - cw * 0.6;
            double shoeY = -ch;
            double x = shoeX + (slotX - shoeX) * e;
            double y = shoeY + (slotY - shoeY) * e;
            double rot = (1 - e) * 0.5;
            float alpha = (float) Math.min(1, t * 4);

            boolean face = !v.hidden;
            double sx = 1;
            if (v.flipAt >= 0) {
                double p = (now - v.flipAt) / (double) FLIP_MS;
                if (p < 0) {
                    face = false;
                } else if (p < 1) {
                    if (p < 0.5) {
                        face = false;
                        sx = 1 - 2 * p;
                    } else {
                        face = true;
                        sx = 2 * p - 1;
                    }
                }
            }
            sx = Math.max(0.02, sx);

            Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(alpha));
            c.translate(x + cw / 2.0, y + ch / 2.0);
            c.rotate(rot);
            c.scale(sx, 1);
            paintCard(c, -cw / 2.0, -ch / 2.0, cw, ch, v.code, face);
            c.dispose();
        }

        private void paintBanner(Graphics2D g, int w, int h, long now) {
            if (bannerText == null || (veilTitle != null && now >= veilAt)) {
                return;
            }
            double t = (now - bannerAt) / 380.0;
            if (t < 0) {
                return;
            }
            t = Math.min(1, t);
            double s = 1 + 2.70158 * Math.pow(t - 1, 3) + 1.70158 * Math.pow(t - 1, 2);   // easeOutBack

            Font f = new Font("SansSerif", Font.BOLD, 36);
            FontMetrics fm = g.getFontMetrics(f);
            double bw = fm.stringWidth(bannerText) + 90;
            double bh = 70;

            Graphics2D c = (Graphics2D) g.create();
            c.translate(w / 2.0, h * 0.5);
            c.scale(s, s);
            c.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), (float) (bw * 0.6), new float[]{0f, 1f},
                    new Color[]{new Color(bannerColor.getRed(), bannerColor.getGreen(), bannerColor.getBlue(), 60),
                            new Color(bannerColor.getRed(), bannerColor.getGreen(), bannerColor.getBlue(), 0)}));
            c.fill(new Ellipse2D.Double(-bw * 0.6, -bw * 0.6, bw * 1.2, bw * 1.2));
            c.setColor(new Color(0, 0, 0, 120));
            c.fill(new RoundRectangle2D.Double(-bw / 2, -bh / 2 + 5, bw, bh, bh, bh));
            c.setColor(new Color(5, 20, 14, 240));
            c.fill(new RoundRectangle2D.Double(-bw / 2, -bh / 2, bw, bh, bh, bh));
            c.setColor(bannerColor);
            c.setStroke(new BasicStroke(3f));
            c.draw(new RoundRectangle2D.Double(-bw / 2 + 1.5, -bh / 2 + 1.5, bw - 3, bh - 3, bh, bh));
            c.setFont(f);
            c.drawString(bannerText, (float) (-fm.stringWidth(bannerText) / 2.0), (float) (fm.getAscent() / 2.0 - 4));
            c.dispose();
        }

        private void paintToast(Graphics2D g, int w, int h, long now) {
            if (toastText == null) {
                return;
            }
            long age = now - toastAt;
            if (age > 3200) {
                return;
            }
            float alpha = (float) Math.min(1.0, Math.min(age / 150.0, (3200 - age) / 500.0));
            Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
            c.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = c.getFontMetrics();
            double bw = fm.stringWidth(toastText) + 70;
            double bh = 44;
            double x = (w - bw) / 2.0;
            double y = h - 34 - bh;
            c.setColor(new Color(0, 0, 0, 120));
            c.fill(new RoundRectangle2D.Double(x, y + 3, bw, bh, bh, bh));
            c.setColor(new Color(120, 20, 28, 240));
            c.fill(new RoundRectangle2D.Double(x, y, bw, bh, bh, bh));
            c.setColor(LOSE_COL);
            c.setStroke(new BasicStroke(1.6f));
            c.draw(new RoundRectangle2D.Double(x + 0.8, y + 0.8, bw - 1.6, bh - 1.6, bh, bh));
            c.fill(new Ellipse2D.Double(x + 14, y + bh / 2 - 9, 18, 18));
            c.setColor(new Color(120, 20, 28));
            c.setFont(new Font("SansSerif", Font.BOLD, 14));
            c.drawString("!", (float) (x + 14 + 9 - c.getFontMetrics().stringWidth("!") / 2.0), (float) (y + bh / 2 + 5));
            c.setColor(Color.WHITE);
            c.setFont(new Font("SansSerif", Font.BOLD, 16));
            c.drawString(toastText, (float) (x + 42), (float) (y + bh / 2 + 6));
            c.dispose();
        }

        private void paintVeil(Graphics2D g, int w, int h, long now) {
            if (veilTitle == null || now < veilAt) {
                return;
            }
            double p = Math.min(1, (now - veilAt) / 500.0);
            g.setColor(new Color(0, 0, 0, (int) (225 * p)));
            g.fillRect(0, 0, w, h);
            Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive((float) p));
            c.setColor(GOLD);
            c.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 72));
            FontMetrics fm = c.getFontMetrics();
            c.drawString(veilTitle, (float) ((w - fm.stringWidth(veilTitle)) / 2.0), (float) (h * 0.46));
            c.setColor(Color.WHITE);
            c.setFont(new Font("SansSerif", Font.PLAIN, 22));
            fm = c.getFontMetrics();
            c.drawString(veilSub, (float) ((w - fm.stringWidth(veilSub)) / 2.0), (float) (h * 0.46 + 46));
            if (!veilHint.isEmpty()) {
                c.setColor(new Color(255, 255, 255, 140));
                c.setFont(new Font("SansSerif", Font.PLAIN, 14));
                fm = c.getFontMetrics();
                c.drawString(veilHint, (float) ((w - fm.stringWidth(veilHint)) / 2.0), (float) (h * 0.46 + 82));
            }
            c.dispose();
        }
    }

    /* ================================================================ dessin : cartes, couleurs, jetons */

    private static void paintCard(Graphics2D g, double x, double y, double w, double h, String code, boolean face) {
        double arc = w * 0.14;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 5, w, h, arc, arc));
        g.setColor(new Color(0, 0, 0, 35));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 9, w - 2, h, arc, arc));
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, w, h, arc, arc);

        if (!face) {
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(0x2A4A8C), (float) (x + w), (float) (y + h), new Color(0x0C1B40)));
            g.fill(shape);
            RoundRectangle2D inner = new RoundRectangle2D.Double(x + w * 0.07, y + w * 0.07, w * 0.86, h - w * 0.14, w * 0.09, w * 0.09);
            g.setColor(new Color(0xF3E9CF));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(inner);
            Shape old = g.getClip();
            g.clip(inner);
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(1f));
            double step = w * 0.16;
            for (double k = -h; k < w + h; k += step) {
                g.draw(new Line2D.Double(x + k, y, x + k + h, y + h));
                g.draw(new Line2D.Double(x + k + h, y, x + k, y + h));
            }
            g.setClip(old);
            g.setColor(new Color(0x0C1B40));
            g.fill(new Ellipse2D.Double(x + w / 2 - w * 0.24, y + h / 2 - w * 0.24, w * 0.48, w * 0.48));
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(x + w / 2 - w * 0.24, y + h / 2 - w * 0.24, w * 0.48, w * 0.48));
            drawSuit(g, 'S', x + w / 2, y + h / 2, w * 0.14);
            g.setColor(new Color(255, 255, 255, 60));
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
            return;
        }

        g.setPaint(new GradientPaint(0f, (float) y, Color.WHITE, 0f, (float) (y + h), new Color(0xECE6D6)));
        g.fill(shape);
        g.setColor(new Color(0, 0, 0, 55));
        g.setStroke(new BasicStroke(1f));
        g.draw(shape);

        char r = code.charAt(0);
        char s = code.charAt(1);
        Color col = (s == 'H' || s == 'D') ? RED_SUIT : BLACK_SUIT;
        String rank = (r == 'T') ? "10" : String.valueOf(r);

        paintCorner(g, x, y, w, h, rank, s, col);
        Graphics2D flipped = (Graphics2D) g.create();
        flipped.rotate(Math.PI, x + w / 2, y + h / 2);
        paintCorner(flipped, x, y, w, h, rank, s, col);
        flipped.dispose();

        g.setColor(col);
        if (r == 'J' || r == 'Q' || r == 'K') {
            RoundRectangle2D frame = new RoundRectangle2D.Double(x + w * 0.22, y + h * 0.2, w * 0.56, h * 0.6, w * 0.08, w * 0.08);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 18));
            g.fill(frame);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 120));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(frame);
            g.setColor(col);
            g.setFont(new Font("Serif", Font.BOLD, (int) (h * 0.34)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(rank, (float) (x + (w - fm.stringWidth(rank)) / 2.0), (float) (y + h * 0.5 + fm.getAscent() * 0.33));
            drawSuit(g, s, x + w / 2, y + h * 0.29, w * 0.09);
            drawSuit(g, s, x + w / 2, y + h * 0.71, w * 0.09);
        } else {
            drawSuit(g, s, x + w / 2, y + h / 2, (r == 'A') ? w * 0.30 : w * 0.2);
        }
    }

    private static void paintCorner(Graphics2D g, double x, double y, double w, double h, String rank, char suit, Color col) {
        g.setColor(col);
        g.setFont(new Font("Serif", Font.BOLD, (int) (h * 0.2)));
        FontMetrics fm = g.getFontMetrics();
        double cx = x + w * 0.1 + fm.stringWidth(rank) / 2.0;
        g.drawString(rank, (float) (x + w * 0.1), (float) (y + h * 0.17));
        drawSuit(g, suit, cx, y + h * 0.26, w * 0.075);
    }

    /** Dessine une couleur avec la couleur courante, centree en (cx, cy), "taille" sz. */
    private static void drawSuit(Graphics2D g, char suit, double cx, double cy, double sz) {
        switch (suit) {
            case 'H': {
                Path2D p = new Path2D.Double();
                p.moveTo(cx, cy + sz * 0.95);
                p.curveTo(cx - sz * 1.55, cy + sz * 0.05, cx - sz * 0.95, cy - sz * 0.95, cx, cy - sz * 0.3);
                p.curveTo(cx + sz * 0.95, cy - sz * 0.95, cx + sz * 1.55, cy + sz * 0.05, cx, cy + sz * 0.95);
                g.fill(p);
                break;
            }
            case 'D': {
                Path2D p = new Path2D.Double();
                p.moveTo(cx, cy - sz * 1.1);
                p.lineTo(cx + sz * 0.78, cy);
                p.lineTo(cx, cy + sz * 1.1);
                p.lineTo(cx - sz * 0.78, cy);
                p.closePath();
                g.fill(p);
                break;
            }
            case 'S': {
                double oy = cy - sz * 0.12;
                Path2D p = new Path2D.Double();
                p.moveTo(cx, oy - sz * 0.95);
                p.curveTo(cx - sz * 1.55, oy - sz * 0.05, cx - sz * 0.95, oy + sz * 0.95, cx, oy + sz * 0.3);
                p.curveTo(cx + sz * 0.95, oy + sz * 0.95, cx + sz * 1.55, oy - sz * 0.05, cx, oy - sz * 0.95);
                g.fill(p);
                Path2D stem = new Path2D.Double();
                stem.moveTo(cx - sz * 0.1, oy + sz * 0.3);
                stem.lineTo(cx - sz * 0.38, oy + sz * 1.1);
                stem.lineTo(cx + sz * 0.38, oy + sz * 1.1);
                stem.lineTo(cx + sz * 0.1, oy + sz * 0.3);
                stem.closePath();
                g.fill(stem);
                break;
            }
            default: { // 'C'
                double rr = sz * 0.48;
                g.fill(new Ellipse2D.Double(cx - rr, cy - sz * 0.95, rr * 2, rr * 2));
                g.fill(new Ellipse2D.Double(cx - sz * 0.95, cy - sz * 0.05, rr * 2, rr * 2));
                g.fill(new Ellipse2D.Double(cx + sz * 0.95 - rr * 2, cy - sz * 0.05, rr * 2, rr * 2));
                Path2D stem = new Path2D.Double();
                stem.moveTo(cx - sz * 0.1, cy + sz * 0.2);
                stem.lineTo(cx - sz * 0.38, cy + sz * 1.05);
                stem.lineTo(cx + sz * 0.38, cy + sz * 1.05);
                stem.lineTo(cx + sz * 0.1, cy + sz * 0.2);
                stem.closePath();
                g.fill(stem);
                break;
            }
        }
    }

    private static void drawChip(Graphics2D g, double cx, double cy, double r, Color body, String label) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - r, cy - r + 2.5, r * 2, r * 2));
        g.setColor(body);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        // bandes blanches sur le bord
        for (int i = 0; i < 8; i++) {
            Graphics2D c = (Graphics2D) g.create();
            c.rotate(i * Math.PI / 4, cx, cy);
            c.setColor(new Color(255, 255, 255, 235));
            c.fill(new RoundRectangle2D.Double(cx - r * 0.16, cy - r, r * 0.32, r * 0.24, 3, 3));
            c.dispose();
        }
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.05)));
        g.draw(new Ellipse2D.Double(cx - r * 0.66, cy - r * 0.66, r * 1.32, r * 1.32));
        g.setColor(new Color(0, 0, 0, 55));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        if (label != null) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (r * 0.62)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, (float) (cx - fm.stringWidth(label) / 2.0), (float) (cy + fm.getAscent() * 0.36));
        }
    }

    private static void spaced(Graphics2D g, String text, double cx, double y, Color color, double spacing) {
        spaced(g, text, cx, y, color, spacing, true);
    }

    /** Texte espace lettre par lettre ; centre sur cx, ou aligne a gauche en cx si centered est faux. */
    private static void spaced(Graphics2D g, String text, double cx, double y, Color color, double spacing, boolean centered) {
        FontMetrics fm = g.getFontMetrics();
        double total = -spacing;
        for (char ch : text.toCharArray()) {
            total += fm.charWidth(ch) + spacing;
        }
        double x = centered ? cx - total / 2.0 : cx;
        g.setColor(color);
        for (char ch : text.toCharArray()) {
            g.drawString(String.valueOf(ch), (float) x, (float) y);
            x += fm.charWidth(ch) + spacing;
        }
    }

    /* ================================================================ barre de controle */

    private class Controls extends JPanel {
        final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        final JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        final ChipButton[] chips = {
                new ChipButton(5, new Color(0xC0392B)),
                new ChipButton(10, new Color(0x2F6FD6)),
                new ChipButton(25, new Color(0x1F9D61)),
                new ChipButton(100, new Color(0x23272E))
        };
        final PillButton clear = new PillButton("CLEAR", null, new Color(0x3B4A44), Color.WHITE);
        final JLabel betLabel = new JLabel("BET  0");
        final PillButton deal = new PillButton("DEAL", "Enter", GOLD, INK);
        final PillButton hit = new PillButton("HIT", "H", new Color(0x2FB36D), Color.WHITE);
        final PillButton stand = new PillButton("STAND", "S", new Color(0xD9603B), Color.WHITE);
        final PillButton bankBtn = new PillButton("BANK", "B", new Color(0x3C6EA5), Color.WHITE);
        final PillButton quit = new PillButton("QUIT", "Q", new Color(0x4A4F57), Color.WHITE);

        Controls() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            chipRow.setOpaque(false);
            actionRow.setOpaque(false);

            for (ChipButton c : chips) {
                final int value = c.value;
                c.addActionListener(e -> addBet(value));
                chipRow.add(c);
            }
            clear.addActionListener(e -> clearBet());
            chipRow.add(clear);
            betLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            betLabel.setForeground(GOLD);
            betLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
            chipRow.add(betLabel);

            deal.addActionListener(e -> onDeal());
            hit.addActionListener(e -> onHit());
            stand.addActionListener(e -> onStand());
            bankBtn.addActionListener(e -> onBank());
            quit.addActionListener(e -> onQuit());
            actionRow.add(deal);
            actionRow.add(hit);
            actionRow.add(stand);
            actionRow.add(bankBtn);
            actionRow.add(quit);

            add(Box.createVerticalGlue());
            add(chipRow);
            add(actionRow);
            add(Box.createVerticalGlue());
            setFocusable(false);
        }

        void refresh() {
            boolean between = layoutMode == Mode.BETWEEN;
            boolean during = layoutMode == Mode.DURING;
            chipRow.setVisible(between);
            deal.setVisible(between);
            hit.setVisible(during);
            stand.setVisible(during);

            for (ChipButton c : chips) {
                c.setEnabled(mode == Mode.BETWEEN && bank > 0);
            }
            clear.setEnabled(mode == Mode.BETWEEN && betAmount > 0);
            deal.setEnabled(mode == Mode.BETWEEN && betAmount > 0);
            hit.setEnabled(mode == Mode.DURING);
            stand.setEnabled(mode == Mode.DURING);
            bankBtn.setEnabled(mode != Mode.IDLE);
            quit.setEnabled(mode != Mode.IDLE);
            betLabel.setText("BET  " + betAmount);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 138);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new GradientPaint(0, 0, new Color(0x0D261C), 0, getHeight(), INK));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(233, 190, 91, 110));
            g.fillRect(0, 0, getWidth(), 2);
            g.dispose();
        }
    }

    private static class PillButton extends JButton {
        private final Color base;
        private final Color fg;
        private final String hint;

        PillButton(String text, String hint, Color base, Color fg) {
            super(text);
            this.base = base;
            this.fg = fg;
            this.hint = hint;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("SansSerif", Font.BOLD, 15));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(getText()) + 56;
            if (hint != null) {
                w += getFontMetrics(new Font("SansSerif", Font.BOLD, 10)).stringWidth(hint) + 12;
            }
            return new Dimension(Math.max(112, w), 46);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean on = isEnabled();
            boolean hover = on && getModel().isRollover();
            boolean down = on && getModel().isPressed();
            g.setComposite(AlphaComposite.SrcOver.derive(on ? 1f : 0.36f));

            int w = getWidth();
            int h = getHeight() - 5;
            int dy = down ? 2 : 0;
            g.setColor(new Color(0, 0, 0, 110));
            g.fill(new RoundRectangle2D.Double(1, 4 - dy, w - 2, h, h, h));
            Color top = hover ? base.brighter() : base;
            Color bottom = new Color(Math.max(0, top.getRed() - 40), Math.max(0, top.getGreen() - 40), Math.max(0, top.getBlue() - 40));
            g.setPaint(new GradientPaint(0, dy, top, 0, h + dy, bottom));
            g.fill(new RoundRectangle2D.Double(1, dy, w - 2, h, h, h));
            g.setColor(new Color(255, 255, 255, hover ? 110 : 60));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Double(1.6, dy + 0.6, w - 3.2, h - 1.2, h, h));

            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            Font hf = new Font("SansSerif", Font.BOLD, 10);
            int hw = hint == null ? 0 : g.getFontMetrics(hf).stringWidth(hint) + 12;
            int tx = (w - (fm.stringWidth(getText()) + hw)) / 2;
            g.setColor(fg);
            g.drawString(getText(), tx, dy + (h + fm.getAscent()) / 2 - 2);
            if (hint != null) {
                g.setFont(hf);
                g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 140));
                g.drawString(hint, tx + fm.stringWidth(getText()) + 12, dy + (h + g.getFontMetrics().getAscent()) / 2 - 2);
            }
            g.dispose();
        }
    }

    private static class ChipButton extends JButton {
        final int value;
        private final Color color;

        ChipButton(int value, Color color) {
            super(String.valueOf(value));
            this.value = value;
            this.color = color;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(62, 62);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean on = isEnabled();
            boolean hover = on && getModel().isRollover();
            boolean down = on && getModel().isPressed();
            g.setComposite(AlphaComposite.SrcOver.derive(on ? 1f : 0.35f));
            double lift = hover ? -3 : 0;
            if (down) {
                lift = 1;
            }
            drawChip(g, getWidth() / 2.0, getHeight() / 2.0 - 1 + lift, 26, color, String.valueOf(value));
            g.dispose();
        }
    }

    /* ================================================================ demo sans serveur */

    private static int total(List<String> cards) {
        int t = 0;
        int aces = 0;
        for (String c : cards) {
            char r = c.charAt(0);
            if (r == 'A') {
                aces++;
                t += 11;
            } else if ("TJQK".indexOf(r) >= 0) {
                t += 10;
            } else {
                t += r - '0';
            }
        }
        while (t > 21 && aces-- > 0) {
            t -= 10;
        }
        return t;
    }

    private static String line(List<String> cards, boolean hideSecond) {
        if (hideSecond) {
            return cards.get(0) + " ??";
        }
        return String.join(" ", cards) + " " + total(cards);
    }

    /** Petite partie simulee (regles simplifiees) pour essayer l'interface sans lancer le serveur. */
    public static void main(String[] args) {
        GraphicalUserInterface ui = new GraphicalUserInterface();
        Random rnd = new Random();
        String ranks = "23456789TJQKA";
        String suits = "SHDC";
        int bank = 100;
        int bet = 0;
        boolean inRound = false;
        List<String> player = new ArrayList<>();
        List<String> dealer = new ArrayList<>();

        while (true) {
            Action a = inRound ? ui.showActionsDuringRound() : ui.showActionsBetweenRounds();
            boolean settle = false;
            switch (a) {
                case QUIT:
                    ui.showGoodbyeMessage();
                    return;
                case BANK:
                    ui.showBankValue(bank);
                    break;
                case BET:
                    bet = ui.askBetSize();
                    if (bet > bank) {
                        ui.showServerError("INSUFFICIENT FUNDS");
                        break;
                    }
                    bank -= bet;
                    player.clear();
                    dealer.clear();
                    for (int i = 0; i < 2; i++) {
                        player.add("" + ranks.charAt(rnd.nextInt(13)) + suits.charAt(rnd.nextInt(4)));
                        dealer.add("" + ranks.charAt(rnd.nextInt(13)) + suits.charAt(rnd.nextInt(4)));
                    }
                    inRound = true;
                    if (total(player) == 21) {
                        settle = true;
                    } else {
                        ui.showCards(line(player, false), line(dealer, true));
                    }
                    break;
                case HIT:
                    player.add("" + ranks.charAt(rnd.nextInt(13)) + suits.charAt(rnd.nextInt(4)));
                    if (total(player) >= 21) {
                        settle = true;
                    } else {
                        ui.showCards(line(player, false), line(dealer, true));
                    }
                    break;
                default: // STAND
                    settle = true;
                    break;
            }
            if (settle) {
                while (total(player) <= 21 && total(dealer) < 17) {
                    dealer.add("" + ranks.charAt(rnd.nextInt(13)) + suits.charAt(rnd.nextInt(4)));
                }
                int p = total(player);
                int d = total(dealer);
                ui.showCards(line(player, false), line(dealer, false));
                if (p == 21 && player.size() == 2 && d != 21) {
                    ui.showRoundResult(Result.BLACKJACK, bet * 3 / 2);
                    bank += bet + bet * 3 / 2;
                } else if (p > 21 || (d <= 21 && d > p)) {
                    ui.showRoundResult(Result.LOSE, bet);
                } else if (p == d) {
                    ui.showRoundResult(Result.PUSH, 0);
                    bank += bet;
                } else {
                    ui.showRoundResult(Result.WIN, bet);
                    bank += 2 * bet;
                }
                inRound = false;
                if (bank == 0) {
                    ui.showGameOver(0);
                    return;
                }
            }
        }
    }
}
