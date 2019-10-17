package com.playsql.extensions.reqif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ReqifUtils {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT_HUMAN = new SimpleDateFormat("EEEE dd MMMM yyyy 'at' HH:mm:ss");
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static void closeSilently(Closeable dataStream) {
        if (dataStream != null) {
            try {
                dataStream.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    public static int between(Integer min, int number, Integer max) {
        if (min != null)
            number = Math.max(min, number);
        if (max != null)
            number = Math.min(max, number);
        return number;
    }
}
