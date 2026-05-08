package corp.NickAstafyev.xo;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Перечисление режимов игры
enum GameMode {
    PVP, PVE
}

// Уровень сложности ИИ
enum AILevel {
    EASY, HARD
}

// Модель игрового поля (Singleton)
class Board {
    private static Board instance;
    private char[][] grid;
    private List<int[]> winningCells;

    private Board() {
        grid = new char[3][3];
        reset();
    }

    public static Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    public void reset() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                grid[i][j] = ' ';
            }
        }
        winningCells = null;
    }

    public boolean makeMove(int row, int col, char player) {
        if (row < 0 || row > 2 || col < 0 || col > 2) return false;
        if (grid[row][col] != ' ') return false;
        grid[row][col] = player;
        checkWinner(); // обновляем выигрышную линию при каждом ходе
        return true;
    }

    public char getCell(int row, int col) {
        return grid[row][col];
    }

    public boolean isFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[i][j] == ' ') return false;
            }
        }
        return true;
    }

    public char getWinner() {
        if (winningCells == null) return ' ';
        return grid[winningCells.get(0)[0]][winningCells.get(0)[1]];
    }

    public List<int[]> getWinningCells() {
        return winningCells;
    }

    private void checkWinner() {
        // Проверяем все 8 линий
        winningCells = null;
        // Строки
        for (int i = 0; i < 3; i++) {
            if (grid[i][0] != ' ' && grid[i][0] == grid[i][1] && grid[i][1] == grid[i][2]) {
                winningCells = new ArrayList<>();
                winningCells.add(new int[]{i, 0});
                winningCells.add(new int[]{i, 1});
                winningCells.add(new int[]{i, 2});
                return;
            }
        }
        // Столбцы
        for (int j = 0; j < 3; j++) {
            if (grid[0][j] != ' ' && grid[0][j] == grid[1][j] && grid[1][j] == grid[2][j]) {
                winningCells = new ArrayList<>();
                winningCells.add(new int[]{0, j});
                winningCells.add(new int[]{1, j});
                winningCells.add(new int[]{2, j});
                return;
            }
        }
        // Диагонали
        if (grid[0][0] != ' ' && grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2]) {
            winningCells = new ArrayList<>();
            winningCells.add(new int[]{0, 0});
            winningCells.add(new int[]{1, 1});
            winningCells.add(new int[]{2, 2});
            return;
        }
        if (grid[0][2] != ' ' && grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0]) {
            winningCells = new ArrayList<>();
            winningCells.add(new int[]{0, 2});
            winningCells.add(new int[]{1, 1});
            winningCells.add(new int[]{2, 0});
            return;
        }
    }

    public char[][] getGridCopy() {
        char[][] copy = new char[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, 3);
        }
        return copy;
    }
}

// Класс искусственного интеллекта
class AIPlayer {

    public static int[] getMove(Board board, char aiPlayer, AILevel level) {
        if (level == AILevel.EASY) {
            return getEasyMove(board, aiPlayer);
        } else {
            return getHardMove(board, aiPlayer);
        }
    }

    private static int[] getEasyMove(Board board, char aiPlayer) {
        char humanPlayer = (aiPlayer == 'X') ? 'O' : 'X';
        char[][] grid = board.getGridCopy();
        List<int[]> emptyCells = getEmptyCells(grid);

        // 1. Может ли ИИ выиграть немедленно?
        for (int[] cell : emptyCells) {
            grid[cell[0]][cell[1]] = aiPlayer;
            if (checkWin(grid, aiPlayer)) {
                return cell;
            }
            grid[cell[0]][cell[1]] = ' ';
        }

        // 2. Блокировка выигрыша человека
        for (int[] cell : emptyCells) {
            grid[cell[0]][cell[1]] = humanPlayer;
            if (checkWin(grid, humanPlayer)) {
                return cell;
            }
            grid[cell[0]][cell[1]] = ' ';
        }

        // 3. Попытка создать вилку (две линии с двумя своими и одной пустой)
        for (int[] cell : emptyCells) {
            grid[cell[0]][cell[1]] = aiPlayer;
            int forkLines = 0;
            for (int[][] line : getAllLines()) {
                int aiCount = 0;
                int empty = 0;
                for (int[] coord : new int[][]{line[0], line[1], line[2]}) {
                    if (grid[coord[0]][coord[1]] == aiPlayer) aiCount++;
                    else if (grid[coord[0]][coord[1]] == ' ') empty++;
                }
                if (aiCount == 2 && empty == 1) forkLines++;
            }
            grid[cell[0]][cell[1]] = ' ';
            if (forkLines >= 2) {
                return cell;
            }
        }

        // 4. Центр
        if (grid[1][1] == ' ') {
            return new int[]{1, 1};
        }

        // 5. Углы
        int[][] corners = {{0,0}, {0,2}, {2,0}, {2,2}};
        for (int[] corner : corners) {
            if (grid[corner[0]][corner[1]] == ' ') {
                return corner;
            }
        }

        // 6. Случайный ход
        if (!emptyCells.isEmpty()) {
            return emptyCells.get(new Random().nextInt(emptyCells.size()));
        }
        return null; // Нет доступных ходов
    }

