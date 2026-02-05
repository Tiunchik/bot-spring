package org.bot.spring.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ProcessCommand {
    String videoUrl;
    String videoId;
    String folderPath;
    String fileName;

    public List<String> getListCommands() {
        List<String> list = new ArrayList<>();
        list.add("yt-dlp");
        if (videoId != null) {
            list.add("-f");
            list.add(videoId);
        }
        if (folderPath != null) {
            list.add("-P");
            list.add(folderPath);
        }
        if (fileName != null) {
            list.add("-o");
            list.add(fileName);
        }
        list.add(videoUrl);

        return list;
    }

    public String getOutputPath() {
        return folderPath + "/" + fileName;
    }
}
