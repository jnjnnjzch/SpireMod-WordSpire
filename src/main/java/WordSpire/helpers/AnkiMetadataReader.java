package WordSpire.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.StandardCopyOption;

public class AnkiMetadataReader {

    public static class AnkiStructure {
        public List<String> fieldNames = new ArrayList<>();
        public List<String> sampleValues = new ArrayList<>();
    }

    private static class AnkiModel {
        public List<AnkiFieldDef> flds;
    }

    private static class AnkiFieldDef {
        public String name;
        public Integer ord;
    }

    public static AnkiStructure readStructure(File apkgFile) {
        AnkiStructure structure = new AnkiStructure();
        File tempDbFile = null;

        try (ZipFile zip = new ZipFile(apkgFile)) {
            // 解压数据库
            ZipEntry dbEntry = zip.getEntry("collection.anki21"); // 先读新版的
            if (dbEntry == null) dbEntry = zip.getEntry("collection.anki2"); //读不到再读旧版的

            if (dbEntry != null) {
                // 创建一个tempDb用来读取
                tempDbFile = File.createTempFile("anki_meta_temp", ".db");
                tempDbFile.deleteOnExit();
                try (InputStream is = zip.getInputStream(dbEntry)) {
                    Files.copy(is, tempDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                
                // 强制 Java 加载 SQLite 的驱动插件（JDBC）
                try {Class.forName("org.sqlite.JDBC"); } catch (Exception e) { e.printStackTrace(); }
                // 这里的 url 指向我们刚才从 .apkg 解压出来的临时 .db 文件
                String url = "jdbc:sqlite:" + tempDbFile.getAbsolutePath();
                try (Connection conn = DriverManager.getConnection(url)) {
                    // 获取第一个模型的字段名
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT models FROM col")) {
                        if (rs.next()) {
                            String json = rs.getString("models");
                            // Anki 把所有模版定义都塞在一个 JSON 对象里
                            // 这里用 Gson 把 JSON 字符串转成 Java 的 Map 结构
                            Map<String, AnkiModel> rawMap = new Gson().fromJson(json, new TypeToken<Map<String, AnkiModel>>(){}.getType());
                            // 使用 TreeMap 自动按 ord (索引) 排序，并收集所有模型中的字段名
                            Map<Integer, String> allFieldsMap = new TreeMap<>();
                            // 遍历所有的模型 (Model)
                            for (AnkiModel model : rawMap.values()) {
                                if (model.flds != null) {
                                    for (AnkiFieldDef f : model.flds) {
                                        allFieldsMap.putIfAbsent(f.ord, f.name);
                                    }
                                }
                            }
                            // 按排序后的顺序将字段名加入到 structure 中
                            for (String fieldName : allFieldsMap.values()) {
                                structure.fieldNames.add(fieldName);
                            }
                        }
                    }
                    // 获取第一条笔记 (预览内容)
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT flds FROM notes LIMIT 1")) {
                        if (rs.next()) {
                            String flds = rs.getString("flds");
                            String[] parts = flds.split("\u001F", -1);
                            for (String part : parts) {
                                // 截取前 20 个字符作为预览，不做清洗
                                String preview = part.length() > 20 ? part.substring(0, 20) + "..." : part;
                                structure.sampleValues.add(preview);
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
        
        // 补齐：如果数据行比字段名多/少，进行对其防止越界
        while (structure.sampleValues.size() < structure.fieldNames.size()) {
            structure.sampleValues.add("N/A");
        }
        
        return structure;
    }
}