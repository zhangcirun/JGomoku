package ai;

import ai.constant.AiConst;
import ai.utility.AiUtils;
import ai.utility.ChessboardScanUtils;
import gui.Background;
import gui.Chessboard;
import gui.constant.GuiConst;

import java.util.ArrayList;

/**
 * This class is an Ai agent uses threat space search
 *
 * @author Cirun Zhang
 * @version 1.1
 */
public class ThreatSpace extends Agent {
    /**
     * The list contains a valid wining sequence with the format of <[lastX, lastY], [x, y]>
     */
    private static ArrayList<int[]> threatSequence = new ArrayList<>(2);

    private ThreatSpace() {
    }

    /**
     * The entrance of threat space search
     *
     * @param chess The chessboard
     * @return Coordinates of the best next AI move
     */
    public static int[] startThreatSpaceSearch(int[][] chess) {
        if (detectThreats(chess, aiPieceType)) {
            //Background.addMessage("emergent");
            return MinimaxAbp.startAlphaBetaPruningWithSort(chess);
        }
        if (!threatSequence.isEmpty()) {
            if (isMoveValid(threatSequence.get(0), chess)) {
                System.out.println("Wining sequence found, second move");
                Background.addMessage("Wining sequence found, second move");
                Background.addMessage("---------------------------------");
                return threatSequence.remove(0);
            } else {
                threatSequence.clear();
                //Background.addMessage("threat move is blocked");
                return MinimaxAbp.startAlphaBetaPruningWithSort(chess);
            }

        } else if (threatSpaceSearch(chess, 0, -1, -1)) {
            Background.addMessage("---------------------------------");
            Background.addMessage("Wining sequence found, first move");
            System.out.println("Wining sequence found, first move");
            return threatSequence.remove(0);
        } else {
            return MinimaxAbp.startAlphaBetaPruningWithSort(chess);
        }
    }

    /**
     * Method of depth-first threat space search.
     * <p>
     * Scans each possible gain squares recursively, if it forms a wining sequence with one of
     * the dependent child node, store the wining sequence and return
     *
     * @param chess       The chessboard
     * @param depth       Current depth of the search tree
     * @param lastThreatX X-coordinate of the last threat move
     * @param lastThreatY Y-coordinate of the last threat move
     * @return Whether the wining sequence is found or not
     */
    public static boolean threatSpaceSearch(int[][] chess, int depth, int lastThreatX, int lastThreatY) {
        //Represent wining sequence is found or not
        boolean success = false;
        if (depth >= 2) {
            //System.out.println("finish");
            return false;
        }

        //for each empty tiles
        for (int i = 0; i < GuiConst.TILE_NUM_PER_ROW; i++) {
            for (int j = 0; j < GuiConst.TILE_NUM_PER_ROW; j++) {
                int threatDirection;

                if (chess[i][j] == 0 && ((threatDirection = detectPotentialThreatWithDirection(i, j, chess))
                    != AiConst.NO_THREAT)) {
                    if (detectWiningThreatSequence(i, j, lastThreatX, lastThreatY, chess)) {
                        threatSequence.add(new int[] {lastThreatX, lastThreatY, aiPieceType});
                        threatSequence.add(new int[] {i, j, aiPieceType});
                        return true;
                    } else {
                        //NOTE: | or || do effect the recursion
                        //lazy Operator(||) would stop the recursion after the first wining sequence is found.
                        int[][] nextChess = AiUtils.nextMoveChessboard(chess, i, j, aiPieceType);
                        //Defense the threat.
                        defenseSimulation(nextChess, i, j, aiPieceType, threatDirection);

                        success = success || threatSpaceSearch(nextChess, depth + 1, i, j);
                    }
                }
            }
        }
        return success;
    }

