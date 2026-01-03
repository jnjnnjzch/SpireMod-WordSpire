package WordSpire.helpers;

import java.util.Arrays;
import java.util.List;

public class DownloadConfig {
    
    public static class DownloadPreset {
        public String name;       // 显示名称
        public String fileName;   // 保存的文件名
        public String url;        // 下载地址
        public String configRaw;  // 预设配置

        public DownloadPreset(String name, String fileName, String url, String configRaw) {
            this.name = name;
            this.fileName = fileName;
            this.url = url;
            this.configRaw = configRaw;
        }
    }

    // 这里集中管理预设列表，以后要改下载地址或者加新词库，只改这里即可
    public static final List<DownloadPreset> PRESETS = Arrays.asList(
        // 示例：
        new DownloadPreset("JLPT-10k",  "JLPT10k.apkg", "https://www.ghproxy.cn/https://github.com/5mdld/anki-jlpt-decks/releases/latest/download/eggrolls-JLPT10k-v3.apkg",   "1|4,5,7|8")
    );
}