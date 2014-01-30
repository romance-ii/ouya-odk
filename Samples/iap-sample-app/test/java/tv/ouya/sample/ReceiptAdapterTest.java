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

package tv.ouya.sample;

import android.view.View;
import android.widget.TextView;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import tv.ouya.console.api.Receipt;

import java.util.Date;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
public class ReceiptAdapterTest {
    private ReceiptAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new ReceiptAdapter(Robolectric.application, new Receipt[] {
                new Receipt("one", 100, new Date(1000000000L), new Date(0), "gamer", "uuid", 1.00, "USD"),
                new Receipt("two", 200, new Date(2000000000L), new Date(0), "gamer", "uuid", 2.00, "USD"),
                new Receipt("three", 300, new Date(3000000000L), new Date(0), "gamer", "uuid", 3.00, "USD")});
        forceLosAngeles();
    }

    private void forceLosAngeles() {
        adapter.setTimezone(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @Test
    public void getItemId_shouldReturnIndex() throws Exception {
        assertEquals(666L, adapter.getItemId(666));
    }

    @Test
    public void getView_canCreateNewView() throws Exception {
        View view = adapter.getView(0, null, null);
        assertEquals("1970-01-12 05:46:40",((TextView) view.findViewById(R.id.date)).getText());
        assertEquals("one", ((TextView) view.findViewById(R.id.productId)).getText());
        assertEquals("$1.00", ((TextView) view.findViewById(R.id.price)).getText());
    }

    @Test
    public void getView_canUseRecycledView() throws Exception {
        View viewToRecycle = adapter.getView(0, null, null);
        View view = adapter.getView(1, viewToRecycle, null);
        assertSame(viewToRecycle, view);
        assertEquals("1970-01-23 19:33:20", ((TextView) view.findViewById(R.id.date)).getText());
        assertEquals("two", ((TextView) view.findViewById(R.id.productId)).getText());
        assertEquals("$2.00", ((TextView) view.findViewById(R.id.price)).getText());
    }

    @Test
    public void canSwitchTimezonesForTestingPurposes() throws Exception {
        adapter.setTimezone(TimeZone.getTimeZone("America/New_York"));
        View atlanticView = adapter.getView(0, null, null);
        assertEquals("1970-01-12 08:46:40", ((TextView) atlanticView.findViewById(R.id.date)).getText());
        forceLosAngeles();
        View pacificView = adapter.getView(0, null, null);
        assertEquals("1970-01-12 05:46:40", ((TextView) pacificView.findViewById(R.id.date)).getText());
    }
}
