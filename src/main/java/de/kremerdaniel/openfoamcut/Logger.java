package de.kremerdaniel.openfoamcut;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight console logger.
 */
public class Logger {

    // Log info and above by default, except for the ArchUnit processor logger which is set to ERROR to suppress its info logs
    private static final Level ROOT_LEVEL = Level.INFO;
    private static final String ARCHUNIT_PROCESSOR_LOGGER = "com.tngtech.archunit.core.importer.ClassFileProcessor";

    // Format is "HH:mm:ss.SSS [thread-name] LEVEL LoggerName - message"
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object OUTPUT_LOCK = new Object();

    private final String loggerName;
    private final Level loggerLevel;

    /**
     * Creates a logger for the given class.
     *
     * @param clazz owning class
     */
    public Logger(Class<?> clazz) {
        this(clazz.getName());
    }

    Logger(String loggerName) {
        this.loggerName = abbreviateLoggerName(loggerName);
        this.loggerLevel = resolveLevel(loggerName);
    }

    /**
     * Logs an INFO message without placeholders.
     *
     * @param string log message
     */
    public void info(String string) {
        log(Level.INFO, string, new Object[0]);
    }

    /**
     * Logs an INFO message with SLF4J-style placeholders.
     *
     * @param string log message
     * @param args placeholder values
     */
    public void info(String string, Object ... args) {
        log(Level.INFO, string, args);
    }

    /**
     * Logs a WARN message with SLF4J-style placeholders.
     *
     * @param string log message
     * @param args placeholder values or trailing throwable
     */
    public void warn(String string, Object ... args) {
        log(Level.WARN, string, args);
    }

    /**
     * Logs an ERROR message with a throwable.
     *
     * @param string log message
     * @param throwable exception to print
     */
    public void error(String string, Throwable throwable) {
        log(Level.ERROR, string, new Object[] {throwable});
    }

    /**
     * Logs a DEBUG message.
     *
     * @param string log message
     */
    public void debug(String string) {
        log(Level.DEBUG, string, new Object[0]);
    }

    @SuppressWarnings("PMD.SystemPrintln")
    private void log(Level level, String message, Object... args) {
        if (!isEnabled(level)) {
            return;
        }

        Throwable throwable = extractThrowable(message, args);
        String renderedMessage = formatMessage(message, args, throwable);

        synchronized (OUTPUT_LOCK) {
            System.out.println(formatPrefix(level) + renderedMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    private boolean isEnabled(Level level) {
        return level.priority() >= loggerLevel.priority();
    }

    private String formatPrefix(Level level) {
        String time = LocalTime.now().format(TIME_FORMATTER);
        String threadName = Thread.currentThread().getName();
        return String.format("%s [%s] %-5s %s - ", time, threadName, level.label(), loggerName);
    }

    private String formatMessage(String message, Object[] args, Throwable throwable) {
        if (args.length == 0) {
            return message;
        }

        int placeholders = countPlaceholders(message);
        int argsToRender = Math.min(placeholders, args.length);
        if (throwable != null && args.length > 0 && throwable.equals(args[args.length - 1]) && placeholders < args.length) {
            argsToRender = Math.min(placeholders, args.length - 1);
        }

        StringBuilder builder = new StringBuilder();
        int searchIndex = 0;
        int renderedArgs = 0;

        while (true) {
            int placeholderIndex = message.indexOf("{}", searchIndex);
            if (placeholderIndex < 0) {
                builder.append(message.substring(searchIndex));
                break;
            }

            builder.append(message, searchIndex, placeholderIndex);
            if (renderedArgs < argsToRender) {
                Object argument = args[renderedArgs];
                builder.append(String.valueOf(argument));
                renderedArgs++;
            } else {
                builder.append("{}");
            }
            searchIndex = placeholderIndex + 2;
        }

        return builder.toString();
    }

    private Throwable extractThrowable(String message, Object[] args) {
        if (args.length == 0 || !(args[args.length - 1] instanceof Throwable throwable)) {
            return null;
        }

        int placeholders = countPlaceholders(message);
        if (placeholders < args.length) {
            return throwable;
        }

        return null;
    }

    private int countPlaceholders(String message) {
        int count = 0;
        int searchIndex = 0;
        while (true) {
            int placeholderIndex = message.indexOf("{}", searchIndex);
            if (placeholderIndex < 0) {
                return count;
            }
            count++;
            searchIndex = placeholderIndex + 2;
        }
    }

    private static String abbreviateLoggerName(String fullLoggerName) {
        int separatorIndex = fullLoggerName.lastIndexOf('.');
        if (separatorIndex >= 0 && separatorIndex < fullLoggerName.length() - 1) {
            return fullLoggerName.substring(separatorIndex + 1);
        }
        return fullLoggerName;
    }

    private static Level resolveLevel(String fullLoggerName) {
        if (ARCHUNIT_PROCESSOR_LOGGER.equals(fullLoggerName)) {
            return Level.ERROR;
        }
        return ROOT_LEVEL;
    }

    private enum Level {
        DEBUG("DEBUG", 10),
        INFO("INFO", 20),
        WARN("WARN", 30),
        ERROR("ERROR", 40);

        private final String label;
        private final int priority;

        Level(String label, int priority) {
            this.label = label;
            this.priority = priority;
        }

        private String label() {
            return label;
        }

        private int priority() {
            return priority;
        }
    }
}
