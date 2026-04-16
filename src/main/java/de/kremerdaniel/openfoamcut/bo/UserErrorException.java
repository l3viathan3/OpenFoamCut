package de.kremerdaniel.openfoamcut.bo;

/**
 * An exeption that is user-facing.
 */
public class UserErrorException extends RuntimeException {

    /**
     * Indicates that someone (most probably the user) made an error
     * @param string Message to display to the user
     * @param e The throwable triggering this error, if any. Otherwise, null
     */
    public UserErrorException(String string, Throwable e) {
        super(string, e);
    }
    
}
