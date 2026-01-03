package WordSpire.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;

import WordSpire.helpers.ImageElements;
import WordSpire.helpers.WordAudioPlayer;

public class ReplayButton extends UIButton {
    public static final float IMG_W;
    public static final float IMG_H;
    private static final Color HOVER_BLEND_COLOR;
    private String wordIDToPlay = null;

    public ReplayButton(float pos_x, float pos_y) {
        super(pos_x, pos_y, IMG_W, IMG_H);
    }

    public void updateWord(String wordId) {
        this.wordIDToPlay = wordId;
    }

    @Override
    public void buttonClicked() {
        if (this.wordIDToPlay != null) {
            WordAudioPlayer.playByWordId(this.wordIDToPlay);            
            // WordAudioPlayer.play("安心");
        }
    }

    @Override
    public void render(SpriteBatch sb, BitmapFont font) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageElements.INFO_BUTTON, this.current_x, this.current_y, IMG_W, IMG_H);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_BLEND_COLOR);
            sb.draw(ImageElements.INFO_BUTTON, this.current_x, this.current_y, IMG_W, IMG_H);
            sb.setBlendFunction(770, 771);
        }
        super.render(sb, font);
    }

    static {
        IMG_W = 50.0F * Settings.xScale; 
        IMG_H = 50.0F * Settings.yScale;
        HOVER_BLEND_COLOR = new Color(1.0F, 1.0F, 1.0F, 0.4F);
    }
}