
package src.pas.battleship.agents;


// SYSTEM IMPORTS
import java.util.Arrays;
import java.io.*;
import java.util.Stack;

// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.EnemyBoard;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;
import edu.bu.battleship.game.ships.Ship.ShipType;
import edu.bu.battleship.game.Constants;

public class ProbabilisticAgent
    extends Agent
{
    /* track the most recent vertex that was attacked */
    private Coordinate lastAttack = new Coordinate(0,0);

    /* agent modes */
    enum Mode { HUNT, TARGET }; 
    private Mode mode = Mode.HUNT;
    private Stack<Coordinate> huntingTargets = new Stack<Coordinate>();

    public ProbabilisticAgent(String name)
    {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    
    public void setLastAttack(Coordinate newCoordinate){ this.lastAttack = newCoordinate; }
    public Coordinate getLastAttack(){ return this.lastAttack; }
    public void setMode(Mode newMode){ this.mode = newMode; }
    public Mode getMode(){ return this.mode; }

    @Override
    public Coordinate makeMove(final GameView game)
    {
        Constants gameConstants = game.getGameConstants();
        Coordinate bestCell = new Coordinate(0,0);
        EnemyBoard.Outcome[][] enemyBoard = game.getEnemyBoardView();
        float[][] probabilities = new float[gameConstants.getNumRows()][gameConstants.getNumCols()];

        /* if most recent attack was a HIT and current not in Target mode*/
        if (this.mode != Mode.TARGET && 
            enemyBoard[this.lastAttack.getXCoordinate()][this.lastAttack.getYCoordinate()] == Outcome.HIT) {
            setMode(Mode.TARGET);
        }
    
        /* execute appropriate method based on current Mode */
        bestCell = (this.mode == Mode.HUNT) ? Hunt(game, probabilities, enemyBoard) 
                                            : Target(game, this.lastAttack, enemyBoard);

        setLastAttack(bestCell);
        return bestCell;
    }

    /* Hunt mode: agent looking for a HIT */
    public Coordinate Hunt(final GameView game, float[][] probabilities, EnemyBoard.Outcome[][] enemyBoard){
        float highestProb = 0f;
        Coordinate bestCell = new Coordinate(0,0);
        System.out.println("we out here hunting");

        /* assign prob for each cell */
        for (int row = 0; row < probabilities.length; row++) {
            for (int col = 0; col < probabilities.length; col++) {
                /*check if cell has already been hit, if so continue */
                if (enemyBoard[row][col] != Outcome.UNKNOWN) {
                    continue;
                }
                for (ShipType c : ShipType.values()) {
                    /*check if this type has not been sunk yet */
                    // TO DO //

                    int shipSize = Constants.Ship.getShipSize(c);
                    /*check if horizontal/vertical orientation fits */
                    for (int orientation = 0; orientation < 2; orientation++) {
                        for (int offset = 0; offset < shipSize; offset++) {
                            int startRow = orientation == 0 ? row - offset : row;
                            int endRow = orientation == 0 ? startRow + shipSize - 1 : startRow;
                            int startCol = orientation == 1 ? col - offset : col;
                            int endCol = orientation == 1 ? startCol + shipSize - 1 : startCol;

                            if (canPlaceShip(startRow, endRow, startCol, endCol, game)) {
                                probabilities[row][col] += 1;
                            }
                        }
                    }
                }

                if (probabilities[row][col] > highestProb) {
                    highestProb = probabilities[row][col];
                    bestCell = new Coordinate(row,col);
                }
            }
        }
        return bestCell;
    }

    /* Target mode: agent has HIT a ship */
    public Coordinate Target(GameView game, Coordinate lastHit, EnemyBoard.Outcome[][] enemyBoard){
        System.out.println("you've reached target mode ;)");
        int xCoord = lastHit.getXCoordinate();
        int yCoord = lastHit.getYCoordinate();
        if (enemyBoard[xCoord][yCoord] == Outcome.HIT) {
            huntingTargets.push(lastHit);
        }

        int[] cardDir = {-1, 0, 1};
        huntingTargets.pop();
        while (!huntingTargets.isEmpty()) {
            for (int xOffset : cardDir) {
                for (int yOffset : cardDir) {
                    if (xOffset == yOffset) {
                        continue;
                    }
                    int nextX = xCoord + xOffset;
                    int nextY = yCoord + yOffset;
                    if (enemyBoard[nextX][nextY] == Outcome.UNKNOWN && game.isInBounds(nextX, nextY)) {
                        return new Coordinate(nextX, nextY);
                    }
                }
            }
            huntingTargets.pop();
        
        } 
        setMode(Mode.HUNT);
        return Hunt(game, ,enemyBoard);
    }


    // Helper method to check if a ship can be placed (considering board bounds and hit/miss information)
    private boolean canPlaceShip(int startRow, int endRow, int startCol, int endCol, final GameView game) {
        if (!game.isInBounds(startRow, startCol) || !game.isInBounds(endRow, endCol)) {
            return false; // Out of bounds
        }

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (game.getEnemyBoardView()[row][col] == EnemyBoard.Outcome.MISS ||
                    game.getEnemyBoardView()[row][col] == EnemyBoard.Outcome.SUNK) {
                    return false; // Cannot place over misses or sunk areas
                }
            }
        }

        return true; // Valid placement
    }
    

    @Override
    public void afterGameEnds(final GameView game) {}

}