// Modified for the AI SDLC browser game fork (goofmint/Mindustry-Classic) by Atsushi Nakatsugawa, 2026. See LICENSE (GPL-3.0).
package io.anuke.mindustry.desktop;

import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.mindustry.core.Platform;
import io.anuke.ucore.core.Settings;

import javax.swing.*;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Random;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform {
    final static DateFormat format = SimpleDateFormat.getDateTimeInstance();
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;
    }

    @Override
    public String format(Date date){
        return format.format(date);
    }

    @Override
    public String format(int number){
        return NumberFormat.getIntegerInstance().format(number);
    }

    @Override
    public void showError(String text){
        JOptionPane.showMessageDialog(null, text);
    }

    @Override
    public String getLocaleName(Locale locale){
        return locale.getDisplayName(locale);
    }

    @Override
    public void updateRPC() {
    }

    @Override
    public void onGameExit() {
    }

    @Override
    public boolean isDebug() {
        return args.length > 0 && args[0].equalsIgnoreCase("-debug");
    }

    @Override
    public ThreadProvider getThreadProvider() {
        return new DefaultThreadImpl();
    }

    @Override
    public byte[] getUUID() {
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            NetworkInterface out;
            for(out = e.nextElement(); out.getHardwareAddress() == null && e.hasMoreElements() && validAddress(out.getHardwareAddress()); out = e.nextElement());

            byte[] bytes = out.getHardwareAddress();
            byte[] result = new byte[8];
            System.arraycopy(bytes, 0, result, 0, bytes.length);

            if(new String(Base64Coder.encode(result)).equals("AAAAAAAAAOA=")) throw new RuntimeException("Bad UUID.");

            return result;
        }catch (Exception e){
            Settings.defaults("uuid", "");

            String uuid = Settings.getString("uuid");
            if(uuid.isEmpty()){
                byte[] result = new byte[8];
                new Random().nextBytes(result);
                uuid = new String(Base64Coder.encode(result));
                Settings.putString("uuid", uuid);
                Settings.save();
                return result;
            }
            return Base64Coder.decode(uuid);
        }
    }

    private boolean validAddress(byte[] bytes){
        byte[] result = new byte[8];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        return !new String(Base64Coder.encode(result)).equals("AAAAAAAAAOA=");
    }
}
