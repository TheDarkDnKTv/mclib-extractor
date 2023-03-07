package thedarkdnktv.mclibextractor.exception;

public class LaunchException extends Exception {

    public static final int EXIT_CODE_UNEXPECTED = 100;

    private int exitCode;

    public LaunchException(String message, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public LaunchException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public LaunchException(String message, Throwable cause) {
        this(message, EXIT_CODE_UNEXPECTED, cause);
    }

    public LaunchException(Throwable cause) {
        super(cause);
        this.exitCode = EXIT_CODE_UNEXPECTED;
    }

    public int getExitCode() {
        return exitCode;
    }
}
