package WordSpire;

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

import WordSpire.events.CallOfCETEvent.BookEnum;
import WordSpire.helpers.AnkiPackageLoader;
import WordSpire.helpers.BookConfig;
import WordSpire.helpers.ImageElements;
import WordSpire.helpers.QuizCommand;
import WordSpire.helpers.BookConfig.LexiconEnum;
import WordSpire.relics.BuildQuizDataRequest;
import WordSpire.relics.JLPTRelic;
import WordSpire.relics.TestCET;
import WordSpire.relics.UserDictRelic;
import WordSpire.screens.QuizScreen;
import WordSpire.ui.ModConfigPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File; //不得不用io File来解决初始化问题
import java.io.IOException;

@SpireInitializer
public class WordSpireInitializer implements
        EditRelicsSubscriber,
        EditStringsSubscriber,
        PostInitializeSubscriber {
    private static final Logger logger = LogManager.getLogger(WordSpireInitializer.class.getName());
    public static String MOD_ID = "WordSpire";  //MOD_ID必须与ModTheSpire.json中的一致
    public static String JSON_MOD_KEY = "CET46:";
    private ModPanel settingsPanel = null;
    
    // 外部词库的存放路径
    public static final String USER_DICT_PATH = "mods/WordSpire/";

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
        // allBooks.put(BookEnum.JLPT, new BookConfig(BookEnum.JLPT,
        //         Arrays.asList(LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5), () -> new JLPTRelic()));
        // allBooks.put(BookEnum.CET, new BookConfig(BookEnum.CET, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6), () -> new TestCET()));
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

    public static void initUserDictionaries() {
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
                            logger.info("WordSpire: Found json file ->" + name);
                        } 
                        // 2. 新增：Anki 包逻辑
                        else if (fileName.endsWith(".apkg")) {
                            name = fileName.substring(0, fileName.lastIndexOf('.'));
                            // 可以给 Anki 文件加个前缀或者特殊标记，不过为了统一，直接用文件名也行
                            logger.info("WordSpire: Found Anki Deck -> " + name);
                        }

                        if (name != null) {
                            userLexicons.add(LexiconEnum.of(name));
                        }
                    }
                }
            } else {
                // 如果目录不存在，自动创建它，方便用户
                dir.mkdirs();
                createReadme(dir);
            }
        } catch (Exception e) {
            logger.error("WordSpire: Failed to scan user dictionaries", e);
        }

        // 注册 USER_DICT 书籍
        if (!userLexicons.isEmpty()) {
            allBooks.put(BookEnum.USER_DICT, new BookConfig(BookEnum.USER_DICT, 
                    userLexicons, () -> new UserDictRelic())); // 新的自定义词库对应的遗物
        }
    }

    private static void createReadme(File dir) {
        File readme = new File(dir, "README.txt");
        try {
            // 如果说明书不存在，就创建它
            if (!readme.exists() && readme.createNewFile()) {
                String content = "Put your Anki files (.apkg) or JSON dictionaries (.json) in this folder.\n" +
                        "The file name will be used as the dictionary name in the game.\n\n" +
                        "请将你的 Anki 导出文件 (.apkg) 或 JSON 词库 (.json) 放在这个文件夹内。\n" +
                        "文件名将直接作为游戏中显示的词库名称。\n\n" +
                        "Supported formats / 支持格式:\n" +
                        "1. .apkg (Anki Deck Export / Anki 牌组导出)\n" +
                        "2. .json (Custom Format / 自定义格式)";
                java.nio.file.Files.write(readme.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                logger.info("WordSpire: Created README.txt in user dictionary folder.");
            }
        } catch (IOException e) {
            logger.error("WordSpire: Failed to create README.txt", e);
        }
    }

    private static void initBooks() {
         WordSpireInitializer.allBooks.values().forEach(bookConfig -> {
            if (bookConfig.needNotLoad()) {
                return;
            }
            userBooks.add(bookConfig);
//            needLoadBooks.add(bookConfig.bookEnum);
            needLoadBooks.addAll(bookConfig.lexicons);
        });
        // test
//        ModConfigPanel.addRelicPage(BookEnum.CET4, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6));
        // ModConfigPanel.addRelicPage(BookEnum.CET, Arrays.asList(LexiconEnum.CET4, LexiconEnum.CET6));
        // ModConfigPanel.addRelicPage(BookEnum.JLPT, Arrays.asList(LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5));
        // 为USER_DICT注册
        if (allBooks.containsKey(BookEnum.USER_DICT)) {
             ModConfigPanel.addRelicPage(BookEnum.USER_DICT, allBooks.get(BookEnum.USER_DICT).lexicons);
        }
        logger.info("initBooks: userBooks = {}, needLoadBooks = {}.", userBooks.stream().map(it -> it.bookEnum).collect(Collectors.toList()), needLoadBooks);
    }

    public  WordSpireInitializer() {
        logger.info("Initialize: {}", MOD_ID);
        BaseMod.subscribe(this);
//        settingsPanel = new CET46Panel("config");
        initBooks();
        // 放在init books 后面来保证注册遗物设置页面成功
        settingsPanel = new ModConfigPanel();
    }

    public static void initialize() {
        new  WordSpireInitializer();
    }

    @Override
    public void receiveEditRelics() {
         WordSpireInitializer.userBooks.forEach(bookConfig -> {
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
        // List<LexiconEnum> builtIn = Arrays.asList(
        //     LexiconEnum.N1, LexiconEnum.N2, LexiconEnum.N3, LexiconEnum.N4, LexiconEnum.N5,
        //     LexiconEnum.CET4, LexiconEnum.CET6
        //     );

        needLoadBooks.forEach(lexiconEnum -> {
            // if (builtIn.contains(lexiconEnum)) { // 首先读取jar内部的词典，定义如上
            //     BaseMod.loadCustomStringsFile(UIStrings.class, "CET46Resource/vocabulary/" + lexiconEnum.name() + ".json");
            // } 
            // else { // 读取外部的用户词典
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
                        SpireConfig tempConfig = new SpireConfig( WordSpireInitializer.MOD_ID, "config");
                        List<AnkiPackageLoader.AnkiRawCard> cards = AnkiPackageLoader.loadFromApkg(ankiFile, audioOutDir, tempConfig);
                        
                        if (cards != null && !cards.isEmpty()) {
                            // 1. 获取枚举/对象 (根据你的重构，这里可能是 valueOf 或 of)
                            LexiconEnum lexicon = LexiconEnum.valueOf(name); 
                            
                            // 2. 更新全局词汇量表 (防止其他地方用 get 报错)
                            if (BookConfig.VOCABULARY_MAP == null) BookConfig.VOCABULARY_MAP = new HashMap<>();
                            BookConfig.VOCABULARY_MAP.put(lexicon, cards.size());
                            
                            // 3. 注册到学习系统 (修复崩溃的核心!)
                            BuildQuizDataRequest.FSRSFactory.INSTANCE.addLexicon(lexicon, cards.size());
                        }

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
            // }
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

    public static void reloadUserDicts() {
        logger.info("WordSpire: Force reloading user dictionaries...");
        
        // 1. 清理旧数据
        if (allBooks.containsKey(BookEnum.USER_DICT)) {
            allBooks.remove(BookEnum.USER_DICT);
        }
        
        // 2. 清理 userBooks 中的旧对象 (非常重要，否则会有脏数据)
        userBooks.removeIf(book -> book.bookEnum == BookEnum.USER_DICT);
        
        // 3. 重新扫描目录
        initUserDictionaries();
        
        // 4. 处理新数据
        if (allBooks.containsKey(BookEnum.USER_DICT)) {
            BookConfig userBook = allBooks.get(BookEnum.USER_DICT);
            
            // [关键] 将新书加入 userBooks，让 Neow 和游戏逻辑看到它
            userBooks.add(userBook);
            
            // [关键] 手动注册遗物！
            // 因为 receiveEditRelics 只在游戏启动时运行一次。
            // 如果启动时没有用户词库，UserDictRelic 此时是未注册状态，必须补票。
            AbstractRelic relic = userBook.relicSupplier.get();
            BaseMod.addRelic(relic, RelicType.SHARED); // 注册到 BaseMod 和 RelicLibrary
            UnlockTracker.markRelicAsSeen(relic.relicId); // 确保解锁
            
            // 加载数据 (防止闪退)
            for (Object obj : userBook.lexicons) {
                String name;
                if (obj instanceof Enum) {
                    name = ((Enum<?>) obj).name();
                } else {
                    name = obj.toString();
                }
                reloadAnkiFile(name);
            }

            // 更新 UI
            ModConfigPanel.addRelicPage(BookEnum.USER_DICT, userBook.lexicons);
        }
        
        logger.info("WordSpire: Reload complete. UserBooks updated & Relic registered.");
    }

    // [新增] 单文件热重载 (用于 Level 3 Save & Return)
    public static void reloadAnkiFile(String name) {
        logger.info("WordSpire: Hot reloading Anki file -> " + name);
        try {
            File ankiFile = new File(USER_DICT_PATH + name + ".apkg");
            if (!ankiFile.exists()) return;

            SpireConfig tempConfig = new SpireConfig(MOD_ID, "config");
            File audioOutDir = new File(USER_DICT_PATH + "audio/");
            
            // 重新解析卡片
            List<AnkiPackageLoader.AnkiRawCard> cards = AnkiPackageLoader.loadFromApkg(ankiFile, audioOutDir, tempConfig);
            
            // ================= 核心修复开始 =================
            if (cards != null && !cards.isEmpty()) {
                // 1. 获取 LexiconEnum 对象 (根据你的静态类实现，这里用 of 或 valueOf)
                // 注意：确保你的 LexiconEnum.of(name) 能正确返回或创建对象
                LexiconEnum lexicon = LexiconEnum.valueOf(name); 
                
                // 2. [关键!] 更新全局词汇量 Map
                // 如果不加这一行，Factory.fromTargetIndex 查不到数量就会报 NullPointerException
                if (BookConfig.VOCABULARY_MAP == null) {
                    BookConfig.VOCABULARY_MAP = new HashMap<>();
                }
                BookConfig.VOCABULARY_MAP.put(lexicon, cards.size());
                logger.info("Updated VOCABULARY_MAP for {}: size={}", name, cards.size());

                // 3. 注册到 FSRS 学习系统 (防止 SessionDoesNotExist)
                WordSpire.relics.BuildQuizDataRequest.FSRSFactory.INSTANCE.addLexicon(lexicon, cards.size());
            }
            // ================= 核心修复结束 =================

            // 重新构建 JSON 数据 (逻辑复用 loadVocabulary)
            Map<String, Object> localizationMap = new HashMap<>();
            Map<String, Object> infoData = new HashMap<>();
            List<String> infoText = new ArrayList<>();
            infoText.add(String.valueOf(cards.size()));
            infoData.put("TEXT", infoText);
            localizationMap.put(JSON_MOD_KEY + name + "_info", infoData);

            for (int i = 0; i < cards.size(); i++) {
                AnkiPackageLoader.AnkiRawCard card = cards.get(i);
                String wordId = JSON_MOD_KEY + name + "_" + i;
                Map<String, Object> wordData = new HashMap<>();
                List<String> textList = new ArrayList<>();
                textList.add(card.front);
                textList.addAll(card.backList);
                wordData.put("TEXT", textList);
                if (card.audio != null && !card.audio.isEmpty()) {
                    Map<String, String> extraData = new HashMap<>();
                    extraData.put("AUDIO", card.audio);
                    wordData.put("TEXT_DICT", extraData);
                }
                localizationMap.put(wordId, wordData);
            }

            // 立即生效
            Gson gson = new Gson();
            String generatedJson = gson.toJson(localizationMap);
            BaseMod.loadCustomStrings(UIStrings.class, generatedJson);
            
            logger.info("WordSpire: Hot reload finished for " + name);
        } catch (Exception e) {
            logger.error("WordSpire: Hot reload failed", e);
        }
    }

}
