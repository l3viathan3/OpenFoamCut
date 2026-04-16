package de.kremerdaniel.openfoamcut.gui;

/**
 * Listener for MoveOutlinePanel
 */
public interface MoveOutlineActionListener {

    /**
     * Called whenever a user action triggered an outline move/offset
     * @param action The MoveOutlineAction that triggered this call
     * @param panel The panel that initiated the action call
     */
    default void actionPerformed(MoveOutlineAction action, MoveOutlinePanel panel) {
        // NOP - Intentionally default to doing nothing.
    }

    /**
     * Indicating user action that triggered the MoveOutlineActionListener
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    enum MoveOutlineAction {
        ALIGN_ABSOLUTE_LEFT,
        ALIGN_OTHER_LEFT,
        ALIGN_OTHER_H_CENTER,
        ALIGN_ABSOLUTE_H_CENTER,
        ALIGN_OTHER_RIGHT,
        ALIGN_ABSOLUTE_RIGHT,
        ALIGN_ABSOLUTE_TOP,
        ALIGN_OTHER_TOP,
        ALIGN_OTHER_V_CENTER,
        ALIGN_ABSOLUTE_V_CENTER,
        ALIGN_OTHER_BOTTOM,
        ALIGN_ABSOLUTE_BOTTOM,

        OFFSET_MANUALLY_CHANGED
    }
    
}
