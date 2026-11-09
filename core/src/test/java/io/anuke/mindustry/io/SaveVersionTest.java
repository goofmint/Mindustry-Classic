// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/** 旧セーブ(本家の Save15)が読み込み対象から外れ、現在の形式が 16 であることを確かめる */
public class SaveVersionTest{

    @Test
    public void currentVersionIs16(){
        assertEquals(16, SaveIO.getVersion().version);
        assertEquals(1, SaveIO.versionArray.size);
    }

    @Test
    public void oldSave15IsNotRegistered(){
        assertNull(SaveIO.versions.get(15));
    }

    @Test
    public void oldSave15StreamIsInvalid() throws IOException{
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(15);
        out.writeLong(0L);
        out.close();
        assertFalse(SaveIO.isSaveValid(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}
