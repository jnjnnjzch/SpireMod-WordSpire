package WordSpire.ui;

import basemod.*;
import basemod.EasyConfigPanel.ConfigField.FieldSetter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;

import WordSpire.WordSpireInitializer;
import WordSpire.events.CallOfCETEvent.BookEnum;
import WordSpire.helpers.AnkiMetadataReader;
import WordSpire.helpers.BookConfig.LexiconEnum;
import WordSpire.helpers.DownloadConfig; // 导入配置类
import WordSpire.relics.QuizRelic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ModConfigPanel extends ModPanel {
    private static final Logger logger = LogManager.getLogger(ModConfigPanel.class.getName());
    
    // --- 页面管理 ---
    private static final List<List<String>> pages;
    private static final int configPageNum;
    private int pageNum = 0;
    
    // --- 下载页面索引记录 ---
    private int downloadPageStartIndex = -1; 

    // --- 数据映射 ---
    private static final HashMap<String, List<Integer>> intRange;
    private static final HashMap<String, List<LexiconEnum>> lexiconMap;
    private HashMap<String, IUIElement> elementData;
    private UIStrings uiStrings = null;
    private final SpireConfig config;

    // --- UI 组件 ---
    private ModLabel pageTitle;
    private ModLabeledButton pageForward;
    private ModLabeledButton pageBackward;
    private ModLabeledButton pageReturn;

    // --- 坐标常量 ---
    private static final float PAGE_TITLE_X = 360.0F;
    private static final float PAGE_TITLE_Y = 815.0F;
    private static final float ELEMENT_X = 355.0F;
    private static final float ELEMENT_Y = 730.0F;
    private static final List<Float> PADDINGS_Y = Arrays.asList(55.0F, 125.0F);
    private static final float PAGE_BUTTON_X1 = 1015.0F;
    private static final float PAGE_BUTTON_X2 = 815.0F;
    private static final float PAGE_BUTTON_Y = 280.0F;

    // --- 配置变量 ---
    public static boolean darkMode = false;
    public static boolean pureFont = true;
    public static boolean fastMode = false;
    public static boolean casualMode = false;
    public static boolean ignoreCheck = false;
    public static boolean showLexicon = true;
    public static int maxAnsNum = 3;
    public static boolean loadJLPT = true;
    public static boolean loadCET = true;
    public static boolean loadUSER_DICT = true;

    public static HashMap<String, Integer> lexiconData;
    public static HashMap<BookEnum, LexiconEnum> weightedLexicon;
    public static HashMap<BookEnum, List<LexiconEnum>> relicLexicon;

    static {
        pages = new ArrayList<>();
        // Page 0: General Settings
        pages.add(Arrays.asList("darkMode", "pureFont", "fastMode", "casualMode", "ignoreCheck", "showLexicon", "maxAnsNum"));
        // Page 1: Relic/Book Settings (Initial)
        List<String> page2 = new ArrayList<>();
        // page2.add("loadCET");
        // page2.add("loadJLPT");
        // loadUSER_DICT 会在 initRelicPages 或刷新时动态处理
        pages.add(page2);

        configPageNum = 2;
        lexiconMap = new HashMap<>();
        lexiconData = new HashMap<>();
        weightedLexicon = new HashMap<>();
        relicLexicon = new HashMap<>();

        intRange = new HashMap<>();
        intRange.put("maxAnsNum", Arrays.asList(1, 3));
    }

    public ModConfigPanel() {
        try {
            Properties configDefaults = new Properties();
            for (int i = 0; i < configPageNum; i++) {
                List<String> page = pages.get(i);
                for (String name: page) {
                    Field field = this.getClass().getField(name);
                    configDefaults.put(field.getName(), String.valueOf(field.get(null)));
                }
            }
            // 默认 USER_DICT 为 true
            configDefaults.put("loadUSER_DICT", "true");
            
            for (Map.Entry<String, Integer> entry: lexiconData.entrySet()) {
                configDefaults.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            this.config = new SpireConfig(WordSpireInitializer.MOD_ID, "config", configDefaults);
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new RuntimeException("Failed to set up SpireConfig for " + WordSpireInitializer.MOD_ID, e);
        }
    }

    public static List<LexiconEnum> getRelicLexicons(BookEnum b) {
        return relicLexicon.getOrDefault(b, new ArrayList<>());
    }

    @Nullable
    public static LexiconEnum getWeightedLexicon(BookEnum b) {
        return weightedLexicon.getOrDefault(b, null);
    }

    public void receivePostInitialize() {
        this.uiStrings = CardCrawlGame.languagePack.getUIString(WordSpireInitializer.JSON_MOD_KEY + "ConfigPanel");
        this.elementData = new HashMap<>();
        
        this.initUIElements();
        this.initRelicPages();
        this.buildDownloadPages(); // 构建下载页面
        
        this.setPage(0);
        this.checkReset();
        this.updateWeights();
        this.resetAllQuizRelics();
    }

    private void initUIElements() {
        pageTitle = new ModLabel("No Title", PAGE_TITLE_X, PAGE_TITLE_Y, this, (text) -> {});
        try {
            for (int i = 0; i < configPageNum; i++) {
                List<String> page = pages.get(i);
                float pagePos = ELEMENT_Y;
                for (String name: page) {
                    Field field = this.getClass().getField(name);
                    IUIElement element = buildElement(field, name);
                    if (element != null) {
                        element.setY(pagePos);
                        this.elementData.put(name, element);
                        if (elementData.containsKey(name + "_label")) {
                            elementData.get(name + "_label").set(ELEMENT_X + 40.0F, pagePos);
                            pagePos -= PADDINGS_Y.get(i);
                            element.set(ELEMENT_X + 40.0F, pagePos);
                        }
                        pagePos -= PADDINGS_Y.get(i);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // --- Level 1 (Page 1) 增加下载中心入口按钮 ---
        // 放在右上角区域
        ModLabeledButton dlCenterBtn = new ModLabeledButton("Download Center", 1100.0F, 800.0F, Color.CYAN, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
            if (this.downloadPageStartIndex != -1) {
                this.setPage(this.downloadPageStartIndex);
            }
        });
        elementData.put("btn_dl_center", dlCenterBtn);
        // 确保添加到 Page 1
        if (pages.size() > 1) {
            pages.get(1).add("btn_dl_center");
        }

        // 翻页按钮
        pageForward = new ModLabeledButton(">", PAGE_BUTTON_X1, PAGE_BUTTON_Y, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {this.nextPage(true);});
        pageBackward = new ModLabeledButton("<", PAGE_BUTTON_X2, PAGE_BUTTON_Y, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {this.nextPage(false);});
        
        // 智能返回按钮
        pageReturn = new ModLabeledButton(uiStrings.EXTRA_TEXT[1], PAGE_BUTTON_X2, PAGE_BUTTON_Y, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
            // Level 3 返回逻辑
            List<String> currentKeys = pages.get(this.pageNum);
            String firstKey = currentKeys.isEmpty() ? "" : currentKeys.get(0);
            
            if (firstKey.startsWith("mapping_")) {
                int prefixLen = "mapping_".length();
                int suffixIdx = firstKey.indexOf(".apkg");
                if (suffixIdx > prefixLen) {
                    String fileName = firstKey.substring(prefixLen, suffixIdx);
                    WordSpireInitializer.reloadAnkiFile(fileName);
                }
                // 返回 User Dict 列表页（通常是 Level 2 的第一页）
                // 寻找 ud_l2_title_0
                for(int i=0; i<pages.size(); i++) {
                    if(!pages.get(i).isEmpty() && pages.get(i).get(0).startsWith("ud_l2_title_")) {
                        this.setPage(i); return;
                    }
                }
                this.setPage(1); // Fallback
                return;
            }
            
            // Download Page 返回逻辑
            if (firstKey.startsWith("dl_pg_title_")) {
                this.setPage(1); // 返回 Level 1
                return;
            }

            // Level 2 返回逻辑
            if (firstKey.startsWith("ud_l2_title_")) {
                this.setPage(1); // 返回 Level 1
                return;
            }

            // 默认保存并返回 Page 1
            try {
                if (!this.checkWeights()) return;
                for (String s: currentKeys) {
                    if (lexiconData.containsKey(s)) {
                        this.config.setString(s, String.valueOf(lexiconData.get(s)));
                    }
                }
                this.config.save();
            } catch (IOException e) { throw new RuntimeException(e); }
            this.setPage(1);
        });
    }

    private void initRelicPages() {
        for (Map.Entry<String, List<LexiconEnum>> entry : lexiconMap.entrySet()) {
            boolean isUserDict = entry.getKey().equals("loadUSER_DICT");
            
            // 如果是用户词典，调用专用构建方法
            if (isUserDict) {
                // 动态注入 Level 1 的 Toggle 按钮（如果初始没有，refresh时会用到这个逻辑）
                injectUserDictToggleIntoLevel1();
                buildUserDictPages();
            } else {
                // 内置词典逻辑
                IUIElement base = elementData.get(entry.getKey());
                if (base == null) continue; // 如果是loadCET但没找到element（不太可能），跳过

                // Level 1 跳转按钮
                ModLabeledButton jumpBtn = new ModLabeledButton(uiStrings.EXTRA_TEXT[0], Math.max(base.getX() + 200.0F, 1000.0F), base.getY() - 2.0F,
                        Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> { 
                            // 查找对应的内置词典页面
                            // 这里简化处理，直接找对应Key开头的页面
                            // 实际原逻辑是顺序添加的，这里为了兼容动态页面，建议使用ID查找
                            // 暂时使用简单的偏移，因为内置词典是一次性生成的
                            // 由于原逻辑是在 loop 里 pages.add，我们需要记录索引
                            // 但现在页面顺序变了，建议重构。
                            // 简单方案：给内置词典页面加个特征ID，遍历查找
                            String targetPrefix = entry.getKey() + "_name";
                            for(int i=0; i<pages.size(); i++) {
                                if (pages.get(i).size() > 0 && pages.get(i).contains(targetPrefix)) {
                                    this.setPage(i); return;
                                }
                            }
                            // 如果找不到（第一次初始化），说明还没生成，就在下面生成
                            int target = pages.size();
                            if (this.downloadPageStartIndex != -1 && target >= this.downloadPageStartIndex) {
                                // 如果下载页已经生成在前面了，插入位置要注意
                                // 但通常 initRelicPages 在 buildDownloadPages 前运行
                            }
                            this.setPage(target);
                        });
                
                elementData.put(entry.getKey() + "_jump", jumpBtn);
                pages.get(1).add(entry.getKey() + "_jump");

                ModLabel displayLabel = new ModLabel("", base.getX() + 25.0F, base.getY() - PADDINGS_Y.get(0), this, (text) -> {});
                elementData.put(entry.getKey() + "_display", displayLabel);
                pages.get(1).add(entry.getKey() + "_display");

                // 生成内置词典详情页
                List<String> level2PageIds = new ArrayList<>();
                for (int i = 0; i < entry.getValue().size(); i++) {
                    LexiconEnum l = entry.getValue().get(i);
                    String tmp_name = entry.getKey() + "_" + l.name();
                    if (!config.has(tmp_name)) config.setString(tmp_name, "1");
                    lexiconData.put(tmp_name, Integer.parseInt(this.config.getString(tmp_name)));

                    float x = 380.0F + 400.0F * (float) (i % 3);
                    float y = 720.0F + -160.0F * (float) (i / 3);

                    IUIElement nameLabel = new ModLabel(uiStrings.TEXT_DICT.getOrDefault(l.name(), l.name()), x, y, this, (label) -> {});
                    level2PageIds.add(tmp_name + "_name");
                    elementData.put(tmp_name + "_name", nameLabel);

                    level2PageIds.add(tmp_name + "_add");
                    elementData.put(tmp_name + "_add", new ModLabeledButton("+", x + 200.0F, y - 80.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
                        lexiconData.put(tmp_name, Math.min(10, lexiconData.get(tmp_name) + 1));
                        ((ModLabel) elementData.get(tmp_name)).text = lexiconData.get(tmp_name).toString();
                    }));

                    level2PageIds.add(tmp_name + "_sub");
                    elementData.put(tmp_name + "_sub", new ModLabeledButton("-", x + 0.0F, y - 80.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
                        lexiconData.put(tmp_name, Math.max(0, lexiconData.get(tmp_name) - 1));
                        ((ModLabel) elementData.get(tmp_name)).text = lexiconData.get(tmp_name).toString();
                    }));

                    level2PageIds.add(tmp_name);
                    elementData.put(tmp_name, new ModLabel(lexiconData.get(tmp_name).toString(), x + 120.0F, y - 50.0F, this, (label) -> {}));
                }
                pages.add(level2PageIds);
            }
        }
    }

    // --- 新增：Level 1 动态注入逻辑 ---
    private void injectUserDictToggleIntoLevel1() {
        String key = "loadUSER_DICT";
        // 如果数据有了，但UI元素没有
        if (!elementData.containsKey(key)) {
            // 1. 创建 Toggle
            if (!config.has(key)) config.setString(key, "true");
            boolean val = Boolean.parseBoolean(config.getString(key));
            
            // 计算位置：放在 CET/JLPT 下面
            // 假设 Page 1 现有元素占用了 Y，我们需要找到最后一个元素的位置
            // 这里简单硬编码一个位置，或者根据 pages.get(1) 的大小计算
            float yPos = ELEMENT_Y - PADDINGS_Y.get(1) * 0; // 第3行
            
            ModLabeledToggleButton toggle = new ModLabeledToggleButton("User Dictionary", ELEMENT_X, yPos,
                    Settings.CREAM_COLOR, FontHelper.charDescFont, val, this, (label) -> {},
                    (button) -> {
                        try {
                            config.setString(key, String.valueOf(button.enabled));
                            config.save();
                        } catch (Exception e) {}
                    });
            elementData.put(key, toggle);
            pages.get(1).add(key);

            // 2. 创建 Manage 按钮
            ModLabeledButton jumpBtn = new ModLabeledButton("Manage", Math.max(ELEMENT_X + 200.0F, 1000.0F), yPos + 8.0F, // 微调 Y
                    Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> { 
                        // 跳转到 User Dict Level 2
                        for(int i=0; i<pages.size(); i++) {
                            if(!pages.get(i).isEmpty() && pages.get(i).get(0).startsWith("ud_l2_title_")) {
                                this.setPage(i); return;
                            }
                        }
                    });
            elementData.put(key + "_jump", jumpBtn);
            pages.get(1).add(key + "_jump");

            // 3. Display Label
            ModLabel displayLabel = new ModLabel("", ELEMENT_X + 25.0F, yPos - PADDINGS_Y.get(0), this, (text) -> {});
            elementData.put(key + "_display", displayLabel);
            pages.get(1).add(key + "_display");
        }
    }

    // --- 新增：构建下载页面 (独立界面) ---
    private void buildDownloadPages() {
        this.downloadPageStartIndex = pages.size();
        
        int itemsPerPage = 6; // 2列 x 3行
        int totalItems = DownloadConfig.PRESETS.size();
        int totalPages = (int) Math.ceil((double)totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        for (int p = 0; p < totalPages; p++) {
            List<String> page = new ArrayList<>();
            
            // 标题
            String titleKey = "dl_pg_title_" + p;
            elementData.put(titleKey, new ModLabel("Download Center (" + (p+1) + "/" + totalPages + ")", PAGE_TITLE_X, PAGE_TITLE_Y, Color.CYAN, this, (l)->{}));
            page.add(titleKey);

            int startItem = p * itemsPerPage;
            int endItem = Math.min(startItem + itemsPerPage, totalItems);

            for (int i = startItem; i < endItem; i++) {
                DownloadConfig.DownloadPreset preset = DownloadConfig.PRESETS.get(i);
                int idxInPage = i - startItem;
                
                // 布局：2列 (X=400, 1000)
                float colX = (idxInPage % 2 == 0) ? 400.0F : 1000.0F;
                // 行 Y
                float rowY = 700.0F - (idxInPage / 2) * 200.0F; 

                String baseKey = "dl_item_" + i;
                
                // 名字
                elementData.put(baseKey + "_name", new ModLabel(preset.name, colX, rowY, Color.GOLD, this, (l)->{}));
                page.add(baseKey + "_name");
                
                // // 描述
                // elementData.put(baseKey + "_desc", new ModLabel(preset.description, colX, rowY - 40.0F, Color.GRAY, this, (l)->{}));
                // page.add(baseKey + "_desc");

                // 下载按钮
                elementData.put(baseKey + "_btn", new ModLabeledButton("Download", colX, rowY - 100.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                    this.downloadAndInstall(preset);
                }));
                page.add(baseKey + "_btn");
            }
            
            // 内部翻页
            if (p > 0) {
                String k = "dl_prev_" + p;
                final int targetPage = downloadPageStartIndex + p - 1; // [修复] 定义 final 变量
                elementData.put(k, new ModLabeledButton("Prev", 450.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetPage)));
                page.add(k);
            }
            if (p < totalPages - 1) {
                String k = "dl_next_" + p;
                final int targetPage = downloadPageStartIndex + p + 1; // [修复] 定义 final 变量
                elementData.put(k, new ModLabeledButton("Next", 1150.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetPage)));
                page.add(k);
            }

            pages.add(page);
        }
    }

    private void buildUserDictPages() {
        if (!lexiconMap.containsKey("loadUSER_DICT")) return;
        
        File dir = new File(WordSpireInitializer.USER_DICT_PATH);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".apkg"));
        if (files == null) files = new File[0];

        int filesPerPage = 4;
        int totalFiles = files.length;
        int totalL2Pages = (int) Math.ceil((double)totalFiles / filesPerPage);
        if (totalL2Pages == 0) totalL2Pages = 1;

        int l2StartIndex = pages.size(); 
        int[] fileToL3PageIndex = new int[totalFiles];
        int currentL3Accumulator = l2StartIndex + totalL2Pages;
        AnkiMetadataReader.AnkiStructure[] structs = new AnkiMetadataReader.AnkiStructure[totalFiles];

        for (int i = 0; i < totalFiles; i++) {
            structs[i] = AnkiMetadataReader.readStructure(files[i]);
            fileToL3PageIndex[i] = currentL3Accumulator;
            int subPages = (int) Math.ceil((double)structs[i].fieldNames.size() / 4.0);
            if (subPages == 0) subPages = 1;
            currentL3Accumulator += subPages;
        }

        for (int p = 0; p < totalL2Pages; p++) {
            List<String> l2SubPage = new ArrayList<>();
            String titleKey = "ud_l2_title_" + p;
            elementData.put(titleKey, new ModLabel("User Dictionaries (" + (p+1) + "/" + totalL2Pages + ")", 360.0F, 810.0F, Color.GOLD, this, (l)->{}));
            l2SubPage.add(titleKey);

            String refreshKey = "ud_refresh_" + p;
            elementData.put(refreshKey, new ModLabeledButton("Refresh Files", 800.0F, 810.0F, Color.CYAN, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.refreshUserDictionaries()));
            l2SubPage.add(refreshKey);

            int startFile = p * filesPerPage;
            int endFile = Math.min(startFile + filesPerPage, totalFiles);

            for (int i = startFile; i < endFile; i++) {
                File apkgFile = files[i];
                int idxInPage = i - startFile;
                float x = 400.0F + (idxInPage % 2) * 600.0F;
                float y = 700.0F - (idxInPage / 2) * 200.0F;
                
                String fileId = "user_apkg_" + i;
                elementData.put(fileId + "_name", new ModLabel(apkgFile.getName(), x, y, Color.GOLD, this, (l) -> {}));
                l2SubPage.add(fileId + "_name");

                String nameNoExt = apkgFile.getName().replace(".apkg", "");
                String wKey = "loadUSER_DICT_" + nameNoExt;
                
                if (!lexiconData.containsKey(wKey)) lexiconData.put(wKey, 1);
                if (!config.has(wKey)) config.setString(wKey, "1");

                float wY = y - 70.0F;
                elementData.put(wKey + "_sub", new ModLabeledButton("-", x, wY, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                    lexiconData.put(wKey, Math.max(0, lexiconData.get(wKey) - 1));
                    ((ModLabel) elementData.get(wKey)).text = lexiconData.get(wKey).toString();
                    try { config.setString(wKey, lexiconData.get(wKey).toString()); config.save(); } catch (Exception e) {}
                }));
                l2SubPage.add(wKey + "_sub");

                elementData.put(wKey, new ModLabel(lexiconData.get(wKey).toString(), x + 100.0F, wY + 10, this, (l) -> {}));
                l2SubPage.add(wKey);

                elementData.put(wKey + "_add", new ModLabeledButton("+", x + 140.0F, wY, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                    lexiconData.put(wKey, Math.min(10, lexiconData.get(wKey) + 1));
                    ((ModLabel) elementData.get(wKey)).text = lexiconData.get(wKey).toString();
                    try { config.setString(wKey, lexiconData.get(wKey).toString()); config.save(); } catch (Exception e) {}
                }));
                l2SubPage.add(wKey + "_add");

                int targetPage = fileToL3PageIndex[i];
                elementData.put(fileId + "_cfg", new ModLabeledButton("Config Mapping", x, y - 140.0F, Color.SKY, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetPage)));
                l2SubPage.add(fileId + "_cfg");
            }
            
            if (p > 0) {
                String prevKey = "ud_l2_prev_" + p;
                final int targetP = l2StartIndex + p - 1; // [修复] 提前计算并赋值给 final 变量
                elementData.put(prevKey, new ModLabeledButton("< Prev", 450.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetP)));
                l2SubPage.add(prevKey);
            }
            if (p < totalL2Pages - 1) {
                String nextKey = "ud_l2_next_" + p;
                final int targetP = l2StartIndex + p + 1; // [修复] 提前计算并赋值给 final 变量
                elementData.put(nextKey, new ModLabeledButton("Next >", 1150.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetP)));
                l2SubPage.add(nextKey);
            }
            pages.add(l2SubPage);
        }

        // Level 3 (Mapping)
        for (int i = 0; i < totalFiles; i++) {
            File apkgFile = files[i];
            AnkiMetadataReader.AnkiStructure struct = structs[i];
            String configKey = "mapping_" + apkgFile.getName();
            if (!config.has(configKey)) config.setString(configKey, "0|1|-1");
            
            int l3FieldsPerPage = 4;
            int totalSubPages = (int) Math.ceil((double)struct.fieldNames.size() / l3FieldsPerPage);
            if (totalSubPages == 0) totalSubPages = 1;
            int startPageIdx = fileToL3PageIndex[i];

            for (int p = 0; p < totalSubPages; p++) {
                List<String> subPage = new ArrayList<>();
                String pgTitleKey = configKey + "_pg_title_" + p;
                elementData.put(pgTitleKey, new ModLabel(apkgFile.getName() + " (" + (p+1) + "/" + totalSubPages + ")", 360.0F, 810.0F, Color.GOLD, this, (l)->{}));
                subPage.add(pgTitleKey);

                int startF = p * l3FieldsPerPage;
                int endF = Math.min(startF + l3FieldsPerPage, struct.fieldNames.size());
                
                for (int j = startF; j < endF; j++) {
                    final int fIdx = j;
                    float rowY = 750.0F - (j - startF) * 110.0F;
                    
                    String nKey = configKey + "_" + j + "_n";
                    elementData.put(nKey, new ModLabel(j + ". " + struct.fieldNames.get(j), 360.0F, rowY, Color.WHITE, this, (l)->{}));
                    subPage.add(nKey);

                    // [补回] 字段内容预览
                    String pKey = configKey + "_" + j + "_p";
                    // 安全获取预览文本，防止越界或 null
                    String previewText = (struct.sampleValues != null && struct.sampleValues.size() > j) ? struct.sampleValues.get(j) : "";
                    if (previewText == null) previewText = "";
                    // 截断过长文本
                    if (previewText.length() > 20) previewText = previewText.substring(0, 20) + "...";
                    
                    // 添加 Label (灰色，稍微下移错开)
                    elementData.put(pKey, new ModLabel("   > " + previewText, 360.0F, rowY - 35.0F, Color.GRAY, this, (l)->{}));
                    subPage.add(pKey);
                    
                    String qKey = configKey + "_" + j + "_q";
                    elementData.put(qKey, new ModLabeledToggleButton("Question", 750.0F, rowY, Color.WHITE, FontHelper.cardDescFont_N, config.getString(configKey).startsWith(j + "|"), this, (l)->{}, (btn) -> {
                        if (btn.enabled) {
                             for (String k : elementData.keySet()) { if (k.startsWith(configKey) && k.endsWith("_q") && !k.equals(qKey)) ((ModLabeledToggleButton)elementData.get(k)).toggle.enabled = false; }
                            String[] pts = config.getString(configKey).split("\\|");
                            config.setString(configKey, fIdx + "|" + (pts.length > 1 ? pts[1] : "1") + "|" + (pts.length > 2 ? pts[2] : "-1"));
                            try { config.save(); } catch (Exception e) {}
                        } else btn.enabled = true;
                    }));
                    subPage.add(qKey);

                    String aKey = configKey + "_" + j + "_a";
                    boolean isA = Arrays.asList(config.getString(configKey).split("\\|")[1].split(",")).contains(String.valueOf(j));
                    elementData.put(aKey, new ModLabeledToggleButton("Answer", 980.0F, rowY, Color.WHITE, FontHelper.cardDescFont_N, isA, this, (l)->{}, (btn) -> {
                         String[] pts = config.getString(configKey).split("\\|");
                         Set<String> aSet = new HashSet<>(Arrays.asList(pts[1].split(",")));
                         if (btn.enabled) aSet.add(String.valueOf(fIdx)); else aSet.remove(String.valueOf(fIdx));
                         config.setString(configKey, pts[0] + "|" + String.join(",", aSet) + "|" + (pts.length > 2 ? pts[2] : "-1"));
                         try { config.save(); } catch (Exception e) {}
                    }));
                    subPage.add(aKey);

                    String sKey = configKey + "_" + j + "_s";
                    boolean isS = config.getString(configKey).endsWith("|" + j);
                    elementData.put(sKey, new ModLabeledToggleButton("Sound", 1210.0F, rowY, Color.WHITE, FontHelper.cardDescFont_N, isS, this, (l)->{}, (btn) -> {
                        if (btn.enabled) {
                             for (String k : elementData.keySet()) { if (k.startsWith(configKey) && k.endsWith("_s") && !k.equals(sKey)) ((ModLabeledToggleButton)elementData.get(k)).toggle.enabled = false; }
                             String[] pts = config.getString(configKey).split("\\|");
                             config.setString(configKey, pts[0] + "|" + pts[1] + "|" + fIdx);
                        } else {
                             String[] pts = config.getString(configKey).split("\\|");
                             if (pts.length > 2 && pts[2].equals(String.valueOf(fIdx))) config.setString(configKey, pts[0] + "|" + pts[1] + "|-1");
                        }
                        try { config.save(); } catch (Exception e) {}
                    }));
                    subPage.add(sKey);
                }

                if (p > 0) {
                     String pKey = configKey + "_pgbtn_" + p + "_prev";
                     final int targetPage = startPageIdx + p - 1; // [修复] 定义 final 变量
                     elementData.put(pKey, new ModLabeledButton("Prev", 1450.0F, 600.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetPage)));
                     subPage.add(pKey);
                }
                if (p < totalSubPages - 1) {
                     String nKey = configKey + "_pgbtn_" + p + "_next";
                     final int targetPage = startPageIdx + p + 1;
                     elementData.put(nKey, new ModLabeledButton("Next", 1450.0F, 400.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetPage)));
                     subPage.add(nKey);
                }
                pages.add(subPage);
            }
        }
        updateWeights();
    }

    private void refreshUserDictionaries() {
        // 1. 系统重载
        WordSpireInitializer.reloadUserDicts();

        // 2. 清理 UI：移除 UserDict 和 Mapping 页面
        Iterator<List<String>> it = pages.iterator();
        while (it.hasNext()) {
            List<String> page = it.next();
            if (!page.isEmpty()) {
                String fk = page.get(0);
                if (fk.startsWith("ud_") || fk.startsWith("mapping_")) {
                    it.remove();
                    for (String k : page) elementData.remove(k);
                }
            }
        }
        
        // 3. 关键修复：确保 Level 1 拥有 loadUSER_DICT 的 Toggle 和 Manage 按钮
        injectUserDictToggleIntoLevel1();

        // 4. 重建 UserDict 页面
        buildUserDictPages();
        
        // 5. 刷新视图 (Level 1)
        this.setPage(1);
    }

    private void downloadAndInstall(DownloadConfig.DownloadPreset preset) {
        try {
            logger.info("Downloading " + preset.name + "...");
            
            File targetFile = new File(WordSpireInitializer.USER_DICT_PATH, preset.fileName);
            if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();

            // === [升级] 使用 HttpURLConnection 以支持设置超时和代理 ===
            URL url = new URL(preset.url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            
            // 伪装成浏览器，防止被服务器拒绝
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            // 设置超时 (单位毫秒)，防止游戏无限卡死
            conn.setConnectTimeout(10000); // 10秒连不上就报错
            conn.setReadTimeout(60000);    // 60秒读不完就报错
            conn.setInstanceFollowRedirects(true);
            
            // 手动处理 302/301 重定向 (GitHub Releases 必选)
            int status = conn.getResponseCode();
            if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
                status == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                status == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
                
                String newUrl = conn.getHeaderField("Location");
                logger.info("Redirecting to: " + newUrl);
                conn = (java.net.HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
            }

            // 开始下载
            try (java.io.InputStream in = conn.getInputStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            logger.info("Download successful!");

            // 写入配置
            String configKey = "mapping_" + preset.fileName;
            config.setString(configKey, preset.configRaw);
            config.save();

            // 刷新界面
            refreshUserDictionaries();
            
        } catch (Exception e) {
            logger.error("Download failed", e);
            // 这里其实可以弹个 Toast 提示用户失败，或者在界面上显示个 Error
        }
    }

    public static void addRelicPage(BookEnum relicBook, List<LexiconEnum> list, int default_) {
        relicLexicon.put(relicBook, new ArrayList<>());
        String varName = "load" + relicBook.name();
        lexiconMap.put(varName, list);
        for (LexiconEnum l: list) {
            lexiconData.put(varName + "_" + l.name(), default_);
        }
    }

    public static void addRelicPage(BookEnum relicBook, List<LexiconEnum> list) {
        addRelicPage(relicBook, list, 1);
    }

    public void nextPage(boolean forward) {
        if (forward) {
            this.pageNum++;
            if (this.pageNum >= configPageNum) {
                this.pageNum = 0;
            }
        } else {
            this.pageNum--;
            if (this.pageNum < 0) {
                this.pageNum = configPageNum - 1;
            }
        }
        setPage(this.pageNum);
    }

    public void setPage(int id) {
        if (pages.isEmpty()) return;
        if (id < 0) id = pages.size() - 1;
        id %= pages.size();

        this.pageNum = id;
        List<IUIElement> pageElements = new ArrayList<>();
        if (uiStrings != null && id < uiStrings.TEXT.length && !uiStrings.TEXT[id].isEmpty()) {
            this.pageTitle.text = uiStrings.TEXT[id];
            pageElements.add(this.pageTitle);
        }
        for (String name: pages.get(id)) {
            IUIElement el = this.elementData.get(name);
            if (el != null) pageElements.add(el);
            if (this.elementData.containsKey(name + "_label")) {
                pageElements.add(this.elementData.get(name + "_label"));
            }
        }
        if (this.pageNum < configPageNum) {
            pageElements.add(this.pageForward);
            pageElements.add(this.pageBackward);
        } else {
            pageElements.add(this.pageReturn);
        }
        this.resetElements(pageElements);
        if (id == 1) this.updateWeights();
    }

    private IUIElement buildElement(Field field, String name) throws IllegalAccessException {
        if (field.getType() == boolean.class) {
            field.set(null, Boolean.parseBoolean(this.config.getString(field.getName())));
            return new ModLabeledToggleButton(uiStrings.TEXT_DICT.getOrDefault(name, name), ELEMENT_X, 0.0F,
                    Settings.CREAM_COLOR, FontHelper.charDescFont, (Boolean)field.get(null), this, (label) -> {},
                    (button) -> {saveVar(button.enabled, field, s -> {field.set(null, Boolean.parseBoolean(s));});});
        }
        if (field.getType() == int.class) {
            field.set(null, Integer.parseInt(this.config.getString(field.getName())));
            elementData.put(name + "_label",
                    new ModLabel(uiStrings.TEXT_DICT.getOrDefault(name, name), ELEMENT_X, 0.0F,
                            Settings.CREAM_COLOR, FontHelper.charDescFont, this, (text) -> {}));
            return new ModMinMaxSlider("", ELEMENT_X, 10.0F, intRange.get(name).get(0), intRange.get(name).get(1),
                    (Integer)field.get(null), "%.0f", this,
                    (slider) -> {saveVar(slider.getValue(), field, s -> {field.set(null, Math.round(Float.parseFloat(s)));});});
        }
        return null;
    }

    private void saveVar(Object var, Field field, FieldSetter setter) {
        try {
            setter.set(var.toString());
            this.config.setString(field.getName(), field.get(null).toString());
            this.config.save();
        } catch (IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean updateStop = false;
    private List<IUIElement> tmpCache = null;
    private void resetElements(List<IUIElement> list) {
        this.tmpCache = list;
        this.updateStop = true;
    }
    private void checkReset() {
        if (!this.updateStop) return;
        this.getUpdateElements().clear();
        this.getRenderElements().clear();
        if (tmpCache != null) {
            for (IUIElement element: tmpCache) this.addUIElement(element);
            tmpCache = null;
        }
        this.updateStop = false;
    }

    @Override
    public void update() {
        for (IUIElement element: this.getUpdateElements()) element.update();
        this.checkReset();
        if (InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            BaseMod.modSettingsUp = false;
        }
        if (!BaseMod.modSettingsUp) {
            this.waitingOnEvent = false;
            Gdx.input.setInputProcessor(this.oldInputProcessor);
            CardCrawlGame.mainMenuScreen.lighten();
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            CardCrawlGame.cancelButton.hideInstantly();
            this.isUp = false;
            this.setPage(0);
            this.checkReset();
            this.resetAllQuizRelics();
        }
    }

    public void resetAllQuizRelics() {
        for (AbstractRelic r: RelicLibrary.specialList) {
            if (r instanceof QuizRelic) {
                ((QuizRelic) r).resetTexture();
                ((QuizRelic) r).resetDescription();
            }
        }
    }

    private void updateWeights() {
        for (Map.Entry<String, List<LexiconEnum>> entry: lexiconMap.entrySet()) {
            IUIElement element = elementData.getOrDefault(entry.getKey() + "_display", null);
            if (!(element instanceof ModLabel)) continue;
            int total = 0;
            int max = 0; 
            List<LexiconEnum> notZeroList = new ArrayList<>();
            for (LexiconEnum l: entry.getValue()) {
                int tmp = lexiconData.getOrDefault(entry.getKey() + "_" + l.name(), 0);
                if (tmp != 0) {
                    notZeroList.add(l);
                    total += tmp;
                }
                if (tmp > max) max = tmp;
            }
            float k = total == 0 ? 0.0F : 100.0F / total;
            StringBuilder sb = new StringBuilder();
            BookEnum relic = BookEnum.valueOf(entry.getKey().substring(4));
            ArrayList<LexiconEnum> lexicons = (ArrayList<LexiconEnum>) getRelicLexicons(relic);
            lexicons.clear();
            LexiconEnum weighted = null;
            for (LexiconEnum l: notZeroList) {
                int tmp = lexiconData.get(entry.getKey() + "_" + l.name());
                if (weighted == null && tmp == max) weighted = l;
                for (int i = 0; i < tmp; i++) lexicons.add(l);
                sb.append(uiStrings.TEXT_DICT.getOrDefault(l.name(), l.name())).append(": ");
                sb.append(Math.round(k * tmp)).append("%. ");
            }
            relicLexicon.put(relic, lexicons);
            ((ModLabel) element).text = sb.toString();
            weightedLexicon.put(relic, weighted);
            logger.info("Weights updated.");
            logLexicons();
        }
    }

    public static void logLexicons() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====== Lexicons Info ======");
        weightedLexicon.forEach((book, weightedL) -> {
            sb.append("\n---- Book-").append(book.name()).append(" (")
                    .append(weightedL == null ? "NULL" : weightedL.name()).append(") ----");
            sb.append("\n> Lexicons: ").append(getRelicLexicons(book).toString());
        });
        sb.append("\n======== Info  End ========");
        logger.info(sb.toString());
    }

    private boolean checkWeights() {
        for (Map.Entry<String, List<LexiconEnum>> entry: lexiconMap.entrySet()) {
            int total = 0;
            for (LexiconEnum l: entry.getValue()) {
                total += lexiconData.getOrDefault(entry.getKey() + "_" + l.name(), 0);
            }
            if (total == 0) {
                this.updateColor(entry, Color.RED);
                logger.error("Weights should not be ZEROS.");
                return false;
            }
            this.updateColor(entry, Color.WHITE);
        }
        return true;
    }

    private void updateColor(Map.Entry<String, List<LexiconEnum>> entry, Color c) {
        for (LexiconEnum l: entry.getValue()) {
            IUIElement e = elementData.getOrDefault(entry.getKey() + "_" + l.name(), null);
            if (e instanceof ModLabel) ((ModLabel) e).color = c;
        }
    }
}