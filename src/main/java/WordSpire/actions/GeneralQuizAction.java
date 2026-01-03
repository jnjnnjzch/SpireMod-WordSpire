package WordSpire.actions;

import WordSpire.WordSpireInitializer;
import WordSpire.helpers.BookConfig;
import WordSpire.helpers.BookConfig.LexiconEnum;
import WordSpire.relics.QuizRelic;

public class GeneralQuizAction extends QuizAction {

    public GeneralQuizAction(QuizRelic quizRelic, BookConfig bookConfig, LexiconEnum usingLexicon) {
        super(
                quizRelic,
                usingLexicon
        );
    }

}
