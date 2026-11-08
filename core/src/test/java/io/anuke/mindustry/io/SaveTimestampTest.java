// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Test;

/** SaveFileVersion.readTimestamp が writeLong で書いた値をそのまま返すことの確認(JVM での動作) */
public class SaveTimestampTest{

    private static long roundTrip(long value) throws IOException{
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeLong(value);
        out.writeInt(0x12345678);
        out.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        long read = SaveFileVersion.readTimestamp(in);
        // 8 バイトだけ読み、後続のフィールドがずれないこと
        assertEquals(0x12345678, in.readInt());
        return read;
    }

    @Test
    public void lowWordWithBit31SetIsNotSignExtended() throws IOException{
        long value = 1762000000000L | 0x80000000L;
        assertEquals(0x80000000L, value & 0x80000000L);
        assertEquals(value, roundTrip(value));
    }

    @Test
    public void lowWordWithBit31ClearIsReadAsIs() throws IOException{
        long value = 1762000000000L & ~0x80000000L;
        assertEquals(0L, value & 0x80000000L);
        assertEquals(value, roundTrip(value));
    }

    @Test
    public void edgeValuesRoundTrip() throws IOException{
        assertEquals(0L, roundTrip(0L));
        assertEquals(-1L, roundTrip(-1L));
        assertEquals(Long.MAX_VALUE, roundTrip(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, roundTrip(Long.MIN_VALUE));
    }
}
