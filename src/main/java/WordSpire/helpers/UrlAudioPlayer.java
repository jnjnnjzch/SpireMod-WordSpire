package WordSpire.helpers;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
public class UrlAudioPlayer {
    private static final Logger logger = LogManager.getLogger(WordAudioPlayer.class);
    private static Sound currentSound = null;
    
    // 缓存目录路径
    private static final String FOLDER_PATH = "mods/WordSpire/audio/";

    /**
     * 播放单词音频。
     * 逻辑：优先查找本地缓存 -> 如果没有，则尝试下载 -> 下载后播放
     *
     * @param word 单词拼写 (用于文件名)
     * @param audioUrl 在线音频的完整 URL (如果本地没有，将从这里下载)
     */
    public static void play(String word, String audioUrl) {
        // 1. 清理上一个音频
        stopCurrentSound();

        // 2. 检查本地是否有缓存 (支持 mp3, ogg, wav)
        FileHandle localFile = findLocalFile(word);

        if (localFile != null && localFile.exists()) {
            // A. 本地有文件，直接播放
            playSoundFile(localFile);
        } else {
            // B. 本地没文件，且提供了 URL -> 启动后台线程下载
            if (audioUrl != null && !audioUrl.isEmpty()) {
                logger.info("WordAudioPlayer: 本地未找到，准备下载 -> " + word);
                downloadAndPlay(word, audioUrl);
            } else {
                logger.info("WordAudioPlayer: 本地未找到且未提供URL -> " + word);
            }
        }
    }

    // 辅助方法：停止并销毁当前音频
    private static void stopCurrentSound() {
        if (currentSound != null) {
            try {
                currentSound.stop();
                currentSound.dispose();
            } catch (Exception e) {
                logger.error("销毁音频出错", e);
            }
            currentSound = null;
        }
    }

    // 辅助方法：查找本地存在的音频文件
    private static FileHandle findLocalFile(String word) {
        // 依次尝试常见格式
        String[] extensions = {".mp3", ".ogg", ".wav"};
        for (String ext : extensions) {
            FileHandle file = Gdx.files.local(FOLDER_PATH + word + ext);
            if (file.exists()) return file;
        }
        return null;
    }

    // 核心逻辑：后台下载 -> 主线程播放
    private static void downloadAndPlay(String word, String urlString) {
        new Thread(() -> {
            try {
                // --- 1. 网络下载部分 (后台线程) ---
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // 伪装成浏览器，防止某些网站(如韦氏词典)拒绝请求
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(5000); // 5秒超时
                
                // 获取文件后缀 (例如从 url 中提取 .wav)
                String extension = ".mp3"; // 默认
                if (urlString.contains(".")) {
                    extension = urlString.substring(urlString.lastIndexOf('.'));
                    // 处理可能的 URL 参数 (如 file.mp3?token=123)
                    if (extension.contains("?")) {
                        extension = extension.substring(0, extension.indexOf("?"));
                    }
                }

                // 确定保存路径
                FileHandle targetFile = Gdx.files.local(FOLDER_PATH + word + extension);
                
                // 确保文件夹存在
                if (!targetFile.parent().exists()) {
                    targetFile.parent().mkdirs();
                }

                try (InputStream in = conn.getInputStream()) {
                    // 使用 java.nio.file 复制流
                    Files.copy(in, targetFile.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                logger.info("WordAudioPlayer: 下载成功 -> " + targetFile.path());

                // --- 2. 播放部分 (必须切回主线程) ---
                Gdx.app.postRunnable(() -> {
                    // 下载完成后，尝试播放
                    // 注意：这里需要再次检查文件是否存在，防止下载过程出错
                    if (targetFile.exists()) {
                        playSoundFile(targetFile);
                    }
                });

            } catch (Exception e) {
                logger.error("WordAudioPlayer: 下载或保存失败 -> " + word, e);
            }
        }).start();
    }

    // 统一的播放逻辑 (必须在主线程调用)
    private static void playSoundFile(FileHandle file) {
        try {
            currentSound = Gdx.audio.newSound(file);
            currentSound.play(1.0f);
            logger.info("WordAudioPlayer: 正在播放 -> " + file.name());
        } catch (Exception e) {
            logger.error("WordAudioPlayer: 播放失败 (可能是格式不支持) -> " + file.path(), e);
        }
    }
}