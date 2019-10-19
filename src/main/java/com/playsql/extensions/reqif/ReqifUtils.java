package com.playsql.extensions.reqif;

/*-
 * #%L
 * Play SQL - ReqIF Import
 * %%
 * Copyright (C) 2019 Play SQL S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
