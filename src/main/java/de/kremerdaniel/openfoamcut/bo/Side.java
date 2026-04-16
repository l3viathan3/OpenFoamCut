package de.kremerdaniel.openfoamcut.bo;

/**
 * Left / right
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum Side {
    LEFT, RIGHT;

    /**
     * Gives the other side compared to this one
     * @return Left if called from right and right if called from left
     */
    public Side other() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
