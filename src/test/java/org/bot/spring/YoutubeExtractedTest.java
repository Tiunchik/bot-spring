package org.bot.spring;

import org.bot.spring.configuration.properties.DownloadProperties;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.YtDlpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
class YoutubeExtractedTest {

    private List<String> lines = List.of(
            "sb2     mhtml 25x45        1    │                   mhtml │ images                                   storyboard",
            "sb3     mhtml 48x27        2    │                   mhtml │ images                                   storyboard",
            "sb1     mhtml 50x90        1    │                   mhtml │ images                                   storyboard",
            "sb0     mhtml 101x180      1    │                   mhtml │ images                                   storyboard",
            "139-drc m4a   audio only      2 │  246.29KiB    49k https │ audio only           mp4a.40.5   49k 22k [ru] Russian original, low, DRC, m4a_dash",
            "249-drc webm  audio only      2 │  282.85KiB    56k https │ audio only           opus        56k 48k [ru] Russian original, low, DRC, webm_dash",
            "139-0   m4a   audio only      2 │  246.61KiB    49k https │ audio only           mp4a.40.5   49k 22k [en-US] English (US) (default), low, m4a_dash",
            "139-1   m4a   audio only      2 │  246.30KiB    49k https │ audio only           mp4a.40.5   49k 22k [ru] Russian original, low, m4a_dash",
            "249-0   webm  audio only      2 │  291.16KiB    58k https │ audio only           opus        58k 48k [en-US] English (US) (default), low, webm_dash",
            "249-1   webm  audio only      2 │  282.58KiB    56k https │ audio only           opus        56k 48k [ru] Russian original, low, webm_dash",
            "140-drc m4a   audio only      2 │  650.76KiB   130k https │ audio only           mp4a.40.2  130k 44k [ru] Russian original, medium, DRC, m4a_dash",
            "251-drc webm  audio only      2 │  714.89KiB   142k https │ audio only           opus       142k 48k [ru] Russian original, medium, DRC, webm_dash",
            "140-0   m4a   audio only      2 │  651.95KiB   130k https │ audio only           mp4a.40.2  130k 44k [en-US] English (US) (default), medium, m4a_dash",
            "140-1   m4a   audio only      2 │  650.76KiB   130k https │ audio only           mp4a.40.2  130k 44k [ru] Russian original, medium, m4a_dash",
            "251-0   webm  audio only      2 │  718.56KiB   143k https │ audio only           opus       143k 48k [en-US] English (US) (default), medium, webm_dash",
            "251-1   webm  audio only      2 │  714.00KiB   142k https │ audio only           opus       142k 48k [ru] Russian original, medium, webm_dash",
            "160     mp4   144x256     30    │  962.88KiB   62k https │ avc1.4d400c     62k video only          144p, mp4_dash",
            "133     mp4   240x426     30    │    1.79MiB  118k https │ avc1.4d4015    118k video only          240p, mp4_dash",
            "242     webm  240x426     30    │    1.28MiB   84k https │ vp9             84k video only          240p, webm_dash",
            "395     mp4   240x426     30    │    1.21MiB   79k https │ av01.0.00M.08   79k video only          240p, mp4_dash",
            "134     mp4   360x640     30    │    3.78MiB  249k https │ avc1.4d401e    249k video only          360p, mp4_dash",
            "18      mp4   360x640     30  2 │    9.05MiB  595k https │ avc1.42001E         mp4a.40.2       48k [en] 360p",
            "243     webm  360x640     30    │    2.34MiB  154k https │ vp9            154k video only          360p, webm_dash",
            "396     mp4   360x640     30    │    3.28MiB  216k https │ av01.0.01M.08  216k video only          360p, mp4_dash",
            "135     mp4   480x854     30    │    7.15MiB  470k https │ avc1.4d401f    470k video only          480p, mp4_dash",
            "397     mp4   480x854     30    │    4.94MiB  325k https │ av01.0.04M.08  325k video only          480p, mp4_dash",
            "779     webm  608x1080    30    │    5.14MiB  338k https │ vp9            338k video only          480p, webm_dash",
            "780     webm  608x1080    30    │    8.20MiB  539k https │ vp9            539k video only          480p, webm_dash",
            "788     mp4   608x1080    30    │    4.31MiB  283k https │ av01.0.04M.08  283k video only          480p, mp4_dash",
            "136     mp4   720x1280    30    │   12.22MiB  804k https │ avc1.4d401f    804k video only          720p, mp4_dash",
            "247     webm  720x1280    30    │   13.55MiB  892k https │ vp9            892k video only          720p, webm_dash",
            "398     mp4   720x1280    30    │    7.98MiB  525k https │ av01.0.05M.08  525k video only          720p, mp4_dash",
            "137     mp4   1080x1920   30    │   27.79MiB 1829k https │ avc1.640028   1829k video only          1080p, mp4_dash",
            "248     webm  1080x1920   30    │   14.87MiB  979k https │ vp9            979k video only          1080p, webm_dash",
            "399     mp4   1080x1920   30    │   11.58MiB  762k https │ av01.0.08M.08  762k video only          1080p, mp4_dash"
    );

    @Test
    void youtubeExtractedTest() {
        DownloadProperties properties = new DownloadProperties();
        properties.setDownloadPath("/");
        properties.setMaxFileSizeMB(BigDecimal.TWO);
        YtDlpService service = new YtDlpService(properties);
        var result = new ArrayList<VideoFormatDto>();
        lines.forEach(it -> service.extracted(it, result));
        Assertions.assertTrue(result.size() > 0);
    }

}