    private static int[] getHardMove(Board board, char aiPlayer) {
        char[][] grid = board.getGridCopy();
        char humanPlayer = (aiPlayer == 'X') ? 'O' : 'X';
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        List<int[]> moves = getEmptyCells(grid);
        for (int[] move : moves) {
            grid[move[0]][move[1]] = aiPlayer;
            int score = minimax(grid, 0, false, aiPlayer, humanPlayer, alpha, beta);
            grid[move[0]][move[1]] = ' ';
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
        }
        return bestMove;
    }

    private static int minimax(char[][] grid, int depth, boolean isMaximizing,
                               char aiPlayer, char humanPlayer, int alpha, int beta) {
        // Терминальные состояния
        if (checkWin(grid, aiPlayer)) {
            return 100 - depth;
        }
        if (checkWin(grid, humanPlayer)) {
            return -100 + depth;
        }
        if (isBoardFull(grid)) {
            return 0;
        }
        // Ограничение глубины
        if (depth >= 6) {
            return evaluateBoard(grid, aiPlayer, humanPlayer);
        }

        List<int[]> moves = getEmptyCells(grid);
        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (int[] move : moves) {
                grid[move[0]][move[1]] = aiPlayer;
                int score = minimax(grid, depth + 1, false, aiPlayer, humanPlayer, alpha, beta);
                grid[move[0]][move[1]] = ' ';
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) break;
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (int[] move : moves) {
                grid[move[0]][move[1]] = humanPlayer;
                int score = minimax(grid, depth + 1, true, aiPlayer, humanPlayer, alpha, beta);
                grid[move[0]][move[1]] = ' ';
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            return minScore;
        }
    }

    private static int evaluateBoard(char[][] grid, char aiPlayer, char humanPlayer) {
        int score = 0;
        for (int[][] line : getAllLines()) {
            int aiCount = 0, humanCount = 0;
            for (int[] coord : new int[][]{line[0], line[1], line[2]}) {
                if (grid[coord[0]][coord[1]] == aiPlayer) aiCount++;
                else if (grid[coord[0]][coord[1]] == humanPlayer) humanCount++;
            }
            if (aiCount > 0 && humanCount > 0) continue; // линия заблокирована
            if (aiCount == 3) score += 100;
            else if (aiCount == 2) score += 10;
            else if (aiCount == 1) score += 1;
            if (humanCount == 3) score -= 100;
            else if (humanCount == 2) score -= 10;
            else if (humanCount == 1) score -= 1;
        }
        return score;
    }

    private static boolean checkWin(char[][] grid, char player) {
        for (int[][] line : getAllLines()) {
            boolean win = true;
            for (int[] coord : new int[][]{line[0], line[1], line[2]}) {
                if (grid[coord[0]][coord[1]] != player) {
                    win = false;
                    break;
                }
            }
            if (win) return true;
        }
        return false;
    }