    /**
     * Detects whether the given moves form a wining sequence
     *
     * @param x     X-coordinate of the second move
     * @param y     Y-coordinate of the second move
     * @param lastX X-coordinate of the first move
     * @param lastY Y-coordinate of the first move
     * @param chess The chessboard
     * @return A boolean indicates whether or not the given moves form a wining sequence
     */
    private static boolean detectWiningThreatSequence(int x, int y, int lastX, int lastY, int[][] chess) {
        if (x == lastX) {
            //Same array, in the same row
            String pieces = ChessboardScanUtils.scanHorizontal(chess, x, y, lastX, lastY, aiPieceType);
            return isDependentThreat(pieces) && numOfNearbyThreats(x, y, chess, aiPieceType) >= 2;

        } else if (y == lastY) {
            //Same index in different array, in the same column
            String pieces = ChessboardScanUtils.scanVertical(chess, x, y, lastX, lastY, aiPieceType);
            return isDependentThreat(pieces) && numOfNearbyThreats(x, y, chess, aiPieceType) >= 2;

        } else if (x - lastX == -(y - lastY)) {
            //diagonal
            String pieces = ChessboardScanUtils.scanDiagonal(chess, x, y, lastX, lastY, aiPieceType);
            return isDependentThreat(pieces) && numOfNearbyThreats(x, y, chess, aiPieceType) >= 2;
        } else if (x - lastX == y - lastY) {
            //anti-diagonal
            String pieces = ChessboardScanUtils.scanAntiDiagonal(chess, x, y, lastX, lastY, aiPieceType);
            return isDependentThreat(pieces) && numOfNearbyThreats(x, y, chess, aiPieceType) >= 2;
        }
        return false;
    }

    /**
     * Determine whether or not a position is a gain square, if yes, return the direction of the threat
     *
     * @param x     X coordinate of a empty tile
     * @param y     Y coordinate of a empty tile
     * @param chess The chessboard
     * @return Direction of the threat
     */
    private static int detectPotentialThreatWithDirection(int x, int y, int[][] chess) {
        String vertical = ChessboardScanUtils.verticalAdjacentPieces10(chess, x, y, aiPieceType, 't');
        String horizontal = ChessboardScanUtils.horizontalAdjacentPieces10(chess, x, y, aiPieceType, 't');
        String diagonal = ChessboardScanUtils.diagonalAdjacentPieces10(chess, x, y, aiPieceType, 't');
        String antiDiagonal = ChessboardScanUtils.antiDiagonalAdjacentPieces10(chess, x, y, aiPieceType, 't');

        if (horizontal.contains(AiConst.POTENTIAL_THREAT_A) || horizontal.contains(AiConst.POTENTIAL_THREAT_B)
            || horizontal.contains(AiConst.POTENTIAL_THREAT_C) || horizontal.contains(AiConst.POTENTIAL_THREAT_D)
            || horizontal.contains(AiConst.POTENTIAL_THREAT_E) || horizontal.contains(AiConst.POTENTIAL_THREAT_F)
            || horizontal.contains(AiConst.POTENTIAL_THREAT_G) || horizontal.contains(AiConst.POTENTIAL_THREAT_H)
            || horizontal.contains(AiConst.POTENTIAL_THREAT_I)) {
            return AiConst.HORIZONTAL_THREAT;
        } else if (vertical.contains(AiConst.POTENTIAL_THREAT_A) || vertical.contains(AiConst.POTENTIAL_THREAT_B)
            || vertical.contains(AiConst.POTENTIAL_THREAT_C) || vertical.contains(AiConst.POTENTIAL_THREAT_D)
            || vertical.contains(AiConst.POTENTIAL_THREAT_E) || vertical.contains(AiConst.POTENTIAL_THREAT_F)
            || vertical.contains(AiConst.POTENTIAL_THREAT_G) || vertical.contains(AiConst.POTENTIAL_THREAT_H)
            || vertical.contains(AiConst.POTENTIAL_THREAT_I)) {
            return AiConst.VERTICAL_THREAT;
        } else if (diagonal.contains(AiConst.POTENTIAL_THREAT_A) || diagonal.contains(AiConst.POTENTIAL_THREAT_B)
            || diagonal.contains(AiConst.POTENTIAL_THREAT_C) || diagonal.contains(AiConst.POTENTIAL_THREAT_D)
            || diagonal.contains(AiConst.POTENTIAL_THREAT_E) || diagonal.contains(AiConst.POTENTIAL_THREAT_F)
            || diagonal.contains(AiConst.POTENTIAL_THREAT_G) || diagonal.contains(AiConst.POTENTIAL_THREAT_H)
            || diagonal.contains(AiConst.POTENTIAL_THREAT_I)) {
            return AiConst.DIAGONAL_THREAT;
        } else if (antiDiagonal.contains(AiConst.POTENTIAL_THREAT_A) || antiDiagonal
            .contains(AiConst.POTENTIAL_THREAT_B) || antiDiagonal.contains(AiConst.POTENTIAL_THREAT_C) || antiDiagonal
            .contains(AiConst.POTENTIAL_THREAT_D) || antiDiagonal.contains(AiConst.POTENTIAL_THREAT_E) || antiDiagonal
            .contains(AiConst.POTENTIAL_THREAT_F) || antiDiagonal.contains(AiConst.POTENTIAL_THREAT_G) || antiDiagonal
            .contains(AiConst.POTENTIAL_THREAT_H) || antiDiagonal.contains(AiConst.POTENTIAL_THREAT_I)) {
            return AiConst.ANTIDIAGONAL_THREAT;
        }
        return AiConst.NO_THREAT;
    }

