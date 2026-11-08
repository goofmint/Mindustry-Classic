// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.desktop.tools;

import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.CodeItem.CodeAttr;
import io.anuke.mindustry.resource.Item;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * コードアイテムのアイコンを作る。基本画像 code-base/q&lt;stage&gt;-i&lt;importance&gt;.png(8×8、14 枚)に、
 * 通過状態のドットを右下の (5,7) 白(レビュー)、(6,7) 黒(テスト)、(7,7) 緑(CI)に重ね、
 * icon-code-q&lt;stage&gt;-p&lt;passed&gt;-i&lt;importance&gt;.png を書き出す。
 * リポジトリのルートを作業ディレクトリにして実行する(gradle :desktop:genIcons)。
 * 画像処理は javax.imageio と java.awt.image だけを使い、libGDX のグラフィックスには触れない。
 */
public class IconGenerator{
    private static final String SPRITES_DIR = "core/assets-raw/sprites";
    private static final String BASE_DIR = SPRITES_DIR + "/code-base";
    private static final String OUTPUT_PREFIX = "icon-code-";
    private static final int SIZE = 8;
    private static final int EXPECTED_COUNT = 70;

    private static final int DOT_Y = 7;
    private static final int REVIEW_X = 5, TEST_X = 6, CI_X = 7;
    private static final int REVIEW_COLOR = 0xFFFFFFFF;
    private static final int TEST_COLOR = 0xFF000000;
    private static final int CI_COLOR = 0xFF3AC83A;

    public static void main(String[] args) throws IOException{
        System.setProperty("java.awt.headless", "true");

        File spritesDir = new File(SPRITES_DIR);
        if(!spritesDir.isDirectory()){
            throw new IllegalStateException("Sprites directory not found: " + spritesDir.getAbsolutePath()
                    + " (run from the repository root)");
        }

        deleteOldIcons(spritesDir);

        int count = 0;
        for(Item item : Item.getAllItems()){
            CodeAttr attr = CodeItem.attrOf(item);
            if(attr == null) continue;

            BufferedImage base = readBase(attr.stage, attr.importance);
            BufferedImage icon = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for(int y = 0; y < SIZE; y++){
                for(int x = 0; x < SIZE; x++){
                    icon.setRGB(x, y, base.getRGB(x, y));
                }
            }

            if((attr.passed & CodeItem.REVIEW) != 0) icon.setRGB(REVIEW_X, DOT_Y, REVIEW_COLOR);
            if((attr.passed & CodeItem.TEST) != 0) icon.setRGB(TEST_X, DOT_Y, TEST_COLOR);
            if((attr.passed & CodeItem.CI) != 0) icon.setRGB(CI_X, DOT_Y, CI_COLOR);

            File out = new File(spritesDir, "icon-" + item.name + ".png");
            if(!ImageIO.write(icon, "png", out)){
                throw new IOException("No PNG writer available for " + out.getPath());
            }
            count++;
        }

        System.out.println("IconGenerator: wrote " + count + " icons to " + SPRITES_DIR);
        if(count != EXPECTED_COUNT){
            throw new IllegalStateException("Expected " + EXPECTED_COUNT + " code icons, wrote " + count);
        }
    }

    private static void deleteOldIcons(File spritesDir) throws IOException{
        File[] files = spritesDir.listFiles();
        if(files == null){
            throw new IOException("Cannot list " + spritesDir.getPath());
        }
        for(File file : files){
            String name = file.getName();
            if(file.isFile() && name.startsWith(OUTPUT_PREFIX) && name.endsWith(".png")){
                if(!file.delete()){
                    throw new IOException("Cannot delete " + file.getPath());
                }
            }
        }
    }

    private static BufferedImage readBase(int stage, int importance) throws IOException{
        File file = new File(BASE_DIR, "q" + stage + "-i" + importance + ".png");
        if(!file.isFile()){
            throw new IOException("Base image not found: " + file.getPath());
        }
        BufferedImage image = ImageIO.read(file);
        if(image == null){
            throw new IOException("Not a readable image: " + file.getPath());
        }
        if(image.getWidth() != SIZE || image.getHeight() != SIZE){
            throw new IOException("Base image must be " + SIZE + "x" + SIZE + ": " + file.getPath()
                    + " is " + image.getWidth() + "x" + image.getHeight());
        }
        int[] dotsX = {REVIEW_X, TEST_X, CI_X};
        for(int x : dotsX){
            if((image.getRGB(x, DOT_Y) >>> 24) != 0){
                throw new IOException("Dot pixel (" + x + "," + DOT_Y + ") must be transparent: " + file.getPath());
            }
        }
        return image;
    }
}
