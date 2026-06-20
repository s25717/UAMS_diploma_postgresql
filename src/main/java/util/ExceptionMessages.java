package util;

import java.sql.SQLException;

public final class ExceptionMessages {
    private ExceptionMessages() {
    }

    public static String userMessage(Throwable throwable) {
        SQLException sqlException = findSqlException(throwable);
        if (sqlException != null) {
            SQLException next = sqlException.getNextException();
            return clean(next == null ? sqlException.getMessage() : next.getMessage());
        }

        Throwable root = rootCause(throwable);
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = root.getClass().getSimpleName();
        }
        return clean(message);
    }

    private static SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String clean(String message) {
        if (message == null || message.isBlank()) {
            return "Operation failed.";
        }
        String cleaned = message.replace("\r\n", "\n").trim();
        if (cleaned.startsWith("ERROR: ")) {
            cleaned = cleaned.substring("ERROR: ".length());
        }
        int detailIndex = cleaned.indexOf("\n  Detail:");
        if (detailIndex >= 0) {
            String detail = cleaned.substring(detailIndex + "\n  Detail:".length()).trim();
            int nextLine = detail.indexOf('\n');
            detail = nextLine >= 0 ? detail.substring(0, nextLine).trim() : detail;
            cleaned = cleaned.substring(0, detailIndex).trim() + " (" + detail + ")";
        }
        int whereIndex = cleaned.indexOf("\n  Where:");
        if (whereIndex >= 0) {
            cleaned = cleaned.substring(0, whereIndex).trim();
        }
        return cleaned;
    }
}
