package snakevssnake.util;

import java.util.regex.Pattern;

/** Validation of the text fields on the menu. */
public final class InputValidator {

    public static final int MAX_NAME_LENGTH = 12;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9 _\\-]+");

    private InputValidator() { }

    /**
     * Cleans and validates a player name.
     *
     * @param label used in error messages, e.g. "Player 1"
     * @return the trimmed name with repeated spaces collapsed
     */
    public static String validateName(String raw, String label) throws InvalidInputException {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new InvalidInputException(label + " name cannot be empty.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException(label + " name can have at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!ALLOWED.matcher(name).matches()) {
            throw new InvalidInputException(label + " name may only contain letters, digits, spaces, '_' and '-'.");
        }
        return name;
    }

    public static void requireDifferent(String a, String b) throws InvalidInputException {
        if (a.equalsIgnoreCase(b)) {
            throw new InvalidInputException("The two players need different names.");
        }
    }
}
