# WordSpire - 在尖塔背单词！（支持导入Anki词库、支持音频、内置词库下载中心）

> **[本项目](https://github.com/jnjnnjzch/SpireMod-WordSpire)遵循 [GPL v3.0](LICENSE) 开源协议。**
> 欢迎任何人参与贡献（贡献代码、制作词库、或者评测与推荐优质词库！）

![License](https://img.shields.io/github/license/jnjnnjzch/spiremod-wordspire?color=blue&label=License&style=flat-square)
![Game](https://img.shields.io/badge/Game-Slay_the_Spire-red?style=flat-square&logo=steam)

![浏览量](https://img.shields.io/steam/views/3637498963?label=浏览量&style=flat-square&logo=steam)
![订阅数](https://img.shields.io/steam/subscriptions/3637498963?label=订阅数&style=flat-square&logo=steam)
![收藏数](https://img.shields.io/steam/favorites/3637498963?label=收藏数&style=flat-square&logo=steam)

## 🙏 致谢

感谢前辈的开源代码[别忘了四六级](https://github.com/sleepyHolo/SpireMod_CET46InSpire)

同时感谢诸多前辈的优秀模组提供灵感！
* [别忘了四六级(CET46 In Spire)
](https://steamcommunity.com/sharedfiles/filedetails/?id=3447680436)
* [StudySpire（在尖塔中学习）](https://steamcommunity.com/sharedfiles/filedetails/?id=3499905594)
* [尖塔诗词大会](https://steamcommunity.com/sharedfiles/filedetails/?id=3454028861)

## ✨ 为什么选择 WordSpire？

在使用同类模组时，你是否也有过这样的感叹：
* *"哎呀，要是这个模组支持 xxx 小语种就好了..."*
* *"背单词没有发音，感觉记不住啊..."*
* *"找词库太麻烦了，要是能一键下载就好了..."*


**现在，这些都不是梦想了！**这个模组实现了三大核心功能！

### 1. 📂 Anki 词库 (.apkg) 支持
不再局限于内置词库！你可以直接从 [AnkiWeb Shared Decks](https://ankiweb.net/shared/decks) 下载海量词库（托福、雅思、日语、德语...），只需放入模组文件夹`SlayTheSpire\mods\WordSpire`，即刻开学！

### 2. 🔊 完整的音频支持
一边爬塔，一边磨耳朵！模组完整支持 Anki 卡片中的音频播放，界面内置**重播按钮**，再也不用担心听漏发音 ~

### 3. ⬇️ 游戏内下载中心
懒得找资源？没关系！模组内置**下载中心 (Download Center)**，收录了经过验证的高质量词库（如 JLPT 10k 带音频版）。点击下载，自动配置，一键体验！

> 如果大家制作/发现了体验较好的词库，也欢迎大家分享告知，我将把下载地址放入下载中心供其他人一键体验！
  
**还有一些小功能：**
* **🔥 支持词库热加载**！无需重启游戏，流畅体验！
  - 话虽如此，重启游戏还是能解决大部分问题的
* **🎮支持键鼠和控制器！**

## 🚀 对于高阶玩家，这个模组还提供了进阶功能！
我们提供了强大的自定义能力：

* **多词库混合**：
  - 可以同时加载多个词库，通过**权重 (Weight)** 控制每个词库在战斗中出现的概率。
* **完全自定义映射 (Mapping)**：
  - 你可以自由指定 Anki 卡片的哪个字段是“问题 (Question)”，哪个/哪些是“答案 (Answer)”，哪个是音频文件 (Sound)。
  - **玩法举例**：
      - *日语学习*：可以设置“看假名(Q) -> 选汉字(A)”，也可以设置“看含义(Q) -> 选假名(A)”。
      - *双向记忆*：把同一个词库复制两份，一份“英译中”，一份“中译英”，实现全方位记忆！
  - **例句/单词音频切换**: 你还可以为词库配置音频文件，有的厉害词库可能会有例句音频，可以通过 Sound 选项来配置音频文件，这样具有语境，更方便理解！
  
> 当然，如果你会制作json词库，我们也支持 `.json` 格式的自定义词库文件，格式在后面。

## 贡献你的力量！
没有前辈们的代码或者想法，就没有这个模组的诞生。相互贡献才能让项目变得更好！

也因此，欢迎你们的任何贡献，比如
- **💻 贡献代码**
  -  帮我做TODO（？）
  -  实现新功能，修Bug，实现自己的想法
- **📦 制作词库**
  - 如果你是 Anki 词库的制作者，欢迎帮忙稍作适配
  - 考题目前以 Anki 字段作为单位，因此一词多义的话能不能用多个字段来呈现？
- **📢 词库评测与推荐**
  - 如果不会写代码，也不会制作题库呢？
  - 完全没问题！你可以体验并评测词库，如果发现好的词库，可以分享到评论区，我验证后会把它们加入下载中心，供**所有**后来人使用！

感谢你们的贡献，我会将你们写到新版本的致谢名单，感谢你们的付出！！

## 📖 如何游玩

和优秀的前辈模组[别忘了四六级(CET46 In Spire)
](https://steamcommunity.com/sharedfiles/filedetails/?id=3447680436)类似，基本步骤非常简单！

### 第一步：添加词库 (二选一)

* **方法 A：通过下载中心 (推荐)**
    1.  进入游戏主菜单 -> **Mods** -> **WordSpire** -> **Config**。
    2.  点击 `>` 翻到第二页。
    3.  点击右上角的 **[Download Center]** 按钮。
    4.  选中你喜欢的词库，点击 **Download**。
    5.  等待下载完成，退回主界面，直接开始游戏！

* **方法 B：手动添加本地文件**
    1.  准备好你的 `.apkg` 文件（Anki 导出文件，建议勾选包含媒体文件）。
    2.  打开游戏安装目录：`SlayTheSpire/mods/WordSpire/`。
        * *省流提示：Steam 游戏界面右边齿轮 -> 属性 -> 已安装文件 -> 浏览 -> mods -> WordSpire*
        * *注：如果是第一次运行，请先启动一次游戏，模组会自动创建该文件夹。*
    3.  将 `.apkg` 文件放入该文件夹。
    4.  回到游戏 Config 界面，点击 **[Refresh Files]** 按钮即可识别。

### 第二步：配置映射 (仅限手动添加)
如果是从下载中心下载的词库，通常已预设好配置，无需操作。
如果是手动添加的复杂 Anki 词库，你需要告诉模组怎么出题：
1.  在 Config 界面找到你的词库，点击 **[Config Mapping]**。
2.  勾选作为题面的 **Question** 字段。
3.  勾选作为正确选项的 **Answer** 字段。
4.  勾选作为音频 **Sound**字段（如果有）。
5.  点击 **Save & Back** 保存。

### 第三步：开始游戏
1.  开始新游戏，在涅奥 (Neow) 处选择初始遗物 **[外词之书]**。
2.  **打牌做题**：每打出一张牌，都会触发单词测验。答对题目可以获得攻击/格挡倍率加成！
3.  **错题本**：答错的题目会被记录。右键点击遗物可复习错题，完美回答错题还有机会获得药水奖励哦！
<!-- 
**添加词库**：有两种方式添加词库
  - **通过下载中心**：主界面 -> Mods -> Word Spire -> Config -> 按">"翻到第二页 -> 右上角 Download Center -> 选中你喜欢的词库点击Download -> 等待词库自行配置完成后退回主界面开始游戏！
  - **手动添加**：*省流：放到`SlayTheSpire\mods\WordSpire`下面*。详细：打开游戏目录（Steam的游戏界面右边齿轮 -> 属性 -> 已安装文件 -> 浏览），然后打开`mods`, 将`.apkg`的anki词库放入`mods`文件夹下的`WordSpire`文件夹中（这个文件夹在第一次启动模组的时候会创建，如果没有的话可以自己创建一个）
  
**配置词库**：如果是下载中心下载的话，我们已经预先配置好了，所以无需配置；如果是手动添加的话，由于Anki词库格式过于复杂，需要进入词库设置界面（游戏内主界面 -> Mods -> Word Spire -> Config -> 按">"翻到第二页 -> User Dictionary旁边的Manage按钮，Config Mapping）开始配置！

**领取外词之书**: 在第0层的涅奥房间结束时强制进入事件, 允许选择遗物的词库，想要体验自己的词库，请领取“**外词之书**”！(也可以选择不拿遗物, 那么就和正常游戏没有区别).  

**打牌做题**: 每当你打出一张卡牌, 会触发一个指定词库范围内的小单词测验, 当测验面板出现的同时，将播放音频（如果有）。你的测验得分会影响你**所有**攻击和格挡的获得倍率. 测验得分在每次测验后更新, 或是在回合结束或战斗结束时复位.  

**临时错题本**: 现在, 同一局中获得0分的题目将被放入错题本中, 随时可以通过右键单击遗物启用错题回顾. 错题回顾得分将在高于目前得分的情况下替换得分, 错题回顾不影响常规连胜作答统计. 每场战斗第一次完美回答错题正确能够获得一瓶随机药水(如果没有错题就能免费获得一瓶随机药水). 目前, 错题仅能在同一局游戏中保存, 不同局的错题数据目前无法互通.   -->

## 📚 词库来源

### 内置词库

内置继承了 [别忘了四六级(CET46 In Spire)
](https://steamcommunity.com/sharedfiles/filedetails/?id=3447680436) 模组的JLPT和CET4/CET6词库，如有侵权将立即删除。

* **CET4**: [大学英语四级单词(词典重制完美版)](https://ankiweb.net/shared/info/1378032490)
* **CET6**: [大学六级英语单词全集（修订版）](https://ankiweb.net/shared/info/2125686844)
* **JLPT**: [5mdld/anki-jlpt-decks](https://github.com/5mdld/anki-jlpt-decks)
  
### 自定义词库

模组不提供自定义词库的资源文件，仅在下载中心 (Download Center) 提供经验证的词库下载地址。

目前自我体验比较好的词库是 @5mdld 大佬的词库，因此下载中心提供这个词库的界面。

也欢迎大家去 GitHub / AnkiShare 给大佬的词库点赞！

目前下载中心提供的词库：

|内部显示词库名字|下载后文件名字|作者|相关地址|
| :--- | :--- | :--- | :--- |
|**JLPT-10k**|JLPT10k.apkg|egg rolls @5mdld |[Anki地址](https://ankiweb.net/shared/info/832276382), [发布链接](https://github.com/5mdld/anki-jlpt-decks)|


## 🌍 本地化
* 支持 **简体中文** 和 **English**。
* 当游戏语言为中文（简/繁）时，界面显示中文；其他语言显示英文。
* *注：单词的释义语言取决于你使用的词库本身。*

## ✅ TODO
* [x] 支持手柄重播音频。
* [ ] 优化长文本布局和自动换行。



## 附录：JSON 词库制作指南
除了 `.apkg`，模组也支持原生的 `.json` 格式词库，适合高级用户手动编写。

**命名规则**：
如果文件名是 `myN1.json`，那么 JSON 内部的 Key 必须以 `CET46:myN1_` 开头。
**格式示例**：
```json
{
  "CET46:myN1_info": {
    "TEXT": [
      "4084" 
    ],
    "EXTRA_TEXT": null,
    "TEXT_DICT": null
  },
  "CET46:myN1_0": {
    "TEXT": [
      "昭和",               
      "（日本年号）昭和",    
      "しょうわ"            
    ],
    "EXTRA_TEXT": null,
    "TEXT_DICT": {
      "WRONG_ANS": "じょうわ|しきうわ",
      "AUDIO": "昭和_ショーワ━_0_NHK.mp3"
    }
  }
}
```
- `..._info`: `TEXT[0]` 填写词库总词数。
- `TEXT`:
  - 索引 0 (TEXT[0])：题面 (Question)。
  - 索引 1 及以后：正确答案 (Correct Answers)。支持一词多义，有多个正确选项。

- `TEXT_DICT`
  - `WRONG_ANS`: 以 | 分隔的固定混淆项 (可选)。如果不填，系统会随机抽取其他题目的答案作为混淆项
  - `AUDIO`: 音频文件名 (音频文件需放入 `mods/WordSpire/audio/` 文件夹)。


例如，示例中题面就是`昭和`，有2个正确答案分别是`（日本年号）昭和`和`しょうわ`，会固定出两个混淆答案`じょうわ`和`しきうわ`，并且，会自动播放对应的`.mp3`音频.
