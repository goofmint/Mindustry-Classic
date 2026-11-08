// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.ucore.graphics;

import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.utils.Array;
import org.junit.Test;

import static org.junit.Assert.*;

public class JapaneseFontDataTest{

    private static Array<Glyph> glyphs(String text){
        Array<Glyph> result = new Array<>();
        for(int i = 0; i < text.length(); i++){
            Glyph glyph = new Glyph();
            glyph.id = text.charAt(i);
            result.add(glyph);
        }
        return result;
    }

    /**Text of the next line when the glyph at index overflow does not fit.*/
    private static String nextLine(String text, int overflow){
        int index = new JapaneseFontData().getWrapIndex(glyphs(text), overflow + 1);
        return text.substring(index);
    }

    @Test
    public void japaneseCharacters(){
        assertTrue(JapaneseFontData.isJapanese('あ'));
        assertTrue(JapaneseFontData.isJapanese('エ'));
        assertTrue(JapaneseFontData.isJapanese('品'));
        assertTrue(JapaneseFontData.isJapanese('、'));
        assertTrue(JapaneseFontData.isJapanese('　'));
        assertTrue(JapaneseFontData.isJapanese('（'));
        assertFalse(JapaneseFontData.isJapanese('A'));
        assertFalse(JapaneseFontData.isJapanese('1'));
        assertFalse(JapaneseFontData.isJapanese(' '));
        assertFalse(JapaneseFontData.isJapanese('('));
    }

    @Test
    public void lineStartProhibitedCharacters(){
        for(char c : "ょゃゅっァィゥェォャュョッー、。」』）".toCharArray()){
            assertTrue(String.valueOf(c), JapaneseFontData.isLineStartProhibited(c));
        }
        for(char c : "あエジ品「1A".toCharArray()){
            assertFalse(String.valueOf(c), JapaneseFontData.isLineStartProhibited(c));
        }
    }

    @Test
    public void wrapsBetweenJapaneseCharacters(){
        //"品質を上げる": overflow at げ -> げ moves to the next line
        assertEquals("げる", nextLine("品質を上げる", 4));
    }

    @Test
    public void neverStartsLineWithSmallKanaOrProlongedSoundMark(){
        //overflow at ェ: neither ェ nor ー may start a line, so ジ moves down with them
        assertEquals("ジェント", nextLine("エージェント", 3));
        assertEquals("ジェント", nextLine("エージェント", 2));
    }

    @Test
    public void neverStartsLineWithPunctuation(){
        assertEquals("す。", nextLine("使います。", 4));
        //overflow at 」
        assertEquals("す」と", nextLine("「使います」と", 5));
    }

    @Test
    public void neverEndsLineWithOpeningBracket(){
        //overflow at 通: 「 stays with 通
        assertEquals("「通常", nextLine("品質「通常", 3));
    }

    @Test
    public void keepsNumbersWithTheFollowingCounter(){
        //"品質を1個ずつ": overflow at 個 -> 1 stays with 個
        assertEquals("1個ずつ", nextLine("品質を1個ずつ", 4));
        //overflow inside a number: the whole number moves
        assertEquals("2026年", nextLine("改変は2026年", 5));
    }

    @Test
    public void neverSplitsAsciiWords(){
        //overflow inside "CodeRabbit": the word moves as a whole, after the Japanese character before it
        assertEquals("CodeRabbitは", nextLine("ブロックCodeRabbitは", 8));
    }

    @Test
    public void englishStillWrapsAtSpacesOnly(){
        //same result as BitmapFontData: wraps after the last space, no break inside words
        assertEquals("world", nextLine("hello world", 8));
        assertEquals(new BitmapFontDataWrap().wrap("hello world", 8), nextLine("hello world", 8));
        assertEquals(0, new JapaneseFontData().getWrapIndex(glyphs("helloworld"), 8));
    }

    @Test
    public void prefersTheNearestCandidate(){
        //a space earlier in the line does not win over a closer Japanese break
        assertEquals("げる", nextLine("CI を通して上げる", 8));
        //a closer space wins over an earlier Japanese break
        assertEquals("CI", nextLine("品質を CI", 5));
    }

    /**Reference result of the stock whitespace wrapping.*/
    private static class BitmapFontDataWrap{
        String wrap(String text, int overflow){
            int index = new com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData().getWrapIndex(glyphs(text), overflow + 1);
            return text.substring(index);
        }
    }
}
