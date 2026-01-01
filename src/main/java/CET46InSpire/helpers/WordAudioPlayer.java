package CET46InSpire.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WordAudioPlayer {
    private static final Logger logger = LogManager.getLogger(WordAudioPlayer.class);
    private static Sound currentSound = null;

    public static void playByWordId(String wordUiStringId) {
        if (wordUiStringId == null) return;
        UIStrings tmp = CardCrawlGame.languagePack.getUIString(wordUiStringId);
    
        if (tmp != null && tmp.TEXT_DICT != null) {
            String audioFileName = tmp.TEXT_DICT.get("AUDIO");
            if (audioFileName != null && !audioFileName.isEmpty()) {
                play(audioFileName); 
            }
        }
    }

    public static void play(String word) {
        if (currentSound != null) {
            currentSound.stop();    // 停止播放
            currentSound.dispose(); // 释放内存
            currentSound = null;
        }
        String folderPath = "mods/CET46InSpire/UserDictionaries/audio/"; 
        
        FileHandle file = null;

        file = Gdx.files.local(folderPath + word + ".ogg");
        
        if (!file.exists()) {
            file = Gdx.files.local(folderPath + word + ".mp3");
        }

        if (file.exists()) {
            try {
                // lazy loading: 只有这一刻才把文件读入内存
                currentSound = Gdx.audio.newSound(file);
                currentSound.play(1.0f); // 1.0f 是音量 (0.0 - 1.0)
                logger.info("WordAudioPlayer: 正在播放 -> " + file.path());
            } catch (Exception e) {
                logger.info("WordAudioPlayer: 文件损坏或无法读取 -> " + word);
                e.printStackTrace();
            }
        } else {
            // 如果你想调试，可以把下面这行注释取消，看看是不是路径不对
            logger.info("WordAudioPlayer: 找不到文件 -> " + file.path());
        }
    }
}