    private static boolean isBoardFull(char[][] grid) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[i][j] == ' ') return false;
            }
        }
        return true;
    }

    private static List<int[]> getEmptyCells(char[][] grid) {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[i][j] == ' ') list.add(new int[]{i, j});
            }
        }
        return list;
    }

    // Возвращает все линии в виде массива из трёх координат (каждая координата — массив из двух int)
    private static List<int[][]> getAllLines() {
        List<int[][]> lines = new ArrayList<>();
        // строки
        for (int i = 0; i < 3; i++) {
            lines.add(new int[][]{{i, 0}, {i, 1}, {i, 2}});
        }
        // столбцы
        for (int j = 0; j < 3; j++) {
            lines.add(new int[][]{{0, j}, {1, j}, {2, j}});
        }
        // диагонали
        lines.add(new int[][]{{0, 0}, {1, 1}, {2, 2}});
        lines.add(new int[][]{{0, 2}, {1, 1}, {2, 0}});
        return lines;
    }
}

// Главное окно приложения (View + Controller)
public class MainForm extends JFrame {
    private Board board;
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private GameMode gameMode = GameMode.PVP;
    private AILevel aiLevel = AILevel.HARD;
    private char currentPlayer = 'X';
    private boolean gameOver = false;
    private Timer blinkTimer;
    private int blinkCount = 0;
    private boolean isBlinkOn = false;
    private Color winFinalColor = new Color(255, 255, 0); // желтый

    // Счетчики побед
    private int winsX = 0;
    private int winsO = 0;
    private int draws = 0;

    public MainForm() {
        board = Board.getInstance();
        initUI();
        updateScoreDisplay();
        updateStatus();
    }