    /**
     * Determine whether a threat is a dependent threat or not
     *
     * @param threat Pieces of the threat
     * @return A boolean indicate whether a threat is dependent or not
     */
    private static boolean isDependentThreat(String threat) {
        if (threat.contains(AiConst.DEPENDENT_THREAT_A) || threat.contains(AiConst.DEPENDENT_THREAT_B) || threat
            .contains(AiConst.DEPENDENT_THREAT_C) || threat.contains(AiConst.DEPENDENT_THREAT_D) || threat
            .contains(AiConst.DEPENDENT_THREAT_E) || threat.contains(AiConst.DEPENDENT_THREAT_F) || threat
            .contains(AiConst.DEPENDENT_THREAT_G) || threat.contains(AiConst.DEPENDENT_THREAT_H) || threat
            .contains(AiConst.DEPENDENT_THREAT_I) || threat.contains(AiConst.DEPENDENT_THREAT_J) || threat
            .contains(AiConst.DEPENDENT_THREAT_K) || threat.contains(AiConst.DEPENDENT_THREAT_L) || threat
            .contains(AiConst.DEPENDENT_THREAT_M) || threat.contains(AiConst.DEPENDENT_THREAT_O) || threat
            .contains(AiConst.DEPENDENT_THREAT_P) || threat.contains(AiConst.DEPENDENT_THREAT_Q) || threat
            .contains(AiConst.DEPENDENT_THREAT_R) || threat.contains(AiConst.DEPENDENT_THREAT_S) || threat
            .contains(AiConst.DEPENDENT_THREAT_T) || threat.contains(AiConst.DEPENDENT_THREAT_U) || threat
            .contains(AiConst.DEPENDENT_THREAT_V) || threat.contains(AiConst.DEPENDENT_THREAT_W) || threat
            .contains(AiConst.DEPENDENT_THREAT_X) || threat.contains(AiConst.DEPENDENT_THREAT_Y) || threat
            .contains(AiConst.DEPENDENT_THREAT_Z) || threat.contains(AiConst.DEPENDENT_THREAT_AA) || threat
            .contains(AiConst.DEPENDENT_THREAT_AB) || threat.contains(AiConst.DEPENDENT_THREAT_AC) || threat
            .contains(AiConst.DEPENDENT_THREAT_AD) || threat.contains(AiConst.DEPENDENT_THREAT_AE) || threat
            .contains(AiConst.DEPENDENT_THREAT_AF) || threat.contains(AiConst.DEPENDENT_THREAT_AG) || threat
            .contains(AiConst.DEPENDENT_THREAT_AH) || threat.contains(AiConst.DEPENDENT_THREAT_AI)) {

            System.out.println("Found Dependent Sequence.");
            return true;
        }
        return false;
    }

