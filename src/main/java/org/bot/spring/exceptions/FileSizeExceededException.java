package org.bot.spring.exceptions;

import java.math.BigDecimal;

public class FileSizeExceededException extends RuntimeException {

    private final BigDecimal fileSizeInMB;
    private final BigDecimal maxFileSizeInMB;

    public FileSizeExceededException(BigDecimal fileSizeInMB, BigDecimal maxFileSizeInMB) {
        super(String.format("Размер файла (%.2f MB) превышает максимально допустимый (%.2f MB).", fileSizeInMB, maxFileSizeInMB));
        this.fileSizeInMB = fileSizeInMB;
        this.maxFileSizeInMB = maxFileSizeInMB;
    }

    public BigDecimal getFileSizeInMB() {
        return fileSizeInMB;
    }

    public BigDecimal getMaxFileSizeInMB() {
        return maxFileSizeInMB;
    }
}