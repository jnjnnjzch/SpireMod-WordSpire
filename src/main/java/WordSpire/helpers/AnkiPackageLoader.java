package WordSpire.helpers;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnkiPackageLoader {

    public static class AnkiRawCard {
        public String front; // 题面
        public List<String> backList = new ArrayList<>(); // 多个答案字段
        public String audio; // 音频文件名
    }

    /**
     * @param apkgFile .apkg 文件对象
     * @param audioOutputDir 音频解压目录
     * @param config 游戏的 SpireConfig 对象，用于读取用户映射配置
     */
    public static List<AnkiRawCard> loadFromApkg(File apkgFile, File audioOutputDir, SpireConfig config) {
        List<AnkiRawCard> resultList = new ArrayList<>();
        File tempDbFile = null;

        if (!audioOutputDir.exists()) {
            audioOutputDir.mkdirs();
        }

        // 默认映射配置
        int qIndex = 0; // 默认题面为第0列
        List<Integer> aIndices = new ArrayList<>(Collections.singletonList(1)); // 默认答案为第1列
        int audioFieldIndex = -1; // -1 表示全局自动扫描音频

        // 1. 读取并解析配置: "Q|A1,A2|Audio"
        String configKey = "mapping_" + apkgFile.getName();
        if (config != null && config.has(configKey)) {
            try {
                String mappingVal = config.getString(configKey);
                String[] parts = mappingVal.split("\\|");
                
                // 第一段：题面索引
                if (parts.length >= 1) {
                    qIndex = Integer.parseInt(parts[0]);
                }
                
                // 第二段：答案列表
                if (parts.length >= 2) {
                    aIndices.clear();
                    for (String idxStr : parts[1].split(",")) {
                        if (!idxStr.trim().isEmpty()) {
                            aIndices.add(Integer.parseInt(idxStr.trim()));
                        }
                    }
                }
                
                // 第三段：指定音频索引
                if (parts.length >= 3) {
                    audioFieldIndex = Integer.parseInt(parts[2].trim());
                }
                
                System.out.println("AnkiLoader: Custom Map Found -> Q:" + qIndex + " A:" + aIndices + " AudioIdx:" + audioFieldIndex);
            } catch (Exception e) {
                System.err.println("AnkiLoader: Mapping config corrupted, using default 0|1.");
            }
        }

        try (ZipFile zip = new ZipFile(apkgFile)) {
            // 2. 预处理音频：将 apkg 里的媒体文件解压到指定目录
            ZipEntry mediaEntry = zip.getEntry("media");
            if (mediaEntry != null) {
                Map<String, String> mediaMap;
                try (Reader reader = new InputStreamReader(zip.getInputStream(mediaEntry), StandardCharsets.UTF_8)) {
                    mediaMap = new Gson().fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                }
                for (Map.Entry<String, String> entry : mediaMap.entrySet()) {
                    String realName = entry.getValue();
                    if (isAudioFile(realName)) {
                        File targetFile = new File(audioOutputDir, realName);
                        if (!targetFile.exists() || targetFile.length() == 0) {
                            ZipEntry fileEntry = zip.getEntry(entry.getKey());
                            if (fileEntry != null) {
                                try (InputStream is = zip.getInputStream(fileEntry)) {
                                    Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                    }
                }
            }

            // 3. 处理 SQLite 数据库
            ZipEntry dbEntry = zip.getEntry("collection.anki21");
            if (dbEntry == null) dbEntry = zip.getEntry("collection.anki2");

            if (dbEntry != null) {
                tempDbFile = File.createTempFile("anki_db_temp", ".db");
                tempDbFile.deleteOnExit();
                try (InputStream is = zip.getInputStream(dbEntry)) {
                    Files.copy(is, tempDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                try { Class.forName("org.sqlite.JDBC"); } catch (Exception ignored) {}

                String url = "jdbc:sqlite:" + tempDbFile.getAbsolutePath();
                try (Connection conn = DriverManager.getConnection(url)) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT flds FROM notes")) {
                        
                        while (rs.next()) {
                            String flds = rs.getString("flds");
                            String[] fieldsData = flds.split("\u001F", -1);

                            // --- 题面处理 ---
                            String frontRaw = (qIndex >= 0 && qIndex < fieldsData.length) ? fieldsData[qIndex] : "";
                            String frontClean = cleanText(frontRaw);

                            if (!frontClean.isEmpty()) {
                                AnkiRawCard card = new AnkiRawCard();
                                card.front = frontClean;

                                // --- 音频处理 ---
                                if (audioFieldIndex != -1 && audioFieldIndex < fieldsData.length) {
                                        card.audio = extractSoundFileName(fieldsData[audioFieldIndex]);
                                } else {
                                    card.audio = extractSoundFileName(flds);
                                }

                                // --- 答案列表处理 ---
                                for (int aIdx : aIndices) {
                                    if (aIdx >= 0 && aIdx < fieldsData.length) {
                                        String aClean = cleanText(fieldsData[aIdx]);
                                        if (!aClean.isEmpty()) {
                                            card.backList.add(aClean);
                                        }
                                    }
                                }

                                resultList.add(card);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempDbFile != null) tempDbFile.delete();
        }
        return resultList;
    }

    private static String cleanText(String text) {
        if (text == null) return "";
        String s = text;
        s = s.replaceAll("(?i)\\[sound:.*?\\]", ""); // 移除音频标签
        s = s.replace("<div>", " ").replace("</div>", " ")
             .replace("<br>", " ").replace("<br/>", " "); // 块标签转空格
        s = s.replaceAll("<[^>]+>", ""); // 移除所有 HTML 标签
        s = s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
             .replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'");
        s = s.replaceAll("\\[.*?\\]", ""); // 移除 [abc] 类的注音
        s = s.replaceAll("\\s+", " ");
        return s.replace(" ", "\u00A0").trim(); // 合并多余空格
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav");
    }

    private static String extractSoundFileName(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("(?i)\\[sound:(.*?)\\]"); // 忽略大小写
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }
}