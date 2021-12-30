package com.arashivision.sdk.demo.util;

import java.util.Locale;

public class TimeFormat {

    // Format as 00:00:00
    public static String durationFormat(long msec) {
        msec = (long) (msec / 1000f);
        long hour = msec / 3600;
        long minute = msec % 3600 / 60;
        long second = msec % 3600 % 60;

        return hour == 0
                ? String.format(Locale.getDefault(), "%02d:%02d", minute, second)
                : String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second);
    }

}
