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

import com.google.common.collect.Lists;
import org.junit.Test;
import tv.ouya.console.internal.util.Strings;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static tv.ouya.console.internal.util.Strings.isEmptyOrWhitespace;

public class StringsTest {

    @Test
    public void testIsEmptyOrWhitespace() {
        assertTrue(isEmptyOrWhitespace(""));
        assertTrue(isEmptyOrWhitespace(" "));
        assertTrue(isEmptyOrWhitespace(" \n \t "));
        assertFalse(isEmptyOrWhitespace("foo"));
        assertFalse(isEmptyOrWhitespace(" bar "));
        assertFalse(isEmptyOrWhitespace(" \tbar \n"));
    }

    @Test
    public void canFormatDollarAmounts() throws Exception {
        assertEquals("$0.00", Strings.formatDollarAmount(0));
        assertEquals("$0.01", Strings.formatDollarAmount(1));
        assertEquals("$3.00", Strings.formatDollarAmount(300));
        assertEquals("$3.35", Strings.formatDollarAmount(335));
    }

    @Test
    public void canReadFileGivenName() throws Exception {
        assertTrue(Strings.fromFile("AndroidManifest.xml").startsWith("<?xml"));
    }

    @Test
    public void join_shouldNotHaveSeparatorWithSingleEntry() throws Exception {
        assertEquals("foo", Strings.join("~", "foo"));
    }

    @Test
    public void join_shouldHaveSeparatorBetweenMultipleEntries() throws Exception {
        assertEquals("foo~bar", Strings.join("~", "foo", "bar"));
    }

    @Test
    public void join_canAcceptCollection() throws Exception {
        ArrayList<String> strings = Lists.newArrayList("abc", "def");
        assertEquals("abc~def", Strings.join("~", strings));
    }

    @Test
    public void hasLength_onlyIsTrueWhenNotNullAndNotEmpty() throws Exception {
        assertFalse(Strings.hasLength(null));
        assertFalse(Strings.hasLength(""));
        assertTrue(Strings.hasLength("a"));
    }
}
