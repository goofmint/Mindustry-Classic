// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.ucore.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.utils.Array;

/**Font data for Japanese text. Besides the usual wrap at whitespace, a line may also be wrapped between
 * two characters when at least one of them is a Japanese character (kana, kanji, full-width forms).
 * <ul>
 * <li>A run of ASCII letters and digits is never split, and an ASCII letter or digit stays on the same
 * line as the Japanese character after it (so "1個" and "CodeRabbitは" are not split).</li>
 * <li>A character that must not start a line ({@link #LINE_START_PROHIBITED}) is never moved to the next line,
 * and an opening bracket ({@link #LINE_END_PROHIBITED}) is never left at the end of a line.</li>
 * </ul>
 * If no such position exists in the line, {@link #getWrapIndex} returns 0 and GlyphLayout splits right before
 * the glyph that did not fit, so the rules above can be broken when the width is extremely small.
 * Only plain char comparisons are used so that the class also compiles with GWT.*/
public class JapaneseFontData extends BitmapFontData{
    /**Characters that must not be placed at the start of a line (kinsoku): small kana, the prolonged sound mark,
     * punctuation, closing brackets and iteration marks.*/
    public static final String LINE_START_PROHIBITED =
        "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ" +
        "ｧｨｩｪｫｬｭｮｯ" +
        "ーｰ〜～" +
        "、。，．・：；？！､｡･,.:;?!" +
        "）」』】〕〉》〙〗］｝”’)]}" +
        "ゝゞヽヾ々";

    /**Characters that must not be placed at the end of a line: opening brackets.*/
    public static final String LINE_END_PROHIBITED = "（「『【〔〈《〘〖［｛“‘([{";

    public JapaneseFontData(){
        super();
    }

    public JapaneseFontData(FileHandle fontFile, boolean flip){
        super(fontFile, flip);
    }

    /**Whether the character is Japanese: hiragana, katakana, CJK symbols and punctuation (including the
     * ideographic space), CJK ideographs and full-width/half-width forms.*/
    public static boolean isJapanese(int c){
        return (c >= 0x3000 && c <= 0x303F)
            || (c >= 0x3040 && c <= 0x309F)
            || (c >= 0x30A0 && c <= 0x30FF)
            || (c >= 0x31F0 && c <= 0x31FF)
            || (c >= 0x3400 && c <= 0x4DBF)
            || (c >= 0x4E00 && c <= 0x9FFF)
            || (c >= 0xFF00 && c <= 0xFFEF);
    }

    /**Whether the character must not be placed at the start of a line.*/
    public static boolean isLineStartProhibited(int c){
        return (c >= 0x31F0 && c <= 0x31FF) || LINE_START_PROHIBITED.indexOf(c) >= 0;
    }

    static boolean isAsciiAlphanumeric(int c){
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    static boolean isSpace(int c){
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    /**Whether the character must not be placed at the end of a line.*/
    public static boolean isLineEndProhibited(int c){
        return LINE_END_PROHIBITED.indexOf(c) >= 0;
    }

    /**Whether a line may be wrapped between the characters before and after.*/
    public static boolean canBreakBetween(int before, int after){
        if(isSpace(before) || isSpace(after)) return false;
        if(!isJapanese(before) && !isJapanese(after)) return false;
        if(isAsciiAlphanumeric(before) || isLineEndProhibited(before)) return false;
        return !isLineStartProhibited(after);
    }

    /**Index of the first glyph of the next line for a line in which the glyph at start - 1 did not fit,
     * considering only the Japanese rules; 0 if there is no such position.*/
    public static int japaneseWrapIndex(Array<Glyph> glyphs, int start){
        for(int k = start - 1; k > 0; k--){
            if(canBreakBetween(glyphs.get(k - 1).id, glyphs.get(k).id)) return k;
        }
        return 0;
    }

    @Override
    public int getWrapIndex(Array<Glyph> glyphs, int start){
        return Math.max(super.getWrapIndex(glyphs, start), japaneseWrapIndex(glyphs, start));
    }
}
