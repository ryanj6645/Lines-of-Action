/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/**
 * Represents the state of a game of Lines of Action.
 *
 * @author Ryan Johnson
 */
class Board {

    /**
     * Default number of moves for each side that results in a draw.
     */
    static final int DEFAULT_MOVE_LIMIT = 30;

    /**
     * Pattern describing a valid square designator (cr).
     */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /**
     * A Board whose initial contents are taken from INITIALCONTENTS
     * and in which the player playing TURN is to move. The resulting
     * Board has
     * get(col, row) == INITIALCONTENTS[row][col]
     * Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     * <p>
     * CAUTION: The natural written notation for arrays initializers puts
     * the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /**
     * A new board in the standard initial position.
     */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /**
     * A Board whose initial contents and state are copied from
     * BOARD.
     */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /**
     * Set my state to CONTENTS with SIDE to move.
     */
    void initialize(Piece[][] contents, Piece side) {
        _turn = side;
        setMoveLimit(DEFAULT_MOVE_LIMIT);
        int count = 0;
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < contents[0].length; j++) {
                _board[count] = contents[i][j];
                count++;
            }
        }
        _winnerKnown = false;
        _winner = null;
        _subsetsInitialized = false;
        _whiteRegionSizes.clear();
        _whiteRegionSizes.clear();
        _bMoveCount = 0;
        _wMoveCount = 0;
    }

    /**
     * Set me to the initial configuration.
     */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /**
     * Set my state to a copy of BOARD.
     */
    void copyFrom(Board board) {
        Piece[][] copyContents = new
                Piece[Square.BOARD_SIZE][Square.BOARD_SIZE];
        int count = 0;
        for (int i = 0; i < Square.BOARD_SIZE; i++) {
            for (int j = 0; j < Square.BOARD_SIZE; j++) {
                copyContents[i][j] = board.getBoard()[count];
                count++;
            }
        }
        initialize(copyContents, board.turn());
    }

    /**
     * Return _board.
     */
    Piece[] getBoard() {
        return _board;
    }

    /**
     * Return the contents of the square at SQ.
     */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /**
     * Set the square at SQ to V and set the side that is to move next
     * to NEXT, if NEXT is not null.
     */
    void set(Square sq, Piece v, Piece next) {
        if (next != null) {
            _turn = next;
        }
        this._board[sq.index()] = v;
    }

    /**
     * Set the square at SQ to V, without modifying the side that
     * moves next.
     */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /**
     * Set limit on number of moves (before tie results) to LIMIT.
     */
    /**
     * Set limit on number of moves by each side that results in a tie to
     * LIMIT, where 2 * LIMIT > movesMade().
     */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /**
     * Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture()
     * is false.
     */
    /**
     * Assuming isLegal(MOVE), make MOVE. This function assumes that
     * MOVE.isCapture() will return false.  If it saves the move for
     * later retraction, makeMove itself uses MOVE.captureMove() to produce
     * the capturing move.
     */
    void makeMove(Move move) {
        assert isLegal(move);
        if (this._board[move.getTo().index()].equals(_turn.opposite())) {
            move = move.captureMove();
        }
        _moves.add(move);
        set(move.getTo(), this._board[move.getFrom().index()]);
        set(move.getFrom(), EMP);
        if (_turn.equals(BP)) {
            _bMoveCount += 1;
        } else if (_turn.equals(WP)) {
            _wMoveCount += 1;
        }
        _turn = _turn.opposite();
        _winnerKnown = false;
        _subsetsInitialized = false;
    }

    /**
     * Retract (unmake) one move, returning to the state immediately before
     * that move.  Requires that movesMade () > 0.
     */
    void retract() {
        assert movesMade() > 0;
        if (_winner != null || _winnerKnown) {
            _winnerKnown = false;
            _winner = null;
        }
        Move move = _moves.get(_moves.size() - 1);
        set(move.getFrom(), this._board[move.getTo().index()]);
        if (move.isCapture()) {
            set(move.getTo(), _turn);
        } else {
            set(move.getTo(), EMP);
        }
        _turn = _turn.opposite();
        if (_turn.equals(BP)) {
            _bMoveCount -= 1;
        } else if (_turn.equals(WP)) {
            _wMoveCount -= 1;
        }
        _moves.remove(_moves.size() - 1);
    }

    /**
     * Return the Piece representing who is next to move.
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return true iff FROM - TO is a legal move for the player currently on
     * move.
     */
    boolean isLegal(Square from, Square to) {
        if ((!exists(from.col(), from.row())) || (!exists(to.col(), to.row()))
                || (!from.isValidMove(to))
                || (this._board[to.index()].equals(_turn))) {
            return false;
        }
        int numPieces = 1 + countForward(from, from.direction(to))
                + countForward(from, to.direction(from));
        if ((from.distance(to) != numPieces) || blocked(from, to)) {
            return false;
        }
        return true;
    }

    /**
     * Return number of pieces in DIR, starting from FROM.
     */
    private int countForward(Square from, int dir) {
        int numPieces = 0;
        int steps = 1;
        Square sq = from.moveDest(dir, steps);
        while (sq != null) {
            if (!this._board[sq.index()].equals(EMP)) {
                numPieces += 1;
            }
            steps += 1;
            sq = from.moveDest(dir, steps);
            if (sq == null) {
                break;
            }
        }
        return numPieces;
    }

    /**
     * Return true if a move from FROM to TO is blocked by an opposing
     * piece or by a friendly piece on the target square.
     */
    private boolean blocked(Square from, Square to) {
        int steps = 1;
        Square sq = from.moveDest(from.direction(to), steps);
        while (sq != null) {
            if (sq.index() == to.index()) {
                break;
            }
            if (this._board[sq.index()].equals(_turn.opposite())) {
                return true;
            }
            steps += 1;
            sq = from.moveDest(from.direction(to), steps);
        }
        return false;
    }

    /**
     * Return true iff MOVE is legal for the player currently on move.
     * The isCapture() property is ignored.
     */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /**
     * Return a sequence of all legal moves from this position.
     */
    List<Move> legalMoves() {
        ArrayList<Move> result = new ArrayList<Move>();
        for (int h = 0; h < ALL_SQUARES.length; h++) {
            if (this._board[ALL_SQUARES[h].index()].equals(turn())) {
                for (int j = 0; j < ALL_SQUARES.length; j++) {
                    Move m = Move.mv(ALL_SQUARES[h], ALL_SQUARES[j]);
                    if (isLegal(ALL_SQUARES[h], ALL_SQUARES[j])) {
                        result.add(m);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Return true iff the game is over (either player has all his
     * pieces continguous or there is a tie).
     */
    boolean gameOver() {
        return winner() != null;
    }

    /**
     * Return true iff SIDE's pieces are continguous.
     */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /**
     * Return the winning side, if any.  If the game is not over, result is
     * null.  If the game has ended in a tie, returns EMP.
     */
    Piece winner() {
        if (!_winnerKnown) {
            if (piecesContiguous(BP) && piecesContiguous(WP)) {
                _winner = _turn.opposite();
            } else if (piecesContiguous(BP)) {
                _winner = BP;
            } else if (piecesContiguous(WP)) {
                _winner = WP;
            } else if (_moves.size() >= _moveLimit) {
                _winner = EMP;
            }
            _winnerKnown = true;
        }
        return _winner;
    }

    /**
     * Return the total number of moves that have been made (and not
     * retracted).  Each valid call to makeMove with a normal move increases
     * this number by 1.
     */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }


    /**
     * Return the size of the as-yet unvisited cluster of squares
     * containing P at and adjacent to SQ.  VISITED indicates squares that
     * have already been processed or are in different clusters.  Update
     * VISITED to reflect squares counted.
     */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (p.equals(EMP)) {
            return 0;
        }
        if (!this._board[sq.index()].equals(p)) {
            return 0;
        }
        if (visited[sq.row()][sq.col()]) {
            return 0;
        }
        visited[sq.row()][sq.col()] = true;
        int count = 1;
        for (Square s : sq.adjacent()) {
            count += numContig(s, visited, p);
        }
        return count;
    }

    /**
     * Set the values of _whiteRegionSizes and _blackRegionSizes.
     */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < ALL_SQUARES.length; i++) {
            if (_board[ALL_SQUARES[i].index()].equals(WP)) {
                int numW = numContig(ALL_SQUARES[i], visited,
                        _board[ALL_SQUARES[i].index()]);
                if (numW != 0) {
                    _whiteRegionSizes.add(numW);
                } else {
                    continue;
                }
            } else if (_board[ALL_SQUARES[i].index()].equals(BP)) {
                int numB = numContig(ALL_SQUARES[i], visited,
                        _board[ALL_SQUARES[i].index()]);
                if (numB != 0) {
                    _blackRegionSizes.add(numB);
                } else {
                    continue;
                }
            }
        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /**
     * Return the sizes of all the regions in the current union-find
     * structure for side S.
     */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /**
     * Sets _subsetsInitialized to TEMP.
     */
    public void setSubsetsInitialized(boolean temp) {
        _subsetsInitialized = temp;
    }

    /**
     * Return _moves.
     */
    public ArrayList<Move> getMoves() {
        return _moves;
    }

    /**
     * clears _moves.
     */
    public void clearMoves() {
        _moves.clear();
    }

    /**
     * The standard initial configuration for Lines of Action (bottom row
     * first).
     */
    static final Piece[][] INITIAL_PIECES = {
            {EMP, BP, BP, BP, BP, BP, BP, EMP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {EMP, BP, BP, BP, BP, BP, BP, EMP}
    };

    /**
     * Current contents of the board.  Square S is at _board[S.index()].
     */
    private final Piece[] _board = new Piece[BOARD_SIZE * BOARD_SIZE];

    /**
     * List of all unretracted moves on this board, in order.
     */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /**
     * Current side on move.
     */
    private Piece _turn;
    /**
     * Limit on number of moves before tie is declared.
     */
    private int _moveLimit;
    /**
     * True iff the value of _winner is known to be valid.
     */
    private boolean _winnerKnown;
    /**
     * Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     * in progress).  Use only if _winnerKnown.
     */
    private Piece _winner;

    /**
     * True iff subsets computation is up-to-date.
     */
    private boolean _subsetsInitialized;

    /**
     * List of the sizes of continguous clusters of pieces, by color.
     */
    private final ArrayList<Integer>
            _whiteRegionSizes = new ArrayList<>(),
            _blackRegionSizes = new ArrayList<>();

    /**
     * Number of black moves.
     */
    private int _bMoveCount;
    /**
     * Number of white moves.
     */
    private int _wMoveCount;
}
