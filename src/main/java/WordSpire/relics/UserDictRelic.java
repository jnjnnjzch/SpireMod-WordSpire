package WordSpire.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import WordSpire.actions.QuizAction.QuizData;
import WordSpire.events.CallOfCETEvent;
import WordSpire.helpers.ArrayListHelper;
import WordSpire.helpers.BookConfig;
import WordSpire.ui.ModConfigPanel;

import java.util.ArrayList;
import java.util.Collections;

public class UserDictRelic extends QuizRelic {
    public UserDictRelic() {
        super(CallOfCETEvent.BookEnum.USER_DICT);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new UserDictRelic();
    }

    @Override
    public String updateDesByLexicon(BookConfig.LexiconEnum lexiconEnum) {
        if (lexiconEnum == null) {
            return "Choose a Dictionary"; // 默认提示
        }
        // 直接显示词典的文件名 (如 "myN1")
        return "User Dictionary: " + lexiconEnum.name();
    }

    @Override
    public QuizData buildQuizData(BuildQuizDataRequest request) {
        UIStrings tmp = CardCrawlGame.languagePack.getUIString(request.getTargetUiStringsId());
        String word = null;
        ArrayList<String> right_ans_list = new ArrayList<>();
        for (String item: tmp.TEXT) {
            if (word == null) {
                word = item;
                continue;
            }
            right_ans_list.add(item);
        }
        right_ans_list = ArrayListHelper.choose(right_ans_list, ModConfigPanel.maxAnsNum);

        ArrayList<String> meaning_list = new ArrayList<>();
        // copy
        meaning_list.addAll(right_ans_list);
        int choice_num = 3 * right_ans_list.size();
        // int choice_num = 3 * request.getMaxOptionNum();
        if (choice_num > request.getMaxOptionNum()) {
            choice_num = request.getMaxOptionNum();
        }

        // 插入 "WRONG_ANS" (指定干扰项)
        if (tmp.TEXT_DICT != null) {
            String wrongStr = tmp.TEXT_DICT.get("WRONG_ANS");
            if (wrongStr != null && !wrongStr.isEmpty()) {
                String[] wrongs = wrongStr.split("\\|");
                for (String w : wrongs) {
                    // 只有当选项还没满，且不重复时才添加
                    if (meaning_list.size() < choice_num && !meaning_list.contains(w)) {
                        meaning_list.add(w);
                    }
                }
            }
        }

        for (int i = meaning_list.size(); i < choice_num;) {
            int target_word = MathUtils.random(0, request.getVocabularySize()- 1);
            if (target_word == request.targetId) {
                continue;
            }
            tmp = CardCrawlGame.languagePack.getUIString(request.getUiStringsIdStart() + target_word);
            int target_meaning = MathUtils.random(1, tmp.TEXT.length - 1);
            meaning_list.add(tmp.TEXT[target_meaning]);
            i++;
        }
        Collections.shuffle(meaning_list);
        return new QuizData(request.getTargetId(), request.getTargetUiStringsId(), word, right_ans_list, meaning_list);
    }
}