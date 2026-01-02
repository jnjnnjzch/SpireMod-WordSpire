package CET46InSpire.ui;

import CET46InSpire.CET46Initializer;
import CET46InSpire.events.CallOfCETEvent.BookEnum;
import CET46InSpire.helpers.AnkiMetadataReader;
import CET46InSpire.helpers.BookConfig.LexiconEnum;
import CET46InSpire.relics.QuizRelic;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ModConfigPanel extends ModPanel {
    private static final Logger logger = LogManager.getLogger(ModConfigPanel.class.getName());
    /**
     * pages用于标记参数的显示页数, String是 elementData 的 key
     */
    private static final List<List<String>> pages;
    private static final int configPageNum;
    /**
     * intRange 用于记录 int 型数据的滑块范围，key是 pages的字段
     */
    private static final HashMap<String, List<Integer>> intRange;
    /**
     * key: Button的elementId，内部定义；是 load + BooEnum.name
     * value: 点击这个Button影响的List<LexiconEnum>
     */
    private static final HashMap<String, List<LexiconEnum>> lexiconMap;
    private int pageNum = 0;
    /**
     * 所有IUIElement的elementId映射，elementId是内部定义（开发者自行约定，非框架规定）；
     */
    private HashMap<String, IUIElement> elementData;
    private UIStrings uiStrings = null;
    private final SpireConfig config;

    private ModLabel pageTitle;
    private ModLabeledButton pageForward;
    private ModLabeledButton pageBackward;
    private ModLabeledButton pageReturn;

    private static final float PAGE_TITLE_X;
    private static final float PAGE_TITLE_Y;
    private static final float ELEMENT_X;
    private static final float ELEMENT_Y;
    private static final List<Float> PADDINGS_Y;
    private static final float PAGE_BUTTON_X1;
    private static final float PAGE_BUTTON_X2;
    private static final float PAGE_BUTTON_Y;

    private static final float LEXICON_X;
    private static final float LEXICON_Y;
    private static final float LEXICON_PAD_X;
    private static final float LEXICON_PAD_Y;
    private static final float BUTTON_DELTA_X1;
    private static final float BUTTON_DELTA_X2;
    private static final float BUTTON_DELTA_Y;
    private static final float WEIGHT_DELTA_X;
    private static final float WEIGHT_DELTA_Y;

    /**
     * 以下是实际使用的参数
     */
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

    /**
     * 这个是遗物对应的词库权重
     * lexiconData Map<RelicName_LexiconName, Weight>>>
     */
    public static HashMap<String, Integer> lexiconData;
    public static HashMap<BookEnum, LexiconEnum> weightedLexicon;
    public static HashMap<BookEnum, List<LexiconEnum>> relicLexicon;

    static {
        pages = new ArrayList<>();
        pages.add(Arrays.asList("darkMode", "pureFont", "fastMode", "casualMode", "ignoreCheck", "showLexicon", "maxAnsNum"));
        List<String> page2 = new ArrayList<>();     // 第二页不能用Arrays.asList 因为预计将修改其内容
        page2.add("loadCET");
        page2.add("loadJLPT");
        page2.add("loadUSER_DICT");
        pages.add(page2);

        configPageNum = 2;
        lexiconMap = new HashMap<>();
        lexiconData = new HashMap<>();
        weightedLexicon = new HashMap<>();
        relicLexicon = new HashMap<>();

        // 只读数据 int range
        intRange = new HashMap<>();
        intRange.put("maxAnsNum", Arrays.asList(1, 3));
    }


    public ModConfigPanel() {
        // init config
        try {
            Properties configDefaults = new Properties();
            for (int i = 0; i < configPageNum; i++) {
                List<String> page = pages.get(i);
                for (String name: page) {
                    Field field = this.getClass().getField(name);
                    configDefaults.put(field.getName(), String.valueOf(field.get(null)));
                }
            }
            // 词典数据
            for (Map.Entry<String, Integer> entry: lexiconData.entrySet()) {
                configDefaults.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            this.config = new SpireConfig(CET46Initializer.MOD_ID, "config", configDefaults);
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new RuntimeException("Failed to set up SpireConfig for " + CET46Initializer.MOD_ID, e);
        }

    }

    /**
     * @return 返回加权后的词库列表,即权重等于元素在列表出现的数量
     */
    public static List<LexiconEnum> getRelicLexicons(BookEnum b) {
        return relicLexicon.getOrDefault(b, new ArrayList<>());
    }

    @Nullable
    public static LexiconEnum getWeightedLexicon(BookEnum b) {
        return weightedLexicon.getOrDefault(b, null);
    }

    /**
     * 初始化界面元素, 配置变量读取由类初始化完成; 须保证调用时languagePack初始化已经完成
     */
    public void receivePostInitialize() {
        this.uiStrings = CardCrawlGame.languagePack.getUIString(CET46Initializer.JSON_MOD_KEY + "ConfigPanel");
        this.elementData = new HashMap<>();
        this.initUIElements();
        this.initRelicPages();
        this.setPage(0);
        this.checkReset();
        this.updateWeights();   // 保证更新词库权重
        this.resetAllQuizRelics();      // 从 initializer 搬过来的
    }

    private void initUIElements() {
        pageTitle = new ModLabel("No Title", PAGE_TITLE_X, PAGE_TITLE_Y, this, (text) -> {});
        // 配置属性
        try {
            for (int i = 0; i < configPageNum; i++) {
                List<String> page = pages.get(i);
                float pagePos = ELEMENT_Y;
                for (String name: page) {
                    // 获取属性
                    Field field = this.getClass().getField(name);
                    IUIElement element = buildElement(field, name);
                    if (element != null) {
                        element.setY(pagePos);
                        this.elementData.put(name, element);
                        // 处理整数部分, 这个时候 element 是 slider, 还有个 label 没有设置位置
                        if (elementData.containsKey(name + "_label")) {
                            elementData.get(name + "_label").set(ELEMENT_X + 40.0F, pagePos);
                            pagePos -= PADDINGS_Y.get(i);
                            element.set(ELEMENT_X + 40.0F, pagePos);
                        }
                        // 更新位置
                        pagePos -= PADDINGS_Y.get(i);
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // 翻页按钮
        pageForward = new ModLabeledButton(">", PAGE_BUTTON_X1, PAGE_BUTTON_Y, Settings.CREAM_COLOR, Color.WHITE,
                FontHelper.cardEnergyFont_L, this, (button) -> {this.nextPage(true);});
        pageBackward = new ModLabeledButton("<", PAGE_BUTTON_X2, PAGE_BUTTON_Y, Settings.CREAM_COLOR, Color.WHITE,
                FontHelper.cardEnergyFont_L, this, (button) -> {this.nextPage(false);});
        pageReturn = new ModLabeledButton(uiStrings.EXTRA_TEXT[1], PAGE_BUTTON_X2, PAGE_BUTTON_Y, Settings.CREAM_COLOR,
                Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
            try {
                if (!this.checkWeights()) {
                    return;
                }
                for (String s: pages.get(this.pageNum)) {
                    if (lexiconData.containsKey(s)) {
                        this.config.setString(s, String.valueOf(lexiconData.get(s)));
                    }
                }
                this.config.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.setPage(1);
        });

    }

    private void initRelicPages() {
        for (Map.Entry<String, List<LexiconEnum>> entry : lexiconMap.entrySet()) {
            IUIElement base = elementData.get(entry.getKey());
            if (base == null) continue;

            boolean isUserDict = entry.getKey().equals("loadUSER_DICT");

            // Level 1 -> Level 2 跳转按钮
            String btnText = isUserDict ? "Manage" : uiStrings.EXTRA_TEXT[0];
            int targetIndex = pages.size();
            
            ModLabeledButton jumpBtn = new ModLabeledButton(btnText, Math.max(base.getX() + 200.0F, 1000.0F), base.getY() - 2.0F,
                    Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> { this.setPage(targetIndex); });
            elementData.put(entry.getKey() + "_jump", jumpBtn);
            pages.get(1).add(entry.getKey() + "_jump");

            ModLabel displayLabel = new ModLabel("", base.getX() + 25.0F, base.getY() - PADDINGS_Y.get(0), this, (text) -> {});
            elementData.put(entry.getKey() + "_display", displayLabel);
            pages.get(1).add(entry.getKey() + "_display");

            // --- 开始构建 Level 2 页面 ---
            List<String> level2PageIds = new ArrayList<>();

            if (isUserDict) {
                // ================== USER_DICT (APKG) 逻辑 ==================
                File dir = new File(CET46Initializer.USER_DICT_PATH);
                File[] files = dir.listFiles((d, n) -> n.endsWith(".apkg"));
                
                if (files != null) {
                    // 1. 预计算部分：计算页码偏移量
                    // Level 2 每页 4 个文件
                    int filesPerPage = 4;
                    int totalFiles = files.length;
                    int totalL2Pages = (int) Math.ceil((double)totalFiles / filesPerPage);
                    if (totalL2Pages == 0) totalL2Pages = 1;
                    
                    // Level 3 的起始页码 = 当前页数 + Level 2 总页数
                    int l3StartBaseIndex = pages.size() + totalL2Pages;
                    
                    // 预先计算每个文件对应的 Level 3 跳转目标页码
                    int[] fileToL3PageIndex = new int[totalFiles];
                    int currentL3Accumulator = l3StartBaseIndex;
                    
                    // 缓存每个文件的结构，避免重复读取
                    AnkiMetadataReader.AnkiStructure[] structs = new AnkiMetadataReader.AnkiStructure[totalFiles];
                    
                    for (int i = 0; i < totalFiles; i++) {
                        structs[i] = AnkiMetadataReader.readStructure(files[i]);
                        fileToL3PageIndex[i] = currentL3Accumulator;
                        
                        // 计算该文件需要多少个 Level 3 子页面 (每页4个字段)
                        int fieldsCount = structs[i].fieldNames.size();
                        int subPages = (int) Math.ceil((double)fieldsCount / 4);
                        if (subPages == 0) subPages = 1;
                        currentL3Accumulator += subPages;
                    }

                    // 2. 生成 Level 2 页面 (词典列表)
                    for (int p = 0; p < totalL2Pages; p++) {
                        List<String> l2SubPage = new ArrayList<>();
                        int startFile = p * filesPerPage;
                        int endFile = Math.min(startFile + filesPerPage, totalFiles);
                        
                        // 页面标题
                        String titleKey = "ud_l2_title_" + p;
                        elementData.put(titleKey, new ModLabel("User Dictionaries (Page " + (p+1) + "/" + totalL2Pages + ")", 360.0F, 810.0F, Color.GOLD, this, (l)->{}));
                        l2SubPage.add(titleKey);

                        for (int i = startFile; i < endFile; i++) {
                            File apkgFile = files[i];
                            int idxInPage = i - startFile;
                            
                            // 布局：2行 x 2列
                            // 行 0: Y=750, 行 1: Y=400 (间距 350)
                            // 列 0: X=400, 列 1: X=1000
                            float x = 400.0F + (idxInPage % 2) * 600.0F;
                            float y = 700.0F - (idxInPage / 2) * 200.0F;
                            
                            String fileId = "user_apkg_" + i;

                            // (1) 文件名
                            elementData.put(fileId + "_name", new ModLabel(apkgFile.getName(), x, y, Color.GOLD, this, (l) -> {}));
                            l2SubPage.add(fileId + "_name");

                            // (2) 权重配置
                            String fName = apkgFile.getName();
                            String nameNoExt = fName.substring(0, fName.lastIndexOf('.'));
                            String wKey = entry.getKey() + "_" + nameNoExt;
                            if (!config.has(wKey)) config.setString(wKey, "1");
                            
                            float wY = y - 70.0F; 
                            
                            // [-] 按钮 (修复：添加 config.save)
                            elementData.put(wKey + "_sub", new ModLabeledButton("-", x, wY, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                                lexiconData.put(wKey, Math.max(0, lexiconData.get(wKey) - 1));
                                ((ModLabel) elementData.get(wKey)).text = lexiconData.get(wKey).toString();
                                try {
                                    config.setString(wKey, lexiconData.get(wKey).toString());
                                    config.save();
                                } catch (Exception e) { e.printStackTrace(); }
                            }));
                            l2SubPage.add(wKey + "_sub");

                            // 数值
                            elementData.put(wKey, new ModLabel(lexiconData.get(wKey).toString(), x + 100.0F, wY + 10, this, (l) -> {}));
                            l2SubPage.add(wKey);

                            // [+] 按钮 (修复：添加 config.save)
                            elementData.put(wKey + "_add", new ModLabeledButton("+", x + 140.0F, wY, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                                lexiconData.put(wKey, Math.min(10, lexiconData.get(wKey) + 1));
                                ((ModLabel) elementData.get(wKey)).text = lexiconData.get(wKey).toString();
                                try {
                                    config.setString(wKey, lexiconData.get(wKey).toString());
                                    config.save();
                                } catch (Exception e) { e.printStackTrace(); }
                            }));
                            l2SubPage.add(wKey + "_add");

                            // (3) Config 按钮 (下移 120)
                            int targetPage = fileToL3PageIndex[i];
                            ModLabeledButton cfgBtn = new ModLabeledButton("Config Mapping", x, y - 140.0F, Color.SKY, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> {
                                this.setPage(targetPage);
                            });
                            elementData.put(fileId + "_cfg", cfgBtn);
                            l2SubPage.add(fileId + "_cfg");
                        }
                        
                        // Level 2 翻页按钮
                        if (p > 0) {
                            String prevKey = "ud_l2_prev_" + p;
                            int targetP = targetIndex + p - 1;
                            elementData.put(prevKey, new ModLabeledButton("< Prev Page", 450.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetP)));
                            l2SubPage.add(prevKey);
                        }
                        if (p < totalL2Pages - 1) {
                            String nextKey = "ud_l2_next_" + p;
                            int targetP = targetIndex + p + 1;
                            elementData.put(nextKey, new ModLabeledButton("Next Page >", 1150.0F, 280.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(targetP)));
                            l2SubPage.add(nextKey);
                        }

                        pages.add(l2SubPage);
                    }

                    // 3. 生成 Level 3 页面 (字段映射)
                    for (int i = 0; i < totalFiles; i++) {
                        File apkgFile = files[i];
                        AnkiMetadataReader.AnkiStructure struct = structs[i]; // 使用缓存
                        String configKey = "mapping_" + apkgFile.getName();
                        if (!config.has(configKey)) config.setString(configKey, "0|1|-1");
                        
                        int l3FieldsPerPage = 4; // 限制为 4 个字段
                        int totalFields = struct.fieldNames.size();
                        int totalSubPages = (int) Math.ceil((double)totalFields / l3FieldsPerPage);
                        if (totalSubPages == 0) totalSubPages = 1;
                        
                        int thisFileStartPageIdx = fileToL3PageIndex[i];

                        for (int p = 0; p < totalSubPages; p++) {
                            List<String> subPage = new ArrayList<>();
                            int startF = p * l3FieldsPerPage;
                            int endF = Math.min(startF + l3FieldsPerPage, totalFields);

                            // 标题
                            String pgTitleKey = configKey + "_pg_title_" + p;
                            elementData.put(pgTitleKey, new ModLabel(apkgFile.getName() + " (Page " + (p+1) + "/" + totalSubPages + ")", 360.0F, 810.0F, Color.GOLD, this, (l)->{}));
                            subPage.add(pgTitleKey);

                            for (int j = startF; j < endF; j++) {
                                final int fIdx = j;
                                int rowInPage = j - startF;
                                // Y轴: 750, 640, 530, 420. 不会遮挡底部 280
                                float rowY = 750.0F - rowInPage * 110.0F;

                                // 字段名 + 预览
                                String nKey = configKey + "_" + j + "_n";
                                elementData.put(nKey, new ModLabel(j + ". " + struct.fieldNames.get(j), 360.0F, rowY, Color.WHITE, this, (l)->{}));
                                subPage.add(nKey);

                                String pKey = configKey + "_" + j + "_p";
                                String previewText = struct.sampleValues.size() > j ? struct.sampleValues.get(j) : "";
                                if (previewText.length() > 25) previewText = previewText.substring(0, 25) + "...";
                                elementData.put(pKey, new ModLabel("   > " + previewText, 360.0F, rowY - 35.0F, Color.GRAY, this, (l)->{}));
                                subPage.add(pKey);

                                // 按钮组: Question / Answer / Sound
                                // Question (Radio) - X: 750
                                String qKey = configKey + "_" + j + "_q";
                                elementData.put(qKey, new ModLabeledToggleButton("Question", 750.0F, rowY, Color.WHITE, FontHelper.cardDescFont_N, config.getString(configKey).startsWith(j + "|"), this, (l)->{}, (btn) -> {
                                    if (btn.enabled) {
                                        for (String k : elementData.keySet()) {
                                            if (k.startsWith(configKey) && k.endsWith("_q") && !k.equals(qKey)) {
                                                ((ModLabeledToggleButton)elementData.get(k)).toggle.enabled = false;
                                            }
                                        }
                                        String[] pts = config.getString(configKey).split("\\|");
                                        config.setString(configKey, fIdx + "|" + (pts.length > 1 ? pts[1] : "1") + "|" + (pts.length > 2 ? pts[2] : "-1"));
                                        try { config.save(); } catch (Exception e) {}
                                    } else btn.enabled = true;
                                }));
                                subPage.add(qKey);

                                // Answer (Checkbox) - X: 980
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

                                // Sound (Radio) - X: 1210
                                String sKey = configKey + "_" + j + "_s";
                                boolean isS = config.getString(configKey).endsWith("|" + j);
                                elementData.put(sKey, new ModLabeledToggleButton("Sound", 1210.0F, rowY, Color.WHITE, FontHelper.cardDescFont_N, isS, this, (l)->{}, (btn) -> {
                                    if (btn.enabled) {
                                        for (String k : elementData.keySet()) {
                                            if (k.startsWith(configKey) && k.endsWith("_s") && !k.equals(sKey)) {
                                                ((ModLabeledToggleButton)elementData.get(k)).toggle.enabled = false;
                                            }
                                        }
                                        String[] pts = config.getString(configKey).split("\\|");
                                        config.setString(configKey, pts[0] + "|" + pts[1] + "|" + fIdx);
                                    } else {
                                        String[] pts = config.getString(configKey).split("\\|");
                                        if (pts.length > 2 && pts[2].equals(String.valueOf(fIdx))) {
                                            config.setString(configKey, pts[0] + "|" + pts[1] + "|-1");
                                        }
                                    }
                                    try { config.save(); } catch (Exception e) {}
                                }));
                                subPage.add(sKey);
                            }

                            // Level 3 侧边翻页按钮
                            float NAV_X = 1450.0F;
                            if (p > 0) {
                                int prevPageIdx = thisFileStartPageIdx + p - 1;
                                String prevBtnKey = configKey + "_pgbtn_" + p + "_prev";
                                elementData.put(prevBtnKey, new ModLabeledButton("Prev", NAV_X, 600.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(prevPageIdx)));
                                subPage.add(prevBtnKey);
                            }
                            if (p < totalSubPages - 1) {
                                int nextPageIdx = thisFileStartPageIdx + p + 1;
                                String nextBtnKey = configKey + "_pgbtn_" + p + "_next";
                                elementData.put(nextBtnKey, new ModLabeledButton("Next", NAV_X, 400.0F, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (b) -> this.setPage(nextPageIdx)));
                                subPage.add(nextBtnKey);
                            }

                            pages.add(subPage);
                        }
                    }
                }
            } else {
                // ================== 原有内置词典逻辑 (保持不变) ==================
                for (int i = 0; i < entry.getValue().size(); i++) {
                    LexiconEnum l = entry.getValue().get(i);
                    String tmp_name = entry.getKey() + "_" + l.name();
                    if (!config.has(tmp_name)) config.setString(tmp_name, "1");
                    lexiconData.put(tmp_name, Integer.parseInt(this.config.getString(tmp_name)));

                    float x = LEXICON_X + LEXICON_PAD_X * (float) (i % 3);
                    float y = LEXICON_Y + LEXICON_PAD_Y * (float) (i / 3);

                    IUIElement nameLabel = new ModLabel(uiStrings.TEXT_DICT.getOrDefault(l.name(), l.name()), x, y, this, (label) -> {});
                    level2PageIds.add(tmp_name + "_name");
                    elementData.put(tmp_name + "_name", nameLabel);

                    level2PageIds.add(tmp_name + "_add");
                    elementData.put(tmp_name + "_add", new ModLabeledButton("+", x + BUTTON_DELTA_X1, y + BUTTON_DELTA_Y, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
                        lexiconData.put(tmp_name, Math.min(10, lexiconData.get(tmp_name) + 1));
                        ((ModLabel) elementData.get(tmp_name)).text = lexiconData.get(tmp_name).toString();
                    }));

                    level2PageIds.add(tmp_name + "_sub");
                    elementData.put(tmp_name + "_sub", new ModLabeledButton("-", x + BUTTON_DELTA_X2, y + BUTTON_DELTA_Y, Settings.CREAM_COLOR, Color.WHITE, FontHelper.cardEnergyFont_L, this, (button) -> {
                        lexiconData.put(tmp_name, Math.max(0, lexiconData.get(tmp_name) - 1));
                        ((ModLabel) elementData.get(tmp_name)).text = lexiconData.get(tmp_name).toString();
                    }));

                    level2PageIds.add(tmp_name);
                    elementData.put(tmp_name, new ModLabel(lexiconData.get(tmp_name).toString(), x + WEIGHT_DELTA_X, y + WEIGHT_DELTA_Y, this, (label) -> {}));
                }
                pages.add(level2PageIds);
            }
            logger.info("Successfully initialize page for: {}", entry.getKey());
        }
    }

    public static void addRelicPage(BookEnum relicBook, List<LexiconEnum> list, int default_) {
        relicLexicon.put(relicBook, new ArrayList<>());
        // varName是挂靠的字段名, 需要在elementData里面, 否则没有效果
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
        if (pages.isEmpty()) {
            return;
        }
        if (id < 0) {
            id = pages.size() - 1;
        }
        id %= pages.size();

        this.pageNum = id;
        List<IUIElement> pageElements = new ArrayList<>();
        if (uiStrings != null && id < uiStrings.TEXT.length && !uiStrings.TEXT[id].isEmpty()) {
            this.pageTitle.text = uiStrings.TEXT[id];
            pageElements.add(this.pageTitle);
        }
        for (String name: pages.get(id)) {
            pageElements.add(this.elementData.get(name));
            // 哪个智障设计的, 补丁打到这里来了; 啊 原来我是智障啊
            if (this.elementData.containsKey(name + "_label")) {
                pageElements.add(this.elementData.get(name + "_label"));
            }
        }
        if (this.pageNum < configPageNum) {
            // 翻页按钮
            pageElements.add(this.pageForward);
            pageElements.add(this.pageBackward);
        } else {
            // 返回按钮
            pageElements.add(this.pageReturn);
        }
        this.resetElements(pageElements);
        // update weight display
        if (id == 1) {
            this.updateWeights();
        }
    }

    private IUIElement buildElement(Field field, String name) throws IllegalAccessException {
        if (field.getType() == boolean.class) {
            // load
            field.set(null, Boolean.parseBoolean(this.config.getString(field.getName())));
            return new ModLabeledToggleButton(uiStrings.TEXT_DICT.getOrDefault(name, name), ELEMENT_X, 0.0F,
                    Settings.CREAM_COLOR, FontHelper.charDescFont, (Boolean)field.get(null), this, (label) -> {},
                    (button) -> {saveVar(button.enabled, field, s -> {field.set(null, Boolean.parseBoolean(s));});});
        }
        if (field.getType() == int.class) {
            // load
            field.set(null, Integer.parseInt(this.config.getString(field.getName())));
            // label, 这块处理的有点糟糕了
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

    /**
     * 用来避免update迭代时修改元素
     */
    private boolean updateStop = false;
    private List<IUIElement> tmpCache = null;
    private void resetElements(List<IUIElement> list) {
        this.tmpCache = list;
        this.updateStop = true;
    }
    private void checkReset() {
        if (!this.updateStop) {
            return;
        }
        this.getUpdateElements().clear();
        this.getRenderElements().clear();
        if (tmpCache != null) {
            for (IUIElement element: tmpCache) {
                this.addUIElement(element);
            }
            tmpCache = null;
        }
        this.updateStop = false;
    }

    @Override
    public void update() {
        for (IUIElement element: this.getUpdateElements()) {
            element.update();
        }
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
            // 将面板返回首页, 放在前面那个逻辑块就不行, but why?
            this.setPage(0);
            this.checkReset();
            this.resetAllQuizRelics();
        }

    }

    /**
     * 在 RelicLibrary 中更新所有位于 specialList 的 QuizRelic 的图片和描述
     */
    public void resetAllQuizRelics() {
        for (AbstractRelic r: RelicLibrary.specialList) {
            if (r instanceof QuizRelic) {
                ((QuizRelic) r).resetTexture();
                ((QuizRelic) r).resetDescription();
            }
        }
    }

    /**
     * relicLexicon和weightedLexicon被赋值
     */
    private void updateWeights() {
        // 开摆了, 直接一坨循环搞定得了, 自己搓优化不如求大佬重写逻辑
        for (Map.Entry<String, List<LexiconEnum>> entry: lexiconMap.entrySet()) {
            IUIElement element = elementData.getOrDefault(entry.getKey() + "_display", null);
            if (!(element instanceof ModLabel)) {
                continue;
            }
            int total = 0;
            int max = 0;    // 用于记录最大权重, 在下一个循环获取第一次出现的最大lexicon, 这样就不需要使用pair
            List<LexiconEnum> notZeroList = new ArrayList<>();
            for (LexiconEnum l: entry.getValue()) {
                int tmp = lexiconData.getOrDefault(entry.getKey() + "_" + l.name(), 0);
                if (tmp != 0) {
                    notZeroList.add(l);
                    total += tmp;
                }
                if (tmp > max) {
                    max = tmp;
                }
            }
            float k = total == 0 ? 0.0F : 100.0F / total;
            StringBuilder sb = new StringBuilder();
            BookEnum relic = BookEnum.valueOf(entry.getKey().substring(4));     // 去掉前面的 load
            // 更新权重列表
            ArrayList<LexiconEnum> lexicons = (ArrayList<LexiconEnum>) getRelicLexicons(relic);
            lexicons.clear();

            LexiconEnum weighted = null;
            // update text
            for (LexiconEnum l: notZeroList) {
                int tmp = lexiconData.get(entry.getKey() + "_" + l.name());
                if (weighted == null && tmp == max) {
                    weighted = l;
                }
                for (int i = 0; i < tmp; i++) {
                    lexicons.add(l);    // 添加权重个词库
                }
                sb.append(uiStrings.TEXT_DICT.getOrDefault(l.name(), l.name())).append(": ");
                sb.append(Math.round(k * tmp)).append("%. ");
            }
            relicLexicon.put(relic, lexicons);
            ((ModLabel) element).text = sb.toString();
            weightedLexicon.put(relic, weighted);   // 注意 weighted 有可能是 null
            logger.info("Weights updated.");
            logLexicons();

        }
    }

    /**
     * debug, 输出目前所有加权词库列表和使用的最大权词库
     */
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
            if (e instanceof ModLabel) {
                ((ModLabel) e).color = c;
            }
        }
    }

    static {
        PAGE_TITLE_X = 360.0F;
        PAGE_TITLE_Y = 815.0F;
        ELEMENT_X = 355.0F;
        ELEMENT_Y = 730.0F;
        PADDINGS_Y = Arrays.asList(55.0F, 125.0F);
        PAGE_BUTTON_X1 = 1015.0F;
        PAGE_BUTTON_X2 = 815.0F;
        PAGE_BUTTON_Y = 280.0F;

        LEXICON_X = 380.0F;
        LEXICON_Y = 720.0F;
        LEXICON_PAD_X = 400.0F;
        LEXICON_PAD_Y = -160.0F;
        BUTTON_DELTA_X1 = 200.0F;
        BUTTON_DELTA_X2 = 0.0F;
        BUTTON_DELTA_Y = -80.0F;
        WEIGHT_DELTA_X = 120.0F;
        WEIGHT_DELTA_Y = -50.0F;
    }
}
