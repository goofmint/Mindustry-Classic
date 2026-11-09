// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.anuke.mindustry.core.Logic.Outcome;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.resource.Item;
import org.junit.Before;
import org.junit.Test;

public class LogicOutcomeTest{
    private GameState state;

    @Before
    public void setUp(){
        state = new GameState();
        state.mode = GameMode.waves;
        state.score.reset(10);
    }

    /** 残高を 0 未満にする納品で破産を立てる */
    private void goBankrupt(){
        state.score.deliver(Item.igpu, -20f);
        assertTrue(state.score.bankrupt());
    }

    @Test
    public void noneWhileTargetNotReachedAndNotBankrupt(){
        state.score.target = 100;
        assertEquals(Outcome.none, Logic.evaluateOutcome(state));
    }

    @Test
    public void wonWhenTargetReached(){
        state.score.target = 10;
        assertEquals(Outcome.won, Logic.evaluateOutcome(state));
    }

    @Test
    public void bankruptWhenBelowZero(){
        state.score.target = 100;
        goBankrupt();
        assertEquals(Outcome.bankrupt, Logic.evaluateOutcome(state));
    }

    @Test
    public void targetZeroNeverWinsButCanGoBankrupt(){
        state.score.target = 0;
        assertEquals(Outcome.none, Logic.evaluateOutcome(state));
        goBankrupt();
        assertEquals(Outcome.bankrupt, Logic.evaluateOutcome(state));
    }

    @Test
    public void wonIsCheckedBeforeBankrupt(){
        goBankrupt();
        state.score.deliver(Item.igpu, 5f);
        state.score.target = 5;
        assertEquals(Outcome.won, Logic.evaluateOutcome(state));
    }

    @Test
    public void sandboxNeverEndsByScore(){
        state.mode = GameMode.sandbox;
        state.score.target = 10;
        assertEquals(Outcome.none, Logic.evaluateOutcome(state));
        goBankrupt();
        assertEquals(Outcome.none, Logic.evaluateOutcome(state));
    }
}
