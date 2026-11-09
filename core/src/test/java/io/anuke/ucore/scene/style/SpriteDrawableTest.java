package io.anuke.ucore.scene.style;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;

/** Regression test for #52: drawing must not decay the shared sprite tint or alter the batch colour. */
public class SpriteDrawableTest{
	private static final float EPS = 1e-3f;

	/** Minimal Batch that only keeps its colour; every other call is ignored. */
	private static Batch fakeBatch(final Color color){
		return (Batch)Proxy.newProxyInstance(Batch.class.getClassLoader(), new Class<?>[]{Batch.class}, new InvocationHandler(){
			public Object invoke(Object proxy, Method m, Object[] a){
				String n = m.getName();
				if(n.equals("getColor")) return color;
				if(n.equals("getPackedColor")) return color.toFloatBits();
				if(n.equals("setPackedColor")){
					Color.abgr8888ToColor(color, (Float)a[0]);
					return null;
				}
				if(n.equals("setColor")){
					if(a.length == 1) color.set((Color)a[0]);
					else color.set((Float)a[0], (Float)a[1], (Float)a[2], (Float)a[3]);
					return null;
				}
				return null;
			}
		});
	}

	private static void check(float batchAlpha, float tintAlpha){
		Sprite sprite = new Sprite();
		sprite.setColor(1f, 1f, 1f, tintAlpha);
		SpriteDrawable drawable = new SpriteDrawable(sprite);
		Color batchColor = new Color(1f, 1f, 1f, batchAlpha);
		Batch batch = fakeBatch(batchColor);
		float packedBefore = batch.getPackedColor();

		for(int i = 0; i < 20; i++){
			drawable.draw(batch, 0, 0, 10, 10);
			assertEquals(tintAlpha, sprite.getColor().a, EPS);
			drawable.draw(batch, 0, 0, 5, 5, 10, 10, 1, 1, 0);
			assertEquals(tintAlpha, sprite.getColor().a, EPS);
			assertEquals(packedBefore, batch.getPackedColor(), 0f);
		}
	}

	@Test public void tintSurvivesRepeatedDrawAlphaZero(){ check(0f, 0.9f); }
	@Test public void tintSurvivesRepeatedDrawAlphaPartial(){ check(0.5f, 0.9f); }
	@Test public void tintSurvivesRepeatedDrawAlphaOne(){ check(1f, 0.9f); }
}
