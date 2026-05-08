package corp.NickAstafyev.xo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// ─────────────────────────────────────────────
// Перечисления
// ─────────────────────────────────────────────
enum GameMode { PVP, PVE }
enum AILevel  { EASY, HARD }

// ─────────────────────────────────────────────
// Модель игрового поля (Singleton)
// ─────────────────────────────────────────────
class Board {

    // Все 8 выигрышных линий: хранятся один раз как константа
    static final int[][][] ALL_LINES = {
            // строки
            {{0,0},{0,1},{0,2}},
            {{1,0},{1,1},{1,2}},
            {{2,0},{2,1},{2,2}},
            // столбцы
            {{0,0},{1,0},{2,0}},
            {{0,1},{1,1},{2,1}},
            {{0,2},{1,2},{2,2}},
            // диагонали
            {{0,0},{1,1},{2,2}},
            {{0,2},{1,1},{2,0}}
    };

    private static final Board INSTANCE = new Board(); // eager singleton — потокобезопасно

    private final char[][] grid = new char[3][3];
    private List<int[]> winningCells = null;
    private int emptyCellCount;

    private Board() { reset(); }

    public static Board getInstance() { return INSTANCE; }

    public void reset() {
        for (char[] row : grid) java.util.Arrays.fill(row, ' ');
        winningCells  = null;
        emptyCellCount = 9;
    }

    /**
     * Делает ход и обновляет состояние победы.
     * @return true если ход сделан, false — клетка занята или координаты неверны.
     */
    public boolean makeMove(int row, int col, char player) {
        if (row < 0 || row > 2 || col < 0 || col > 2) return false;
        if (grid[row][col] != ' ') return false;
        grid[row][col] = player;
        emptyCellCount--;
        updateWinner(row, col, player); // проверяем только линии через (row,col)
        return true;
    }

    public char getCell(int row, int col) { return grid[row][col]; }

    /** Быстрая проверка через счётчик свободных клеток. */
    public boolean isFull() { return emptyCellCount == 0; }

    public char getWinner() {
        if (winningCells == null) return ' ';
        int[] first = winningCells.get(0);
        return grid[first[0]][first[1]];
    }

    public List<int[]> getWinningCells() { return winningCells; }

    /**
     * Вместо полного перебора всех 8 линий проверяем только те,
     * которые проходят через только что поставленную фишку.
     */
    private void updateWinner(int row, int col, char player) {
        for (int[][] line : ALL_LINES) {
            boolean touches = false;
            for (int[] c : line) {
                if (c[0] == row && c[1] == col) { touches = true; break; }
            }
            if (!touches) continue;

            if (grid[line[0][0]][line[0][1]] == player &&
                    grid[line[1][0]][line[1][1]] == player &&
                    grid[line[2][0]][line[2][1]] == player) {

                winningCells = List.of(line[0], line[1], line[2]);
                return;
            }
        }
    }

    /** Копия сетки для AI без создания лишних объектов. */
    public char[][] getGridCopy() {
        char[][] copy = new char[3][3];
        for (int i = 0; i < 3; i++) copy[i] = grid[i].clone();
        return copy;
    }
}

// ─────────────────────────────────────────────
// Искусственный интеллект
// ─────────────────────────────────────────────
class AIPlayer {

    private static final Random RNG = new Random();

    public static int[] getMove(Board board, char aiPlayer, AILevel level) {
        return level == AILevel.EASY
                ? getEasyMove(board, aiPlayer)
                : getHardMove(board, aiPlayer);
    }

    // ── Простой уровень ──────────────────────────────────────────────────────

    private static int[] getEasyMove(Board board, char aiPlayer) {
        char human = opponent(aiPlayer);
        char[][] grid = board.getGridCopy();

        // 1. Победный ход AI
        int[] win = findWinningMove(grid, aiPlayer);
        if (win != null) return win;

        // 2. Блокировка победы человека
        int[] block = findWinningMove(grid, human);
        if (block != null) return block;

        // 3. Вилка AI (одним проходом по пустым клеткам)
        int[] fork = findFork(grid, aiPlayer);
        if (fork != null) return fork;

        // 4. Блокировка вилки противника
        int[] blockFork = findFork(grid, human);
        if (blockFork != null) return blockFork;

        // 5. Центр
        if (grid[1][1] == ' ') return new int[]{1, 1};

        // 6. Углы
        int[][] corners = {{0,0},{0,2},{2,0},{2,2}};
        List<int[]> freeCorners = new ArrayList<>(4);
        for (int[] c : corners) if (grid[c[0]][c[1]] == ' ') freeCorners.add(c);
        if (!freeCorners.isEmpty()) return freeCorners.get(RNG.nextInt(freeCorners.size()));

        // 7. Любая свободная клетка
        List<int[]> empty = getEmptyCells(grid);
        return empty.isEmpty() ? null : empty.get(RNG.nextInt(empty.size()));
    }

