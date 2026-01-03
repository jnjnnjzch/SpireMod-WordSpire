package WordSpire.helpers;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import WordSpire.WordSpireInitializer;
import WordSpire.events.CallOfCETEvent.BookEnum;
import WordSpire.relics.BuildQuizDataRequest.FSRSFactory;
import WordSpire.ui.ModConfigPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class BookConfig {
    private static final Logger logger = LogManager.getLogger(BookConfig.class.getName());
    public BookEnum bookEnum;
    public List<LexiconEnum> lexicons;
    public Supplier<AbstractRelic> relicSupplier;

    public BookConfig(BookEnum bookEnum, List<LexiconEnum> lexicons, Supplier<AbstractRelic> relicSupplier) {
        this.bookEnum = bookEnum;
        this.lexicons = new ArrayList<>(lexicons); // 由于lexicons改为动态加载，因此直接传入，不用for循环
        this.relicSupplier = relicSupplier;
    }

    public boolean needNotLoad() {
        try {
            Field field = ModConfigPanel.class.getField("load" + this.bookEnum.name());
            return !field.getBoolean(null);  // 一定是静态字段
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("bad needNotLoad: ", e);
            return false;
        }
    }

    // --- 代替原 CET46Settings 管理的数据 ---


    /**
     * 含义同WordSpireInitializer.needLoadBooks
     */
    public static Map<LexiconEnum, Integer> VOCABULARY_MAP = new HashMap<>();

    public static void init_map() {
        // 调用时间必须在Panel初始化后
        WordSpireInitializer.needLoadBooks.forEach(lexiconEnum -> {
            VOCABULARY_MAP.put(lexiconEnum, Integer.parseInt(CardCrawlGame.languagePack.getUIString(WordSpireInitializer.JSON_MOD_KEY + lexiconEnum.name() + "_info").TEXT[0]));
        });
        FSRSFactory.INSTANCE.initMap(VOCABULARY_MAP);
        logger.info("CET46Settings init called, VOCABULARY_MAP = {}", VOCABULARY_MAP);
    }

    // 重写LexiconEnum，使之能动态读取外部词库
    public static class LexiconEnum{
        private final String id;

        // 构造函数
        private LexiconEnum(String id){
            this.id = id;
        }

        // 保留原有的词典
        public static final LexiconEnum CET4 = new LexiconEnum("CET4");
        public static final LexiconEnum CET6 = new LexiconEnum("CET6");
        public static final LexiconEnum N5 = new LexiconEnum("N5");
        public static final LexiconEnum N4 = new LexiconEnum("N4");
        public static final LexiconEnum N3 = new LexiconEnum("N3");
        public static final LexiconEnum N2 = new LexiconEnum("N2");
        public static final LexiconEnum N1 = new LexiconEnum("N1");

        // 读取外部文件的方法
        public static LexiconEnum of(String id) {
            return new LexiconEnum(id);
        }
        // 模拟Enum的name()方法
        public String name() {
            return id;
        }

        // 重写一些必要的函数
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LexiconEnum that = (LexiconEnum) o;
            return Objects.equals(id, that.id);
        }
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
        @Override
        public String toString() {
            return id;
        }

        public static LexiconEnum valueOf(String name) {
            return getLexicon(name);
        }
    }

    @Nullable
    public static LexiconEnum getLexicon(String lexicon) {
        if (lexicon.equalsIgnoreCase("CET4")) return LexiconEnum.CET4;
        if (lexicon.equalsIgnoreCase("CET6")) return LexiconEnum.CET6;
        if (lexicon.equalsIgnoreCase("N5")) return LexiconEnum.N5;
        if (lexicon.equalsIgnoreCase("N4")) return LexiconEnum.N4;
        if (lexicon.equalsIgnoreCase("N3")) return LexiconEnum.N3;
        if (lexicon.equalsIgnoreCase("N2")) return LexiconEnum.N2;
        if (lexicon.equalsIgnoreCase("N1")) return LexiconEnum.N1;

        // 现在不返回null，而是允许扩展
        return LexiconEnum.of(lexicon);
    }

}