    /**
     * Returns the number of threats near by a particular piece
     *
     * @param x         X coordinate of the piece
     * @param y         Y coordinate of the piece
     * @param chess     The chessboard
     * @param pieceType Piece type of AI
     * @return Number of the threats
     */
    private static int numOfNearbyThreats(int x, int y, int[][] chess, int pieceType) {
        String horizontal = ChessboardScanUtils.verticalAdjacentPieces10(chess, x, y, pieceType, '1');
        String vertical = ChessboardScanUtils.horizontalAdjacentPieces10(chess, x, y, pieceType, '1');
        String diagonal = ChessboardScanUtils.diagonalAdjacentPieces10(chess, x, y, pieceType, '1');
        String antiDiagonal = ChessboardScanUtils.antiDiagonalAdjacentPieces10(chess, x, y, pieceType, '1');

        return detectOneRowThreats(horizontal, chess) + detectOneRowThreats(vertical, chess) + detectOneRowThreats(
            diagonal, chess) + detectOneRowThreats(antiDiagonal, chess);
    }

    /**
     * Return the number of threats in a row
     *
     * @param pieces Pieces in a row
     * @param chess  The chessboard
     * @return Number of threats
     */
    private static int detectOneRowThreats(String pieces, int[][] chess) {
        if (pieces.contains(AiConst.IMPLICATE_FOUR_SINGLE_EMPTY_A) || pieces
            .contains(AiConst.IMPLICATE_FOUR_SINGLE_EMPTY_B) || pieces.contains(AiConst.IMPLICATE_FOUR_SINGLE_EMPTY_C)
            || pieces.contains(AiConst.IMPLICATE_FOUR_SINGLE_EMPTY_D) || pieces
            .contains(AiConst.IMPLICATE_FOUR_SINGLE_EMPTY_E) || pieces.contains(AiConst.IMPLICATE_THREE_A) || pieces
            .contains(AiConst.IMPLICATE_THREE_B) || pieces.contains(AiConst.IMPLICATE_THREE_C) || pieces
            .contains(AiConst.IMPLICATE_THREE_D)) {
            return 1;
        }
        return 0;
    }

    /**
     * Determine whether the threat move is valid or not
     *
     * @param threatMove AI threat move
     * @param chess      The chessboard
     * @return A boolean indicates the validity of the move
     */
    private static boolean isMoveValid(int[] threatMove, int[][] chess) {
        return chess[threatMove[0]][threatMove[1]] == AiConst.EMPTY_STONE;
    }

    /**
     * Defend the threat
     *
     * @param chess           The chessboard
     * @param x               X coordinate of the gain square
     * @param y               Y coordinate of the gain square
     * @param pieceType       Piece type of AI
     * @param threatDirection Direction of the threat
     */
    private static void defenseSimulation(int[][] chess, int x, int y, int pieceType, int threatDirection) {
        switch (threatDirection) {
            case AiConst.HORIZONTAL_THREAT:
                defenseHorizontalThreat(chess, x, y, pieceType);
                break;
            case AiConst.VERTICAL_THREAT:
                defenseVerticalThreat(chess, x, y, pieceType);
                break;
            case AiConst.DIAGONAL_THREAT:
                defenseDiagonalThreat(chess, x, y, pieceType);
                break;
            case AiConst.ANTIDIAGONAL_THREAT:
                defenseAntiDiagonalThreat(chess, x, y, pieceType);
                break;
            default:
                System.out.println("threatDirection " + threatDirection);
        }
    }

    /**
     * Defend the threat in vertical direction
     *
     * @param chess     The chessboard
     * @param x         X coordinate of the gain square
     * @param y         Y coordinate of the gain square
     * @param pieceType Piece type of AI
     */
    private static void defenseVerticalThreat(int[][] chess, int x, int y, int pieceType) {
        ArrayList<ThreatMove> defensiveMoves = new ArrayList<>();
        ArrayList<ThreatMove> possibleMoves = new ArrayList<>();

        for (int i = x - 4; i < x; i++) {
            if (Chessboard.validateArrayIndex(i) && chess[i][y] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.verticalAdjacentPieces10(chess, i, y, pieceType, 't');
                possibleMoves.add(new ThreatMove(i, y, pieces));
            }
        }

        for (int i = x + 1; i < x + 5; i++) {
            if (Chessboard.validateArrayIndex(i) && chess[i][y] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.verticalAdjacentPieces10(chess, i, y, pieceType, 't');
                possibleMoves.add(new ThreatMove(i, y, pieces));
            }
        }

        generateDefensiveMoves(defensiveMoves, possibleMoves);

        for (ThreatMove move : defensiveMoves) {
            chess[move.getX()][move.getY()] = pieceType * -1;
        }

    }