    /** Ищет первый ход, дающий победу указанному игроку. */
    private static int[] findWinningMove(char[][] grid, char player) {
        for (int[][] line : Board.ALL_LINES) {
            int count = 0, emptyIdx = -1;
            for (int k = 0; k < 3; k++) {
                char c = grid[line[k][0]][line[k][1]];
                if (c == player)  count++;
                else if (c == ' ') emptyIdx = k;
            }
            if (count == 2 && emptyIdx >= 0) return line[emptyIdx];
        }
        return null;
    }

    /** Ищет ход, создающий одновременно две угрожающие линии (вилку). */
    private static int[] findFork(char[][] grid, char player) {
        char opp = opponent(player);
        List<int[]> empties = getEmptyCells(grid);
        for (int[] cell : empties) {
            grid[cell[0]][cell[1]] = player;
            int threats = 0;
            for (int[][] line : Board.ALL_LINES) {
                int cnt = 0, empty = 0;
                for (int[] coord : line) {
                    char v = grid[coord[0]][coord[1]];
                    if (v == player)  cnt++;
                    else if (v == ' ') empty++;
                }
                // линия без фишек противника, с двумя своими и одной пустой
                if (cnt == 2 && empty == 1) threats++;
            }
            grid[cell[0]][cell[1]] = ' ';
            if (threats >= 2) return cell;
        }
        return null;
    }

    // ── Сложный уровень (Minimax + Alpha-Beta) ───────────────────────────────

    private static int[] getHardMove(Board board, char aiPlayer) {
        char[][] grid = board.getGridCopy();
        char human = opponent(aiPlayer);

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        int alpha = Integer.MIN_VALUE;
        int beta  = Integer.MAX_VALUE;

        for (int[] move : getEmptyCells(grid)) {
            grid[move[0]][move[1]] = aiPlayer;
            int score = minimax(grid, 0, false, aiPlayer, human, alpha, beta);
            grid[move[0]][move[1]] = ' ';
            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
            alpha = Math.max(alpha, score);
        }
        return bestMove;
    }

    private static int minimax(char[][] grid, int depth, boolean maximizing,
                               char ai, char human, int alpha, int beta) {
        // Терминальные состояния
        if (checkWin(grid, ai))    return 100 - depth;
        if (checkWin(grid, human)) return -100 + depth;

        List<int[]> moves = getEmptyCells(grid);
        if (moves.isEmpty()) return 0;
        if (depth >= 6)      return evaluateBoard(grid, ai, human);

        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int[] m : moves) {
                grid[m[0]][m[1]] = ai;
                best  = Math.max(best, minimax(grid, depth + 1, false, ai, human, alpha, beta));
                grid[m[0]][m[1]] = ' ';
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break; // отсечение
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int[] m : moves) {
                grid[m[0]][m[1]] = human;
                best = Math.min(best, minimax(grid, depth + 1, true, ai, human, alpha, beta));
                grid[m[0]][m[1]] = ' ';
                beta = Math.min(beta, best);
                if (beta <= alpha) break; // отсечение
            }
            return best;
        }
    }

    private static int evaluateBoard(char[][] grid, char ai, char human) {
        int score = 0;
        for (int[][] line : Board.ALL_LINES) {
            int aiCnt = 0, humanCnt = 0;
            for (int[] c : line) {
                char v = grid[c[0]][c[1]];
                if (v == ai)    aiCnt++;
                else if (v == human) humanCnt++;
            }
            if (aiCnt > 0 && humanCnt > 0) continue; // заблокирована
            score += switch (aiCnt)    {  case 3 -> 100; case 2 -> 10; case 1 -> 1; default -> 0; };
            score -= switch (humanCnt) {  case 3 -> 100; case 2 -> 10; case 1 -> 1; default -> 0; };
        }
        return score;
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    private static boolean checkWin(char[][] grid, char player) {
        for (int[][] line : Board.ALL_LINES) {
            if (grid[line[0][0]][line[0][1]] == player &&
                    grid[line[1][0]][line[1][1]] == player &&
                    grid[line[2][0]][line[2][1]] == player) return true;
        }
        return false;
    }

    private static List<int[]> getEmptyCells(char[][] grid) {
        List<int[]> list = new ArrayList<>(9);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (grid[i][j] == ' ') list.add(new int[]{i, j});
        return list;
    }

    private static char opponent(char player) { return player == 'X' ? 'O' : 'X'; }
}

