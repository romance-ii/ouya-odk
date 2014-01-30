/*
 * Copyright (C) 2012 OUYA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.ouya.sample.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

public class Strings {
    public static boolean isEmptyOrWhitespace(String s) {
        return (s == null) || (s.trim().length() == 0);
    }

    public static String fromStream(InputStream inputStream) throws IOException {
        char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, "UTF-8");
        try {
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                out.append(buffer, 0, read);
            }
            return out.toString();
        } finally {
            in.close();
            inputStream.close();
        }
    }

    public static InputStream asStream(String string) {
        return new ByteArrayInputStream(string.getBytes());
    }

    public static String formatDollarAmount(int amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        return "$" + currencyFormatter.format((float)amount / 100f).substring(1);
    }

    // This is only used in tests. If you ever use it in production code, you should do something smarter
    // than just wrapping the IOException in a RuntimeException.
    public static String fromFile(String filename) {
        try {
            return fromStream(new FileInputStream(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String join(String delimiter, Collection collection) {
        return join(delimiter, collection.toArray());
    }

    public static String join(String delimiter, Object... collection) {
        String del = "";
        StringBuilder sb = new StringBuilder();
        for (Object obj : collection) {
            String asString = obj == null ? null : obj.toString();
            if (asString != null && asString.length() > 0) {
                sb.append(del).append(asString);
                del = delimiter;
            }
        }
        return sb.toString();
    }

    public static boolean hasLength(String str) {
        return str != null && str.length() > 0;
    }
}