    private void initUI() {
        setTitle("Крестики-нолики");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Верхняя панель: статус, счёт
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.LIGHT_GRAY);
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(scoreLabel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Центральное поле
        JPanel centerPanel = new JPanel(new GridLayout(3, 3, 3, 3));
        centerPanel.setBackground(Color.BLACK); // линии толщиной 3px
        buttons = new JButton[3][3];
        Font buttonFont = new Font("Arial", Font.BOLD, 48);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton btn = new JButton();
                btn.setFont(buttonFont);
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLUE); // будет меняться
                btn.setOpaque(true);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                int row = i, col = j;
                btn.addActionListener(e -> handleCellClick(row, col));
                buttons[i][j] = btn;
                centerPanel.add(btn);
            }
        }
        add(centerPanel, BorderLayout.CENTER);

        // Нижняя панель: кнопки Новая игра, Выход
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

        // Меню
        setupMenu();
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Меню Файл
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem newGameItem = new JMenuItem("Новая игра");
        newGameItem.addActionListener(e -> showNewGameDialog());
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(newGameItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Меню Режим
        JMenu modeMenu = new JMenu("Режим");
        JRadioButtonMenuItem pvpItem = new JRadioButtonMenuItem("Игрок против игрока");
        JRadioButtonMenuItem pveItem = new JRadioButtonMenuItem("Игрок против компьютера");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(pvpItem);
        modeGroup.add(pveItem);
        pvpItem.setSelected(true);
        pvpItem.addActionListener(e -> {
            gameMode = GameMode.PVP;
            resetGame(); // сразу начать новую игру с новым режимом
        });
        pveItem.addActionListener(e -> {
            gameMode = GameMode.PVE;
            resetGame();
        });
        modeMenu.add(pvpItem);
        modeMenu.add(pveItem);

        menuBar.add(fileMenu);
        menuBar.add(modeMenu);
        setJMenuBar(menuBar);
    }

    private void showNewGameDialog() {
        // Диалог выбора режима и уровня ИИ
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JRadioButton pvpRadio = new JRadioButton("Два игрока");
        JRadioButton pveRadio = new JRadioButton("Против компьютера");
        ButtonGroup bg = new ButtonGroup();
        bg.add(pvpRadio);
        bg.add(pveRadio);
        if (gameMode == GameMode.PVP) pvpRadio.setSelected(true);
        else pveRadio.setSelected(true);

        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel levelLabel = new JLabel("Уровень ИИ:");
        JComboBox<String> levelCombo = new JComboBox<>(new String[]{"Лёгкий", "Сложный"});
        levelCombo.setSelectedIndex(aiLevel == AILevel.EASY ? 0 : 1);
        levelPanel.add(levelLabel);
        levelPanel.add(levelCombo);

        // Слушатель, чтобы блокировать выбор уровня при PvP
        ActionListener enableListener = e -> {
            boolean isPvE = pveRadio.isSelected();
            levelLabel.setEnabled(isPvE);
            levelCombo.setEnabled(isPvE);
        };
        pvpRadio.addActionListener(enableListener);
        pveRadio.addActionListener(enableListener);
        enableListener.actionPerformed(null); // начальное состояние

        panel.add(pvpRadio);
        panel.add(pveRadio);
        panel.add(levelPanel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новая игра",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            gameMode = pveRadio.isSelected() ? GameMode.PVE : GameMode.PVP;
            if (gameMode == GameMode.PVE) {
                aiLevel = levelCombo.getSelectedIndex() == 0 ? AILevel.EASY : AILevel.HARD;
            }
            resetGame();
        }
    }

    private void resetGame() {
        // Остановить мигание, если идёт
        if (blinkTimer != null && blinkTimer.isRunning()) {
            blinkTimer.stop();
        }
        board.reset();
        gameOver = false;
        currentPlayer = 'X';
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].setEnabled(true);
            }
        }
        updateStatus();
        // Если игра против компьютера, то первый ход человека, ничего не делаем.
    }

    private void handleCellClick(int row, int col) {
        if (gameOver) return;
        // Проверяем, что ход игрока (в PvE может быть не его очередь)
        if (gameMode == GameMode.PVE && currentPlayer == 'O') return;
        // Проверяем, что клетка свободна
        if (board.getCell(row, col) != ' ') return;

        // Выполняем ход
        makeMoveOnBoard(row, col, currentPlayer);
        checkGameState();

        if (!gameOver) {
            // Смена игрока
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
            updateStatus();

            // Если режим PvE и сейчас ход компьютера
            if (gameMode == GameMode.PVE && currentPlayer == 'O') {
                scheduleComputerMove();
            }
        }
    }

    private void makeMoveOnBoard(int row, int col, char player) {
        board.makeMove(row, col, player);
        JButton btn = buttons[row][col];
        btn.setText(String.valueOf(player));
        btn.setForeground(player == 'X' ? Color.BLUE : Color.RED);
    }

    private void scheduleComputerMove() {
        // Задержка 400 мс для имитации раздумий
        Timer timer = new Timer(400, e -> {
            computerMove();
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void computerMove() {
        if (gameOver) return;
        int[] move = AIPlayer.getMove(board, 'O', aiLevel);
        if (move != null) {
            makeMoveOnBoard(move[0], move[1], 'O');
        }
        checkGameState();
        if (!gameOver) {
            currentPlayer = 'X';
            updateStatus();
        }
    }

    private void checkGameState() {
        char winner = board.getWinner();
        if (winner != ' ') {
            gameOver = true;
            highlightWinningLine();
            startBlink();
            updateScore(winner);
            statusLabel.setText("Победил " + winner);
        } else if (board.isFull()) {
            gameOver = true;
            draws++;
            updateScoreDisplay();
            statusLabel.setText("Ничья");
        }
    }

    private void highlightWinningLine() {
        List<int[]> cells = board.getWinningCells();
        if (cells != null) {
            for (int[] cell : cells) {
                buttons[cell[0]][cell[1]].setBackground(Color.GREEN);
            }
        }
    }

    private void startBlink() {
        List<int[]> cells = board.getWinningCells();
        if (cells == null) return;
        blinkCount = 0;
        isBlinkOn = true;
        blinkTimer = new Timer(200, e -> {
            Color color = isBlinkOn ? winFinalColor : Color.GREEN;
            for (int[] cell : cells) {
                buttons[cell[0]][cell[1]].setBackground(color);
            }
            isBlinkOn = !isBlinkOn;
            blinkCount++;
            if (blinkCount >= 6) { // 3 полных цикла мигания
                blinkTimer.stop();
                // Окончательно оставляем жёлтый
                for (int[] cell : cells) {
                    buttons[cell[0]][cell[1]].setBackground(winFinalColor);
                }
            }
        });
        blinkTimer.start();
    }

    private void updateScore(char winner) {
        if (winner == 'X') winsX++;
        else if (winner == 'O') winsO++;
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Счёт: X - " + winsX + " | O - " + winsO + " | Ничьи - " + draws);
    }

    private void updateStatus() {
        if (!gameOver) {
            statusLabel.setText("Ход: " + currentPlayer);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainForm form = new MainForm();
            form.setVisible(true);
        });
    }
}