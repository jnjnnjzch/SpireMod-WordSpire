package CET46InSpire;

import CET46InSpire.events.CallOfCETEvent.BookEnum;
import CET46InSpire.helpers.QuizCommand;
import CET46InSpire.relics.TestCET;
import CET46InSpire.relics.UserDictRelic;
import CET46InSpire.relics.JLPTRelic;
import CET46InSpire.helpers.AnkiPackageLoader;
import CET46InSpire.helpers.BookConfig;
import CET46InSpire.helpers.BookConfig.LexiconEnum;
import CET46InSpire.ui.ModConfigPanel;
import basemod.BaseMod;
import basemod.DevConsole;
import basemod.ModPanel;
import basemod.devcommands.ConsoleCommand;
import basemod.helpers.RelicType;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import CET46InSpire.screens.QuizScreen;
import CET46InSpire.helpers.ImageElements;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File; //不得不用io File来解决初始化问题
import java.io.IOException;

@SpireInitializer
public class CET46Initializer implements
        EditRelicsSubscriber,
        EditStringsSubscriber,
        PostInitializeSubscriber {
    private static final Logger logger = LogManager.getLogger(CET46Initializer.class.getName());
    public static String MOD_ID = "CET46InSpire";  //MOD_ID必须与ModTheSpire.json中的一致
    public static String JSON_MOD_KEY = "CET46:";
    private ModPanel settingsPanel = null;
    
    // 外部词库的存放路径
    public static final String USER_DICT_PATH = "mods/CET46InSpire/UserDictionaries/";

    /**
     * 所有可选范围
     */
    public static Map<BookEnum, BookConfig> allBooks = new HashMap<>();
    /**
     * 用户已选择范围
     */
    public static Set<BookConfig> userBooks = new HashSet<>();
    /**
     * 需要加载的范围, 指词库范围
     */
    public static Set<LexiconEnum> needLoadBooks = new HashSet<LexiconEnum>();
    static {
        // test
        allBooks.put(BookEnum.JLPT, new BookConfig(BookEnum.JLPT,
                Arrays.asList(LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5), () -> new JLPTRelic()));
        allBooks.put(BookEnum.CET, new BookConfig(BookEnum.CET, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6), () -> new TestCET()));
//        allBooks.put(BookEnum.CET4, new BookConfig(BookEnum.CET4, new ArrayList<>(), () -> new BookOfCET4()));
//        allBooks.put(BookEnum.CET6, new BookConfig(BookEnum.CET6, Arrays.asList(BookEnum.CET4), () -> new BookOfCET6()));
//        allBooks.put(BookEnum.N5, new BookConfig(BookEnum.N5, new ArrayList<>(), () -> new BookOfJlpt(BookEnum.N5, ImageElements.RELIC_N5_IMG)));
//        // TODO 使用对应的Texture
//        allBooks.put(BookEnum.N4, new BookConfig(BookEnum.N4, Arrays.asList(BookEnum.N5), () -> new BookOfJlpt(BookEnum.N4, ImageElements.RELIC_N5_IMG)));
//        allBooks.put(BookEnum.N3, new BookConfig(BookEnum.N3, Arrays.asList(BookEnum.N4), () -> new BookOfJlpt(BookEnum.N3, ImageElements.RELIC_N5_IMG)));
//        allBooks.put(BookEnum.N2, new BookConfig(BookEnum.N2, Arrays.asList(BookEnum.N3), () -> new BookOfJlpt(BookEnum.N2, ImageElements.RELIC_N5_IMG)));
//        allBooks.put(BookEnum.N1, new BookConfig(BookEnum.N1, Arrays.asList(BookEnum.N2), () -> new BookOfJlpt(BookEnum.N1, ImageElements.RELIC_N5_IMG)));
        
        // 加载用户自动的词库
        initUserDictionaries();
    }

    private static void initUserDictionaries() {
        List<LexiconEnum> userLexicons = new ArrayList<>();
        try {
            // 使用  java.io.File读取游戏根目录下的文件
            File dir = new File(USER_DICT_PATH);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        String name = null;

                        // 1. 原有的 JSON 逻辑
                        if (fileName.endsWith(".json")) {
                            name = fileName.substring(0, fileName.lastIndexOf('.'));
                            logger.info("CET46InSpire: Found json file ->" + name);
                        } 
                        // 2. 新增：Anki 包逻辑
                        else if (fileName.endsWith(".apkg")) {
                            name = fileName.substring(0, fileName.lastIndexOf('.'));
                            // 可以给 Anki 文件加个前缀或者特殊标记，不过为了统一，直接用文件名也行
                            logger.info("CET46InSpire: Found Anki Deck -> " + name);
                        }

                        if (name != null) {
                            userLexicons.add(LexiconEnum.of(name));
                        }
                    }
                }
            } else {
                // 如果目录不存在，自动创建它，方便用户
                dir.mkdirs();
            }
        } catch (Exception e) {
            logger.error("CET46InSpire: Failed to scan user dictionaries", e);
        }

        // 注册 USER_DICT 书籍
        if (!userLexicons.isEmpty()) {
            allBooks.put(BookEnum.USER_DICT, new BookConfig(BookEnum.USER_DICT, 
                    userLexicons, () -> new UserDictRelic())); // 新的自定义词库对应的遗物
        }
    }

    private static void initBooks() {
        CET46Initializer.allBooks.values().forEach(bookConfig -> {
            if (bookConfig.needNotLoad()) {
                return;
            }
            userBooks.add(bookConfig);
//            needLoadBooks.add(bookConfig.bookEnum);
            needLoadBooks.addAll(bookConfig.lexicons);
        });
        // test
//        ModConfigPanel.addRelicPage(BookEnum.CET4, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6));
        ModConfigPanel.addRelicPage(BookEnum.CET, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6));
        ModConfigPanel.addRelicPage(BookEnum.JLPT, Arrays.asList(LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5));
        // 为USER_DICT注册
        if (allBooks.containsKey(BookEnum.USER_DICT)) {
             ModConfigPanel.addRelicPage(BookEnum.USER_DICT, allBooks.get(BookEnum.USER_DICT).lexicons);
        }
        logger.info("initBooks: userBooks = {}, needLoadBooks = {}.", userBooks.stream().map(it -> it.bookEnum).collect(Collectors.toList()), needLoadBooks);
    }

    public CET46Initializer() {
        logger.info("Initialize: {}", MOD_ID);
        BaseMod.subscribe(this);
//        settingsPanel = new CET46Panel("config");
        initBooks();
        // 放在init books 后面来保证注册遗物设置页面成功
        settingsPanel = new ModConfigPanel();
    }

    public static void initialize() {
        new CET46Initializer();
    }

    @Override
    public void receiveEditRelics() {
        CET46Initializer.userBooks.forEach(bookConfig -> {
            AbstractRelic relic = bookConfig.relicSupplier.get();
            BaseMod.addRelic(relic, RelicType.SHARED);
            UnlockTracker.markRelicAsSeen(relic.relicId);
        });
    }

    @Override
    public void receiveEditStrings() {
        String lang = "eng";
        if (Objects.requireNonNull(Settings.language) == Settings.GameLanguage.ZHS) {
            lang = "zhs";
        }
        if (Objects.requireNonNull(Settings.language) == Settings.GameLanguage.ZHT) {
            lang = "zhs";
        }

        BaseMod.loadCustomStringsFile(EventStrings.class, "CET46Resource/localization/events_" + lang + ".json");
        BaseMod.loadCustomStringsFile(PowerStrings.class, "CET46Resource/localization/powers_" + lang + ".json");
        BaseMod.loadCustomStringsFile(RelicStrings.class, "CET46Resource/localization/relics_" + lang + ".json");
        BaseMod.loadCustomStringsFile(UIStrings.class, "CET46Resource/localization/ui_" + lang + ".json");

        loadVocabulary();
    }

    public void loadVocabulary() {
        long startTime = System.currentTimeMillis();

        // jar内部的词典定义
        List<LexiconEnum> builtIn = Arrays.asList(
            LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5,
            LexiconEnum.CET4, LexiconEnum.CET6
            );

        needLoadBooks.forEach(lexiconEnum -> {
            if (builtIn.contains(lexiconEnum)) { // 首先读取jar内部的词典，定义如上
                BaseMod.loadCustomStringsFile(UIStrings.class, "CET46Resource/vocabulary/" + lexiconEnum.name() + ".json");
            } 
            else { // 读取外部的用户词典
                try {
                    String name = lexiconEnum.name();
                    File jsonFile = new File(USER_DICT_PATH + name + ".json");
                    File ankiFile = new File(USER_DICT_PATH + name + ".apkg");

                    if (jsonFile.exists()) { //读取 json
                        String jsonContent = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()), "UTF-8");
                        BaseMod.loadCustomStrings(UIStrings.class, jsonContent);
                        logger.info("Loaded external JSON: " + name);
                    } 
                    else if (ankiFile.exists()) { //读取 anki
                        logger.info("Star loading anki file:" + name);
                        File audioOutDir = new File(USER_DICT_PATH + "audio/");
                        SpireConfig tempConfig = new SpireConfig(CET46Initializer.MOD_ID, "config");
                        List<AnkiPackageLoader.AnkiRawCard> cards = AnkiPackageLoader.loadFromApkg(ankiFile, audioOutDir, tempConfig);
                        
                        Map<String, Object> localizationMap = new HashMap<>();
                        // info字段，就是有多少anki卡片
                        Map<String, Object> infoData = new HashMap<>();
                        List<String> infoText = new ArrayList<>();
                        infoText.add(String.valueOf(cards.size())); // TEXT[0] 放数量
                        infoData.put("TEXT", infoText);
                        localizationMap.put(JSON_MOD_KEY + name + "_info", infoData);

                        // 开始读每张卡
                        for (int i = 0; i < cards.size(); i++) {
                            AnkiPackageLoader.AnkiRawCard card = cards.get(i);
                            String wordId = JSON_MOD_KEY + name + "_" + i; 

                            // 构建 UIString=
                            // 构建TEXT
                            Map<String, Object> wordData = new HashMap<>();
                            List<String> textList = new ArrayList<>();
                            textList.add(card.front); // 第一段是题面
                            for (String backItem : card.backList) { //之后是答案。
                                textList.add(backItem);
                            }
                            wordData.put("TEXT", textList);
                            
                            // 构建TEXT_DICT
                            if (card.audio != null && !card.audio.isEmpty()) {
                                Map<String, String> extraData = new HashMap<>();
                                extraData.put("AUDIO", card.audio); // 加入AUDIO字段
                                wordData.put("TEXT_DICT", extraData);
                            }
                            // 完成这个单词的构建，放入localizationMap
                            localizationMap.put(wordId, wordData);
                        }
                        Gson gson = new Gson();
                        String generatedJson = gson.toJson(localizationMap);
                        // 完成写入
                        BaseMod.loadCustomStrings(UIStrings.class, generatedJson);
                        logger.info("Converted Anki Deck to Spire JSON (" + cards.size() + " cards)");
                    }
                } catch (Exception e) {
                    logger.error("Failed to load external vocabulary: " + lexiconEnum.name(), e);
                }
            }
        });
        logger.info("Vocabulary load time: {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void receivePostInitialize() {
        BookConfig.init_map();
        ((ModConfigPanel) settingsPanel).receivePostInitialize();
        BaseMod.registerModBadge(ImageElements.MOD_BADGE,
                "CET46 In Spire", "__name__, Dim", "Do_not_forget_CET46!", settingsPanel);

        BaseMod.addCustomScreen(new QuizScreen());

        // 注册命令
        ConsoleCommand.addCommand("quiz", QuizCommand.class);
        // 修改 BaseMod 控制台字体
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("CET46Resource/font/VictorMono-Medium.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = (int)(22.0F * Settings.scale);
        DevConsole.consoleFont = generator.generateFont(parameter);
        generator.dispose();
    }

}
