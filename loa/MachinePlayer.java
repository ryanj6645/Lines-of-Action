/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;

import static loa.Piece.*;

/**
 * An automated Player.
 *
 * @author Ryan Johnson
 */
class MachinePlayer extends Player {

    /**
     * A position-score magnitude indicating a win (for white if positive,
     * black if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new MachinePlayer with no piece or controller (intended to produce
     * a template).
     */
    MachinePlayer() {
        this(null, null);
    }

    /**
     * A MachinePlayer that plays the SIDE pieces in GAME.
     */
    MachinePlayer(Piece side, Game game) {
        super(side, game);
    }

    @Override
    String getMove() {
        Move choice;

        assert side() == getGame().getBoard().turn();
        int depth;
        choice = searchForMove();
        getGame().reportMove(choice);
        return choice.toString();
    }

    @Override
    Player create(Piece piece, Game game) {
        return new MachinePlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return false;
    }

    /**
     * Return a move after searching the game tree to DEPTH>0 moves
     * from the current position. Assumes the game is not over.
     */
    private Move searchForMove() {
        Board work = new Board(getBoard());
        int value;
        assert side() == work.turn();
        _foundMove = null;
        if (side() == WP) {
            value = findMove(work, chooseDepth(), true, 1, -INFTY, INFTY);
        } else {
            value = findMove(work, chooseDepth(), true, -1, -INFTY, INFTY);
        }
        return _foundMove;
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _foundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _foundMove. If the game is over
     * on BOARD, does not set _foundMove.
     */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (depth == 0) {
            return heuristics(board);
        }
        int bestScore = -INFTY;
        for (Move move : board.legalMoves()) {
            board.makeMove(move);
            int score = findMove(board, depth - 1,
                    false, sense * -1, alpha, beta);
            board.setSubsetsInitialized(false);
            if (board.piecesContiguous(board.turn()) && board.gameOver()) {
                score = -WINNING_VALUE - depth;
            } else if (board.piecesContiguous(
                    board.turn().opposite()) && board.gameOver()) {
                score = WINNING_VALUE + depth;
            }
            board.retract();
            if (score > bestScore) {
                bestScore = score;
                if (saveMove) {
                    _foundMove = move;
                }
            }
            if (sense == 1) {
                alpha = java.lang.Integer.max(score, alpha);
            } else {
                beta = java.lang.Integer.min(score, alpha);
            }
            if (alpha >= beta) {
                break;
            }
        }
        return bestScore;
    }

    /**
     * Returns heuristics from BOARD.
     */
    private int heuristics(Board board) {
        if (board.piecesContiguous(board.turn())
                && board.gameOver()) {
            return -WINNING_VALUE;
        } else if (board.piecesContiguous(
                board.turn().opposite()) && board.gameOver()) {
            return WINNING_VALUE;
        }
        ArrayList<Integer> list =
                (ArrayList<Integer>) board.getRegionSizes(board.turn());
        ArrayList<Integer> listOpp = (ArrayList<Integer>)
                board.getRegionSizes(board.turn().opposite());
        int num = (int) (Math.random() * 100);
        int total = 0;
        int max = 0;
        for (int elem : list) {
            if (elem > max) {
                max = elem;
                total += elem;
            }
        }
        int totalOpp = 0;
        int maxOpp = 0;
        for (int elem : listOpp) {
            if (elem > maxOpp) {
                maxOpp = elem;
                totalOpp += elem;
            }
        }
        int temp = max / total - maxOpp / totalOpp;
        return num;
    }

    /**
     * Return a search depth for the current position.
     */
    private int chooseDepth() {
        return 3;
    }

    /**
     * Used to convey moves discovered by findMove.
     */
    private Move _foundMove;

}