// ─────────────────────────────────────────────
// Счётчик очков (выделен из MainForm)
// ─────────────────────────────────────────────
class ScoreTracker {
    private int winsX, winsO, draws;

    public void recordWin(char winner) {
        if (winner == 'X') winsX++;
        else if (winner == 'O') winsO++;
    }
    public void recordDraw()  { draws++; }
    public void reset()       { winsX = winsO = draws = 0; }

    public String format() {
        return "Счёт: X — " + winsX + " | O — " + winsO + " | Ничьи — " + draws;
    }
}

// ─────────────────────────────────────────────
// Главное окно (View + Controller)
// ─────────────────────────────────────────────
public class MainForm extends JFrame {

    private static final Color COLOR_WIN_BLINK = Color.GREEN;
    private static final Color COLOR_WIN_FINAL = new Color(255, 215, 0); // золотой
    private static final Color COLOR_X = Color.BLUE;
    private static final Color COLOR_O = Color.RED;
    private static final int   BLINK_CYCLES = 3;

    private final Board        board        = Board.getInstance();
    private final ScoreTracker score        = new ScoreTracker();
    private final JButton[][]  buttons      = new JButton[3][3];
    private       JLabel       statusLabel;
    private       JLabel       scoreLabel;

    private GameMode gameMode    = GameMode.PVP;
    private AILevel  aiLevel     = AILevel.HARD;
    private char     curPlayer   = 'X';
    private boolean  gameOver    = false;
    private Timer    blinkTimer  = null;

    public MainForm() {
        initUI();
        refreshScore();
        refreshStatus();
    }

    // ── UI ──────────────────────────────────────────────────────────────────

