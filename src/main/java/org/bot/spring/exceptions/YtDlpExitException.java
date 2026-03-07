package org.bot.spring.exceptions;

public class YtDlpExitException extends RuntimeException {
    int exitCode;
    
    public YtDlpExitException(int exitCode, String ytDlpLog) {
        this.exitCode = exitCode;
        super(ytDlpLog);
    }
    
    @Override public String toString() {
        return "yt-dlp завершился с кодом " + exitCode;
    }
}