    /**
     * Defend the threat in horizontal direction
     *
     * @param chess     The chessboard
     * @param x         X coordinate of the gain square
     * @param y         Y coordinate of the gain square
     * @param pieceType Piece type of AI
     */
    private static void defenseHorizontalThreat(int[][] chess, int x, int y, int pieceType) {
        ArrayList<ThreatMove> defensiveMoves = new ArrayList<>();
        ArrayList<ThreatMove> possibleMoves = new ArrayList<>();

        for (int i = y - 4; i < y; i++) {
            if (Chessboard.validateArrayIndex(i) && chess[x][i] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.horizontalAdjacentPieces10(chess, x, i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x, i, pieces));
            }
        }

        for (int i = y + 1; i < y + 5; i++) {
            if (Chessboard.validateArrayIndex(i) && chess[x][i] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.horizontalAdjacentPieces10(chess, x, i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x, i, pieces));
            }
        }

        generateDefensiveMoves(defensiveMoves, possibleMoves);

        for (ThreatMove move : defensiveMoves) {
            chess[move.getX()][move.getY()] = pieceType * -1;
        }
    }

    /**
     * Defend the threat in diagonal direction
     *
     * @param chess     The chessboard
     * @param x         X coordinate of the gain square
     * @param y         Y coordinate of the gain square
     * @param pieceType Piece type of AI
     */
    private static void defenseDiagonalThreat(int[][] chess, int x, int y, int pieceType) {
        ArrayList<ThreatMove> defensiveMoves = new ArrayList<>();
        ArrayList<ThreatMove> possibleMoves = new ArrayList<>();

        for (int i = 4; i > 0; i--) {
            if (Chessboard.validateArrayIndex(x - i) && Chessboard.validateArrayIndex(y - i)
                && chess[x - i][y - i] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.diagonalAdjacentPieces10(chess, x - i, y - i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x - i, y - i, pieces));
            }
        }
        //check from target to right bottom
        for (int i = 1; i < 5; i++) {
            if (Chessboard.validateArrayIndex(x + i) && Chessboard.validateArrayIndex(y + i)
                && chess[x + i][y + i] == AiConst.EMPTY_STONE) {
                String pieces = ChessboardScanUtils.diagonalAdjacentPieces10(chess, x + i, y + i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x + i, y + i, pieces));
            }
        }

        generateDefensiveMoves(defensiveMoves, possibleMoves);

        for (ThreatMove move : defensiveMoves) {
            chess[move.getX()][move.getY()] = pieceType * -1;
        }
    }

    /**
     * Defend the threat in anti-diagonal direction
     *
     * @param chess     The chessboard
     * @param x         X coordinate of the gain square
     * @param y         Y coordinate of the gain square
     * @param pieceType Piece type of AI
     */
    private static void defenseAntiDiagonalThreat(int[][] chess, int x, int y, int pieceType) {
        ArrayList<ThreatMove> defensiveMoves = new ArrayList<>();
        ArrayList<ThreatMove> possibleMoves = new ArrayList<>();

        for (int i = 4; i > 0; i--) {
            if (Chessboard.validateArrayIndex(x + i) && Chessboard.validateArrayIndex(y - i)
                && chess[x + i][y - i] == AiConst.EMPTY_STONE) {

                String pieces = ChessboardScanUtils.antiDiagonalAdjacentPieces10(chess, x + i, y - i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x + i, y - i, pieces));
            }
        }

        //check from target to left bottom
        for (int i = 1; i < 5; i++) {
            if (Chessboard.validateArrayIndex(x - i) && Chessboard.validateArrayIndex(y + i)
                && chess[x - i][y + i] == AiConst.EMPTY_STONE) {

                String pieces = ChessboardScanUtils.antiDiagonalAdjacentPieces10(chess, x - i, y + i, pieceType, 't');
                possibleMoves.add(new ThreatMove(x - i, y + i, pieces));
            }
        }

        generateDefensiveMoves(defensiveMoves, possibleMoves);

        for (ThreatMove move : defensiveMoves) {
            chess[move.getX()][move.getY()] = pieceType * -1;
        }
    }

    /**
     * Generate all possible defensive moves of a threat
     *
     * @param defensiveMoves A list stores all defensive moves
     * @param possibleMoves  A list stores all possible moves
     */
    private static void generateDefensiveMoves(ArrayList<ThreatMove> defensiveMoves,
        ArrayList<ThreatMove> possibleMoves) {
        if (defensiveMoves.isEmpty()) {
            for (ThreatMove move : possibleMoves) {
                if (move.getPieces().contains(AiConst.DEFENSE_THREAT_A1) || move.getPieces()
                    .contains(AiConst.DEFENSE_THREAT_A2)) {
                    defensiveMoves.add(move);
                }
            }
        }

        if (defensiveMoves.isEmpty()) {
            for (ThreatMove move : possibleMoves) {
                if (move.getPieces().contains(AiConst.DEFENSE_THREAT_B1) || move.getPieces()
                    .contains(AiConst.DEFENSE_THREAT_B2)) {
                    defensiveMoves.add(move);
                }
            }
        }

        if (defensiveMoves.isEmpty()) {
            for (ThreatMove move : possibleMoves) {
                if (move.getPieces().contains(AiConst.DEFENSE_THREAT_C1) || move.getPieces()
                    .contains(AiConst.DEFENSE_THREAT_C2) || move.getPieces().contains(AiConst.DEFENSE_THREAT_C3) || move
                    .getPieces().contains(AiConst.DEFENSE_THREAT_C4) || move.getPieces()
                    .contains(AiConst.DEFENSE_THREAT_C5) || move.getPieces().contains(AiConst.DEFENSE_THREAT_C6)) {
                    defensiveMoves.add(move);
                }
            }
        }

        if (defensiveMoves.isEmpty()) {
            for (ThreatMove move : possibleMoves) {
                if (move.getPieces().contains(AiConst.DEFENSE_THREAT_D1) || move.getPieces()
                    .contains(AiConst.DEFENSE_THREAT_D2)) {
                    defensiveMoves.add(move);
                }
            }
        }
    }

    /**
     * Detect whether a threat exists or not
     *
     * @param chess     The chessboard
     * @param pieceType Type of the piece
     * @return A boolean indicates the existence of threats
     */
    private static boolean detectThreats(int[][] chess, int pieceType) {
        return ChessboardScanUtils.scanVerticalThreat(chess, pieceType) || ChessboardScanUtils
            .scanHorizontalThreat(chess, pieceType) || ChessboardScanUtils.scanDiagonalThreat(chess, pieceType)
            || ChessboardScanUtils.scanAntiDiagonalThreat(chess, pieceType) || ChessboardScanUtils
            .scanVerticalThreat(chess, pieceType * -1) || ChessboardScanUtils
            .scanHorizontalThreat(chess, pieceType * -1) || ChessboardScanUtils
            .scanDiagonalThreat(chess, pieceType * -1) || ChessboardScanUtils
            .scanAntiDiagonalThreat(chess, pieceType * -1);
    }
}

/**
 * This class represents a threat move
 *
 * @author Chang tz'u jun
 * @version 1.0
 */
class ThreatMove {
    private int x;
    private int y;
    private String pieces;

    ThreatMove(int x, int y, String pieces) {
        this.x = x;
        this.y = y;
        this.pieces = pieces;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getPieces() {
        return pieces;
    }

    public void setPieces(String pieces) {
        this.pieces = pieces;
    }
}