    private void initUI() {
        setTitle("Крестики-нолики");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Верхняя панель
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.LIGHT_GRAY);
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        scoreLabel  = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(scoreLabel,  BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Игровое поле
        JPanel centerPanel = new JPanel(new GridLayout(3, 3, 3, 3));
        centerPanel.setBackground(Color.BLACK);
        Font btnFont = new Font("Arial", Font.BOLD, 48);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton btn = new JButton();
                btn.setFont(btnFont);
                btn.setBackground(Color.WHITE);
                btn.setOpaque(true);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                int r = i, c = j;
                btn.addActionListener(e -> onCellClick(r, c));
                buttons[i][j] = btn;
                centerPanel.add(btn);
            }
        }
        add(centerPanel, BorderLayout.CENTER);

        // Нижняя панель
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.LIGHT_GRAY);
        JButton newGameBtn = new JButton("Новая игра");
        newGameBtn.setFont(new Font("Arial", Font.BOLD, 14));
        newGameBtn.addActionListener(e -> showNewGameDialog());
        JButton exitBtn = new JButton("Выход");
        exitBtn.setFont(new Font("Arial", Font.BOLD, 14));
        exitBtn.addActionListener(e -> System.exit(0));
        bottomPanel.add(newGameBtn);
        bottomPanel.add(exitBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        setupMenu();
    }

    private void setupMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        addMenuItem(fileMenu, "Новая игра", e -> showNewGameDialog());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Выход", e -> System.exit(0));

        JMenu modeMenu = new JMenu("Режим");
        JRadioButtonMenuItem pvpItem = new JRadioButtonMenuItem("Игрок против игрока");
        JRadioButtonMenuItem pveItem = new JRadioButtonMenuItem("Игрок против компьютера");
        ButtonGroup bg = new ButtonGroup();
        bg.add(pvpItem); bg.add(pveItem);
        pvpItem.setSelected(true);
        pvpItem.addActionListener(e -> { gameMode = GameMode.PVP; resetGame(); });
        pveItem.addActionListener(e -> { gameMode = GameMode.PVE; resetGame(); });
        modeMenu.add(pvpItem);
        modeMenu.add(pveItem);

        bar.add(fileMenu);
        bar.add(modeMenu);
        setJMenuBar(bar);
    }

    private static void addMenuItem(JMenu menu, String title, ActionListener al) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(al);
        menu.add(item);
    }

    // ── Диалог новой игры ────────────────────────────────────────────────────

    private void showNewGameDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JRadioButton pvpRadio = new JRadioButton("Два игрока");
        JRadioButton pveRadio = new JRadioButton("Против компьютера");
        ButtonGroup  bg       = new ButtonGroup();
        bg.add(pvpRadio); bg.add(pveRadio);
        (gameMode == GameMode.PVP ? pvpRadio : pveRadio).setSelected(true);

        JLabel    levelLabel = new JLabel("Уровень ИИ:");
        JComboBox<String> levelCombo = new JComboBox<>(new String[]{"Лёгкий", "Сложный"});
        levelCombo.setSelectedIndex(aiLevel == AILevel.EASY ? 0 : 1);

        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        levelPanel.add(levelLabel);
        levelPanel.add(levelCombo);

        // Блокировать выбор уровня в режиме PvP
        ActionListener toggle = e -> {
            boolean pve = pveRadio.isSelected();
            levelLabel.setEnabled(pve);
            levelCombo.setEnabled(pve);
        };
        pvpRadio.addActionListener(toggle);
        pveRadio.addActionListener(toggle);
        toggle.actionPerformed(null); // начальное состояние

        panel.add(pvpRadio);
        panel.add(pveRadio);
        panel.add(levelPanel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая игра",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            gameMode = pveRadio.isSelected() ? GameMode.PVE : GameMode.PVP;
            if (gameMode == GameMode.PVE)
                aiLevel = levelCombo.getSelectedIndex() == 0 ? AILevel.EASY : AILevel.HARD;
            resetGame();
        }
    }

    // ── Игровая логика ───────────────────────────────────────────────────────

    private void resetGame() {
        stopBlink();
        board.reset();
        gameOver  = false;
        curPlayer = 'X';
        for (JButton[] row : buttons)
            for (JButton btn : row) {
                btn.setText("");
                btn.setBackground(Color.WHITE);
                btn.setEnabled(true);
            }
        refreshStatus();
    }

    private void onCellClick(int row, int col) {
        if (gameOver) return;
        if (gameMode == GameMode.PVE && curPlayer == 'O') return;
        if (board.getCell(row, col) != ' ') return;

        applyMove(row, col, curPlayer);
        if (checkEndState()) return;

        curPlayer = AIPlayer.opponent(curPlayer);   // переиспользуем вспомогательный метод
        refreshStatus();

        if (gameMode == GameMode.PVE && curPlayer == 'O') {
            scheduleAiMove();
        }
    }

    private void applyMove(int row, int col, char player) {
        board.makeMove(row, col, player);
        JButton btn = buttons[row][col];
        btn.setText(String.valueOf(player));
        btn.setForeground(player == 'X' ? COLOR_X : COLOR_O);
    }

    private void scheduleAiMove() {
        Timer t = new Timer(400, e -> {
            if (!gameOver) {
                int[] move = AIPlayer.getMove(board, 'O', aiLevel);
                if (move != null) applyMove(move[0], move[1], 'O');
                if (!checkEndState()) {
                    curPlayer = 'X';
                    refreshStatus();
                }
            }
            ((Timer) e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    /**
     * Проверяет конец игры.
     * @return true если игра завершена.
     */
    private boolean checkEndState() {
        char winner = board.getWinner();
        if (winner != ' ') {
            gameOver = true;
            highlightWin();
            score.recordWin(winner);
            refreshScore();
            statusLabel.setText("Победил " + winner + "!");
            return true;
        }
        if (board.isFull()) {
            gameOver = true;
            score.recordDraw();
            refreshScore();
            statusLabel.setText("Ничья!");
            return true;
        }
        return false;
    }

    // ── Подсветка и мигание ──────────────────────────────────────────────────

    private void highlightWin() {
        List<int[]> cells = board.getWinningCells();
        if (cells == null) return;
        for (int[] c : cells) buttons[c[0]][c[1]].setBackground(COLOR_WIN_BLINK);
        startBlink(cells);
    }

    private void startBlink(List<int[]> cells) {
        final int[] tick = {0};
        blinkTimer = new Timer(200, e -> {
            boolean on = (tick[0]++ % 2 == 0);
            Color   c  = on ? COLOR_WIN_FINAL : COLOR_WIN_BLINK;
            for (int[] coord : cells) buttons[coord[0]][coord[1]].setBackground(c);
            if (tick[0] >= BLINK_CYCLES * 2) {
                stopBlink();
                for (int[] coord : cells) buttons[coord[0]][coord[1]].setBackground(COLOR_WIN_FINAL);
            }
        });
        blinkTimer.start();
    }

    private void stopBlink() {
        if (blinkTimer != null && blinkTimer.isRunning()) blinkTimer.stop();
        blinkTimer = null;
    }

    // ── Обновление UI ────────────────────────────────────────────────────────

    private void refreshStatus() {
        if (!gameOver) statusLabel.setText("Ход: " + curPlayer);
    }

    private void refreshScore() {
        scoreLabel.setText(score.format());
    }

    // ── Точка входа ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainForm().setVisible(true));
    }
}