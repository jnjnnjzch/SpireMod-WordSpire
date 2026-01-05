package WordSpire.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;

import WordSpire.helpers.ImageElements;
import WordSpire.helpers.WordAudioPlayer;

public class InfoTip extends UIButton {
    private static final String TIP_BODY;
    public static final float IMG_W;
    public static final float IMG_H;
    private static final Color HOVER_BLEND_COLOR;
    private static final float BOX_EDGE_H;
    private static final float BOX_BODY_H;
    private static final float BOX_W;
    private static final float TEXT_OFFSET_X;
    private static final float BODY_OFFSET_Y;
    private static final float BODY_TEXT_WIDTH;
    private static final float TIP_DESC_LINE_SPACING;
    private String wordIDToPlay = null;

    public InfoTip(float pos_x, float pos_y) {
        super(pos_x, pos_y, IMG_W, IMG_H);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageElements.INFO_TIP, this.current_x, this.current_y, IMG_W, IMG_H);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_BLEND_COLOR);
            sb.draw(ImageElements.INFO_TIP, this.current_x, this.current_y, IMG_W, IMG_H);
            sb.setBlendFunction(770, 771);
        }
        if (this.hb.hovered) {
            renderTip(sb);
        }

        // 新增：如果是手柄模式，渲染按键提示
        if (Settings.isControllerMode) {
            renderControllerButton(sb);
        }

        if (Settings.isDebug) {
            this.hb.render(sb);
        }
    }

    // 新增：渲染手柄按键逻辑
    private void renderControllerButton(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        Texture buttonImg = ImageMaster.CONTROLLER_Y; // 默认使用 Xbox 的 X 键图标
        
        // 渲染位置：放在 InfoTip 图标的左侧，稍微偏移一点
        // 32f 是图标的大概尺寸，根据需要微调
        float iconSize = 32.0F * Settings.scale;
        float iconX = this.current_x - iconSize - 10.0F * Settings.scale; 
        float iconY = this.current_y + (IMG_H - iconSize) / 2.0F; // 垂直居中

        sb.draw(buttonImg, iconX, iconY, iconSize, iconSize);
    }

    public void renderTip(SpriteBatch sb) {
        float h = -FontHelper.getSmartHeight(FontHelper.tipBodyFont, TIP_BODY,
                BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING) - 7.0F * Settings.scale;
        float x = this.current_x;
        float y = this.current_y - IMG_H;
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.KEYWORD_TOP, x, y, BOX_W, BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BODY, x, y - h - BOX_EDGE_H, BOX_W, h + BOX_EDGE_H);
        sb.draw(ImageMaster.KEYWORD_BOT, x, y - h - BOX_BODY_H, BOX_W, BOX_EDGE_H);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, TIP_BODY,
                x + TEXT_OFFSET_X, y + BODY_OFFSET_Y,
                BODY_TEXT_WIDTH, TIP_DESC_LINE_SPACING, Color.WHITE);
    }

    @Override
    public void buttonClicked() {
        if (this.wordIDToPlay != null) {
            WordAudioPlayer.playByWordId(this.wordIDToPlay);
        }
    }

    public void updateWord(String wordId) {
        this.wordIDToPlay = wordId;
    }

    static {
        TIP_BODY = CardCrawlGame.languagePack.getUIString("CET46:InfoTip").TEXT[0];
        BOX_EDGE_H = 32.0F * Settings.scale;
        BOX_BODY_H = 64.0F * Settings.scale;
        BOX_W = 600.0F * Settings.scale;
        TEXT_OFFSET_X = 22.0F * Settings.scale;
        BODY_OFFSET_Y = -10.0F * Settings.scale;
        BODY_TEXT_WIDTH = 560.0F * Settings.scale;
        TIP_DESC_LINE_SPACING = 26.0F * Settings.scale;
        IMG_W = 45.0F * Settings.xScale;
        IMG_H = 45.0F * Settings.yScale;
        HOVER_BLEND_COLOR = new Color(1.0F, 1.0F, 1.0F, 0.2F);
    }
}
