// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import io.anuke.mindustry.game.Difficulty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SaveFileVersion {
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = readTimestamp(stream); //read last saved time
        byte mode = stream.readByte(); //read the gamemode
        byte map = stream.readByte(); //read the map
        int wave = stream.readInt(); //read the wave
        return new SaveMeta(version, time, mode, map, wave, Difficulty.normal);
    }

    public abstract void read(DataInputStream stream) throws IOException;
    public abstract void write(DataOutputStream stream) throws IOException;

    /**
     * Reads the 8-byte "last saved" time written by {@code writeLong}.
     * libGDX 1.13.1's GWT emulation of DataInputStream.readLong() sign-extends the low word,
     * so saves whose low word has bit 31 set show a date around 1969 in the browser
     * (fixed upstream in libGDX PR #7590). Combining two readInt() calls avoids that and is
     * identical to readLong() on the JVM, so it stays correct after a libGDX update.
     */
    protected static long readTimestamp(DataInputStream stream) throws IOException{
        long high = stream.readInt();
        long low = stream.readInt() & 0xffffffffL;
        return (high << 32) | low;
    }

    public static void writeString(DataOutputStream stream, String string) throws IOException{
        stream.writeByte(string.length());
        stream.write(string.getBytes("UTF-8"));
    }

    public static String readString(DataInputStream stream) throws IOException{
        int length = stream.readByte();
        byte[] result = new byte[length];
        stream.read(result);
        return new String(result, "UTF-8");
    }
}
