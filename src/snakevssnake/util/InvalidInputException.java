package snakevssnake.util;

/** Thrown when the user types something the game cannot accept (for example an empty name). */
public class InvalidInputException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
