package org.bot.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFormatDto {
    private String id;
    private String container;
    private String resolution;
    private BigDecimal fileSizeInMB;
}
