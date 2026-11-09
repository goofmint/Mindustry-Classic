package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.resource.CodeItem;
import io.anuke.mindustry.resource.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** GateLogic の単体テスト。状態は libGDX に依存しない最小の実装で持つ */
public class GateLogicTest {

	private static final class Stub implements GateLogic.GateState{
		Item current;
		float progress;
		float power;

		@Override public Item getCurrent(){ return current; }
		@Override public void setCurrent(Item item){ current = item; }
		@Override public float getProgress(){ return progress; }
		@Override public void setProgress(float progress){ this.progress = progress; }
		@Override public float getPower(){ return power; }
		@Override public void setPower(float power){ this.power = power; }
	}

	private static final float EPS = 1e-4f;

	@Test
	public void emptyGateDoesNothing(){
		Stub s = new Stub();
		s.power = 5f;
		assertNull(GateLogic.update(s, CodeItem.TEST, 1, 2f, 0.3f, 1f));
		assertEquals(0f, s.progress, 0f);
		assertEquals(5f, s.power, 0f);
	}

	@Test
	public void progressIsSecondsTimesSixtyFrames(){
		Stub s = new Stub();
		s.current = CodeItem.get(1, 0, 1);
		s.power = 5f;

		//2 秒 = 120 フレーム。delta 1 を 60 回で半分
		for(int i = 0; i < 60; i++){
			assertNull(GateLogic.update(s, CodeItem.TEST, 1, 2f, 0.3f, 1f));
		}
		assertEquals(0.5f, s.progress, EPS);
		assertEquals(5f, s.power, 0f);
	}

	@Test
	public void powerShortageFreezesProgress(){
		Stub s = new Stub();
		s.current = CodeItem.get(1, 0, 1);
		s.progress = 0.4f;
		s.power = 0.29f;

		for(int i = 0; i < 1000; i++){
			assertNull(GateLogic.update(s, CodeItem.TEST, 1, 2f, 0.3f, 1f));
		}
		assertEquals(0.4f, s.progress, 0f);
		assertEquals(0.29f, s.power, 0f);
		assertSame(CodeItem.get(1, 0, 1), s.current);
	}

	@Test
	public void zeroPowerGateRunsWithoutPower(){
		Stub s = new Stub();
		s.current = CodeItem.get(2, 0, 2);

		//5 秒 = 300 フレーム(float の積算誤差で 1 フレーム遅れることがある)
		Item result = null;
		int frames = 0;
		while(result == null && frames < 400){
			result = GateLogic.update(s, CodeItem.REVIEW, 1, 5f, 0f, 1f);
			frames++;
		}
		assertSame(CodeItem.get(3, CodeItem.REVIEW, 2), result);
		assertTrue("frames=" + frames, frames == 300 || frames == 301);
		assertEquals(0f, s.power, 0f);
	}

	@Test
	public void completionDeductsPowerAndResets(){
		Stub s = new Stub();
		s.current = CodeItem.get(1, 0, 1);
		s.progress = 0.99f;
		s.power = 1f;

		Item result = GateLogic.update(s, CodeItem.TEST, 1, 2f, 0.3f, 3f);

		assertSame(CodeItem.get(2, CodeItem.TEST, 1), result);
		assertNull(s.current);
		assertEquals(0f, s.progress, 0f);
		assertEquals(0.7f, s.power, EPS);
	}

	@Test
	public void sameCategoryTwiceReturnsSameItem(){
		Item tested = CodeItem.get(2, CodeItem.TEST, 1);
		Stub s = new Stub();
		s.current = tested;
		s.progress = 0.999f;
		s.power = 1f;

		Item result = GateLogic.update(s, CodeItem.TEST, 1, 2f, 0.3f, 1f);

		assertSame(tested, result);
		//処理時間と電力は使う
		assertEquals(0.7f, s.power, EPS);
	}

	@Test
	public void stagesAreCappedAtMax(){
		Stub s = new Stub();
		s.current = CodeItem.get(3, CodeItem.REVIEW | CodeItem.TEST, 1);
		s.progress = 0.999f;
		s.power = 1f;

		Item result = GateLogic.update(s, CodeItem.CI, 1, 3f, 0.4f, 1f);
		assertSame(CodeItem.get(4, CodeItem.REVIEW | CodeItem.TEST | CodeItem.CI, 1), result);
	}

	@Test
	public void acceptsOnlyCodeWhenIdleAndNoPendingOutput(){
		Stub s = new Stub();
		Item code = CodeItem.get(0, 0, 1);

		assertTrue(GateLogic.accepts(s, code, false));
		assertFalse(GateLogic.accepts(s, Item.igpu, false));
		assertFalse(GateLogic.accepts(s, Item.credit, false));
		assertFalse(GateLogic.accepts(s, code, true));

		s.current = code;
		assertFalse(GateLogic.accepts(s, CodeItem.get(1, 0, 1), false));
	}
}
