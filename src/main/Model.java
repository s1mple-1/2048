package main;

import java.util.*;

public class Model {
    private static final int FIELD_WIDTH = 4;
    private Tile[][] gameTiles;
    protected int maxTile;
    protected int score;
    private boolean isSaveNeeded = true;
    private Stack<Tile[][]> previousStates = new Stack<>();
    private Stack<Integer> previousScores = new Stack<>();

    private void saveState(Tile[][] tile) {
        Tile[][] newTileArr = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < tile.length; i++) {
            for (int j = 0; j < tile[0].length; j++) {
                newTileArr[i][j] = new Tile(tile[i][j].value);
            }
        }
        previousScores.push(score);
        previousStates.push(newTileArr);
        this.isSaveNeeded = false;
    }

    public void rollback() {
        if (!previousScores.isEmpty() && !previousStates.isEmpty()) {
            score = previousScores.pop();
            gameTiles = previousStates.pop();
        }
    }

    public Tile[][] getGameTiles() {
        return gameTiles;
    }

    public Model() {
        resetGameTiles();
    }

    private List<Tile> getEmptyTiles() {
        List<Tile> tileList = new ArrayList<>();
        for (int i = 0; i < gameTiles.length; i++) {
            for (int j = 0; j < gameTiles[0].length; j++) {
                if (gameTiles[i][j].isEmpty()) {
                    tileList.add(gameTiles[i][j]);
                }
            }
        }
        return tileList;
    }

    public void addTile() {
        List<Tile> tileList = getEmptyTiles();
        if (tileList.size() != 0) {
            tileList.get((int) (Math.random() * tileList.size())).value = (Math.random() < 0.9 ? 2 : 4);
        }
    }

    public void resetGameTiles() {
        this.gameTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int i = 0; i < gameTiles.length; i++) {
            for (int j = 0; j < gameTiles[0].length; j++) {
                gameTiles[i][j] = new Tile();
            }
        }
        addTile();
        addTile();
        this.score = 0;
        this.maxTile = 0;
    }

    private boolean compressTiles(Tile[] tiles) {
        boolean flag = false;
        int count = 0;
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].value != 0) {
                if (tiles[count].value != tiles[i].value) flag = true;
                tiles[count++].value = tiles[i].value;
            }
        }
        for (int i = count; i < tiles.length; i++) {
            tiles[i].value = 0;
        }
        return flag;
    }

    private boolean mergeTiles(Tile[] tiles) {
        boolean flag = false;
        int count = 0;
        for (int i = 0; i < tiles.length - 1; i++) {
            if (tiles[i].value == tiles[i + 1].value && tiles[i].value != 0) {
                flag = true;
                tiles[i].value = tiles[i].value * 2;
                tiles[i + 1].value = 0;
                score += tiles[i].value;
                if (maxTile < tiles[i].value) maxTile = tiles[i].value;
            }
        }
        return flag;
    }

    public void left() {
        if (isSaveNeeded) {
            saveState(gameTiles);
            isSaveNeeded = true;
        }
        int count = 0;
        for (int i = 0; i < gameTiles.length; i++) {
            if (compressTiles(gameTiles[i])) count++;
            if (mergeTiles(gameTiles[i])) count++;
            if (compressTiles(gameTiles[i])) count++;
        }
        if (count != 0) addTile();
    }

    public void right() {
        saveState(gameTiles);
        rotateClockwise();
        rotateClockwise();
        left();
        rotateClockwise();
        rotateClockwise();
    }

    public void up() {
        saveState(gameTiles);
        rotateClockwise();
        rotateClockwise();
        rotateClockwise();
        left();
        rotateClockwise();
    }

    public void down() {
        saveState(gameTiles);
        rotateClockwise();
        left();
        rotateClockwise();
        rotateClockwise();
        rotateClockwise();
    }

    private void rotateClockwise() {
        Tile[][] gameFieldCopy = new Tile[FIELD_WIDTH][FIELD_WIDTH];
        for (int y = 0; y < gameFieldCopy.length; y++) {
            for (int x = 0; x < gameFieldCopy.length; x++) {
                gameFieldCopy[y][(gameFieldCopy.length - 1) - x] = gameTiles[x][y];
            }
        }
        for (int y = 0; y < gameTiles.length; y++) {
            for (int x = 0; x < gameTiles.length; x++) {
                gameTiles[y][x] = gameFieldCopy[y][x];
            }
        }
    }

    public boolean canMove() {
        for (int i = 0; i < gameTiles.length; i++) {
            for (int j = 0; j < gameTiles[0].length; j++) {
                if (gameTiles[i][j].value == 0)
                    return true;
                if (i != 0 && gameTiles[i - 1][j].value == gameTiles[i][j].value)
                    return true;
                if (j != 0 && gameTiles[i][j - 1].value == gameTiles[i][j].value)
                    return true;
            }
        }
        return false;
    }

    public void randomMove() {
        int random = ((int) (Math.random() * 100)) % 4;
        switch (random) {
            case 0:
                left();
                break;
            case 1:
                right();
                break;
            case 2:
                up();
                break;
            case 3:
                down();
                break;
        }
    }

    private boolean hasBoardChanged() {
        Tile[][] tileForCompare = previousStates.peek();
        for (int i = 0; i < gameTiles.length; i++) {
            for (int j = 0; j < gameTiles[0].length; j++) {
                if (gameTiles[i][j].value != tileForCompare[i][j].value) {
                    return true;
                }
            }
        }
        return false;
    }

    private MoveEfficiency getMoveEfficiency(Move move) {
        move.move();
        if (!hasBoardChanged()) {
            rollback();
            return new MoveEfficiency(-1, 0, move);
        }
        return new MoveEfficiency(getEmptyTiles().size(), score, move);
    }

    public void autoMove() {
        PriorityQueue<MoveEfficiency> priorityQueue = new PriorityQueue(4, Collections.reverseOrder());
        priorityQueue.offer(getMoveEfficiency(this::left));
        priorityQueue.offer(getMoveEfficiency(this::right));
        priorityQueue.offer(getMoveEfficiency(this::up));
        priorityQueue.offer(getMoveEfficiency(this::down));
        priorityQueue.peek().getMove().move();
    }
}
