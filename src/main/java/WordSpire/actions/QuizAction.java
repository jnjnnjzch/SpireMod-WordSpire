package WordSpire.actions;

import basemod.BaseMod;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;

import WordSpire.helpers.BookConfig.LexiconEnum;
import WordSpire.relics.BuildQuizDataRequest;
import WordSpire.relics.QuizRelic;
import WordSpire.relics.BuildQuizDataRequest.FSRSFactory;
import WordSpire.relics.BuildQuizDataRequest.IFactory;
import WordSpire.screens.QuizScreen;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public abstract class QuizAction extends AbstractGameAction {
    private static final Logger logger = LogManager.getLogger(QuizAction.class);
    protected final LexiconEnum lexicon;
    private final QuizRelic quizRelic;
    public QuizAction(QuizRelic quizRelic, LexiconEnum lexicon) {
        this.lexicon = lexicon;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FASTER;
        this.quizRelic = quizRelic;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FASTER) {
            QuizData quizData = quizRelic.buildQuizData(quizRelic.getFactory().fromRandom(lexicon));
            logger.info("quizData = {}", quizData);
            WordSpire.helpers.WordAudioPlayer.playByWordId(quizData.getWordUiStringsId());
            BaseMod.openCustomScreen(QuizScreen.Enum.WORD_SCREEN, quizData.show, lexicon.name(),
                    quizData.correctOptions, quizData.allOptions, quizData.getWordUiStringsId(), false);
            tickDuration();
            return;
        }
        tickDuration();

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuizData {
        private int wordId;
        private String wordUiStringsId;
        private String show;
        private List<String> correctOptions;
        private List<String> allOptions;
    }

}
