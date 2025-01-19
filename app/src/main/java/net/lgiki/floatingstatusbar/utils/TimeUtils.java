package net.lgiki.floatingstatusbar.utils;

public class TimeUtils {
    public static String getTimePattern(boolean is24HourFormat, boolean showSeconds) {
        String hourPattern = is24HourFormat ? "HH" : "hh";
        String basicPattern = showSeconds ? ":mm:ss" : ":mm";
        String patternSuffix = is24HourFormat ? "" : " aa";
        return hourPattern + basicPattern + patternSuffix;
    }
}
