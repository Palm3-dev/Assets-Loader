package com.palm3.assets_loader;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PrettyLogging {

    public final Logger LOGGER;

    public static final String DEF_LINE = "===================================================================================";  // 83 chars
    public static final String DEF_EMPTY_LINE = "                                                                                   ";  // 83 chars

    public final String line1;
    public final String line2;
    public final String emptyLine1;
    public final String emptyLine2;

    /**
     * Creates the class instance, with 2 custom lines you can create.
     * @param logger The {@link Logger} instance.
     * @param line1Char The character that the {@code line1} will be composed of.
     * @param line1Length The length of the {@code line1}.
     * @param line2Char The character that the {@code line2} will be composed of.
     * @param line2Length The length of the {@code line2}.
     */
    public PrettyLogging(Logger logger, String line1Char, int line1Length, String line2Char, int line2Length) {
        LOGGER = logger;
        line1 = line1Char.repeat(line1Length);
        line2 = line2Char.repeat(line2Length);
        emptyLine1 = line1.replace(line1Char, " ");
        emptyLine2 = line2.replace(line2Char, " ");
    }

    /**
     * Creates the class instance from default params using another logger instance.
     * @param defaultPrettyLoggingParams The instance of the default class.
     */
    public PrettyLogging(Logger logger, DefaultPrettyLoggingParams defaultPrettyLoggingParams) {
        LOGGER = logger;
        line1 = defaultPrettyLoggingParams.line1;
        line2 = defaultPrettyLoggingParams.line2;
        emptyLine1 = defaultPrettyLoggingParams.emptyLine1;
        emptyLine2 = defaultPrettyLoggingParams.emptyLine2;
    }

    /**
     * Creates the class instance from default params, same logger instance.
     * Will log with the class name of the original logger instance.
     * @param defaultPrettyLoggingParams The instance of the default class.
     */
    public PrettyLogging(DefaultPrettyLoggingParams defaultPrettyLoggingParams) {
        LOGGER = defaultPrettyLoggingParams.logger;
        line1 = defaultPrettyLoggingParams.line1;
        line2 = defaultPrettyLoggingParams.line2;
        emptyLine1 = defaultPrettyLoggingParams.emptyLine1;
        emptyLine2 = defaultPrettyLoggingParams.emptyLine2;
    }

    /**
     * Indicates the position of the log messages.
     */
    public enum LogPos {
        BEFORE,
        AFTER,
        BOTH;

        LogPos() {}
    }

    /**
     * Logs the default line of =.
     * @param msg The message to log.
     * @param logPos The position of the line relative to the log message.
     */
    public void logLineI(String msg, LogPos logPos) {
        if (logPos == LogPos.BEFORE || logPos == LogPos.BOTH)
            LOGGER.info(DEF_LINE);
        if (!msg.isEmpty()) LoaderMain.LOGGER.info(msg);
        if (logPos == LogPos.AFTER || logPos == LogPos.BOTH)
            LOGGER.info(DEF_LINE);
    }

    /**
     * Logs the default line (empty or of =).
     */
    public void logLineI(boolean empty) {
        if (!empty) LOGGER.info(DEF_LINE);
        else LOGGER.info(DEF_EMPTY_LINE);
    }

    /**
     * Simply logs an info message.
     * @param msg The message to log.
     */
    public void logI(String msg) {
        LOGGER.info(msg);
    }

    /**
     * Simply logs an error message.
     * @param msg The message to log.
     */
    public void logE(String msg) {
        LOGGER.error(msg);
    }

    /**
     * Simply logs a warn message.
     * @param msg The message to log.
     */
    public void logW(String msg) {
        LOGGER.warn(msg);
    }

    /**
     * Simply logs a fatal error message.
     * @param msg The message to log.
     */
    public void logF(String msg) {
        LOGGER.error(LogUtils.FATAL_MARKER, msg);
    }

    /**
     * Logs the given message with the given spaces.
     * @param msg The message to log.
     * @param spacesNumber The number of spaces to apply.
     * @param logPos The position relative to the message in which to apply the spaces (before, after, both).
     */
    public void logI(String msg, int spacesNumber, LogPos logPos) {
        if (logPos == LogPos.BEFORE || logPos == LogPos.BOTH) logSpacesI(spacesNumber);
        LOGGER.info(msg);
        if (logPos == LogPos.AFTER || logPos == LogPos.BOTH) logSpacesI(spacesNumber);
    }

    /**
     * Logs a one line space.
     */
    public void logSpaceI() {
        LOGGER.info("");
    }

    /**
     * Logs the given number of spaces (empty lines).
     * @param spacesNumber The number of spaces that should be logged. Every value under 1 will be capped at one.
     */
    public void logSpacesI(int spacesNumber) {
        if (spacesNumber <= 0) spacesNumber = 1;
        for (int i = 0; i < spacesNumber; i++) LOGGER.info("");
    }

    /**
     * Return the {@code string} centered relative to the given {@code relativeTo} value.
     * @param string The string to center.
     * @param relativeTo The string to center the given string to.
     * @param keepSameLength If the length of the string should remain the same as the relative.
     * @param applySpaces If spaces should be applied before and after the string once centered with the relative.
     * @return The centered {@link String} relative to the given one.
     */
    public static String centerString(String string, String relativeTo, boolean keepSameLength, boolean applySpaces) {
        int relativeToLength = relativeTo.length();
        int msgLength = string.length();

        String beforeMsg = relativeTo.substring(0, relativeToLength / 2);
        String afterMsg = relativeTo.substring(relativeToLength / 2);

        if (keepSameLength) {
            int additionalSpace = applySpaces ? 1 : 0;
            beforeMsg = beforeMsg.substring(msgLength / 2 + additionalSpace);
            afterMsg = afterMsg.substring(msgLength / 2 + additionalSpace);
        }

        String centeredMsg = "";
        StringBuilder builder = new StringBuilder(centeredMsg);

        builder.append(beforeMsg);
        if (applySpaces) builder.append(" ");
        builder.append(string);
        if (applySpaces) builder.append(" ");
        builder.append(afterMsg);

        int currentMsgLength = builder.toString().length();

        if (currentMsgLength > relativeToLength && keepSameLength) {
            boolean first = true;
            for (int i = 0; i < currentMsgLength - relativeToLength; i++) {
                if (first) builder.delete(0, 1);
                else builder.delete(currentMsgLength, currentMsgLength + 1);
                currentMsgLength = builder.toString().length();
                first = !first;
            }
        }

        return builder.toString();
    }

    /**
     * Logs the given message centered relative to the given string.
     * @param msg The message to center.
     * @param relativeTo The message to center the given string to.
     * @param keepSameLength If the length of the message should remain the same as the relative.
     * @param applySpaces If spaces should be applied before and after your message once centered with the relative.
     */
    public void logCenteredI(String msg, String relativeTo, boolean keepSameLength, boolean applySpaces) {
        LOGGER.info(centerString(msg, relativeTo, keepSameLength, applySpaces));
    }

    /**
     * Logs infos if the condition is true.
     * @param condition The condition expression, logs if true.
     * @param msg The message of the log.
     */
    public void conditionalI(boolean condition, String msg) {
        if (condition) LOGGER.info(msg);
    }

    /**
     * Used to define default params for a {@link PrettyLogging} instance to use in other instances.
     */
    @ParametersAreNonnullByDefault
    public static class DefaultPrettyLoggingParams {

        public final Logger logger;
        public final String line1;
        public final String line2;
        public final String emptyLine1;
        public final String emptyLine2;

        /**
         * Used to define default params for a {@link PrettyLogging} instance to use in other instances.
         * Parameters are the same as {@link PrettyLogging}.
         */
        public DefaultPrettyLoggingParams(Logger logger, String line1Char, int line1Length, String line2Char, int line2Length) {
            this.logger = logger;
            line1 = line1Char.repeat(line1Length);
            line2 = line2Char.repeat(line2Length);
            emptyLine1 = line1.replace(line1Char, " ");
            emptyLine2 = line2.replace(line2Char, " ");
        }
    }
}
