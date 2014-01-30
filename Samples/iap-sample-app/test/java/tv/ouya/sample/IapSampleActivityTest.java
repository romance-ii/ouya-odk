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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowAlertDialog;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import tv.ouya.console.api.OuyaFacade;
import tv.ouya.console.api.Product;
import tv.ouya.console.api.Receipt;
import tv.ouya.console.api.TestOuyaFacade;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class IapSampleActivityTest {
    private IapSampleActivity activity;
    private TestOuyaFacade ouyaFacade;
    private LinearLayout productListView;
    private ListView receiptListView;

    @Before
    public void setup() {
        ouyaFacade = new TestOuyaFacade();
        assertSame(OuyaFacade.getInstance(), ouyaFacade);

        activity = new IapSampleActivity();
        callOnCreateAndFindViews();
    }

    public static Receipt newReceipt(String product, int price, String date, String gamer, String uuid, double localPrice, String currency) throws ParseException {
        SimpleDateFormat dateParser = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
        dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new Receipt(product, price, dateParser.parse(date), new Date(0), gamer, uuid, localPrice, currency);
    }

    @Test
    public void onCreate_initializesFacade() throws Exception {
        assertSame(activity, ouyaFacade.getContextFromInit());
        assertEquals(IapSampleActivity.DEVELOPER_ID, ouyaFacade.getDeveloperId());
    }

    @Test
    public void onCreate_shouldSetReceiptListViewNonFocusable() throws Exception {
        assertFalse(activity.findViewById(R.id.receipts).isFocusable());
    }

    @Test
    public void canShowProductInListView() throws Exception {
        Product product = new Product("SKU1", "red sock", 100, 1., "USD", 1., 0, "");
        activity.addProducts(Arrays.asList(product));
        assertEquals("red sock - $1.00", getButton(0).getText().toString());
    }

    private Button getButton(int index) {
        return ((Button) ((LinearLayout) productListView.getChildAt(index)).getChildAt(1));
    }

    @Test
    public void shouldGetProductsFromGateway() throws Exception {
        Product product1 = new Product("SKU1", "red sock", 100, 1., "USD", 1., 0, "");
        Product product2 = new Product("SKU2", "green sock", 100, 1., "USD", 1., 0, "");
        Product product3 = new Product("SKU3", "blue sock", 100, 1., "USD", 1., 0, "");
        ouyaFacade.addProducts(product1, product2, product3);
        ouyaFacade.expectRequestedIDs(IapSampleActivity.PRODUCT_IDENTIFIER_LIST);
        callOnCreateAndFindViews();
        ouyaFacade.simulateProductListSuccessResponse();

        assertEquals("green sock - $1.00", getButton(1).getText().toString());
    }

    @Ignore
    @Test
    public void redisplayingReceiptsShouldClearOutPreviousReceipts() throws Exception {
        Product product = new Product("SKU1", "red sock", 100, 1., "USD", 1., 0, "");
        initializeWithProducts(product);

        ArrayList<Receipt> receipts = new ArrayList<Receipt>();
        receipts.add(newReceipt("sku2", 874, "1987-12-31T16:00:00Z", "gamer", "uuid", 8.74, "USD"));
        receipts.add(newReceipt("sku1", 123, "1999-12-31T16:00:00Z", "gamer", "uuid", 1.23, "USD"));
        ouyaFacade.addReceipts(receipts);
        ouyaFacade.simulateReceiptListSuccess();
        assertEquals(2, ((ListView) activity.findViewById(R.id.receipts)).getAdapter().getCount());

        activity.requestPurchase(product);
        ouyaFacade.simulatePurchaseSuccessResponse("{ \"identifier\":\"SKU1\", \"name\":\"red sock\", \"priceInCents\":\"100\"}");

        receipts = new ArrayList<Receipt>();
        receipts.add(newReceipt("sku1", 123, "2001-12-31T16:00:00Z", "gamer", "uuid", 1.23, "USD"));
        ouyaFacade.addReceipts(receipts);
        ouyaFacade.simulateReceiptListSuccess();
        assertEquals(3, ((ListView) activity.findViewById(R.id.receipts)).getCount());
    }

    @Ignore
    @Test
    public void shouldDisplayAllReceiptsWithNewestReceiptFirst() throws Exception {
        ArrayList<Receipt> receipts = new ArrayList<Receipt>();
        receipts.add(newReceipt("sku2", 874, "1987-12-31T16:00:00Z", "gamer", "uuid", 8.74, "USD"));
        receipts.add(newReceipt("sku1", 123, "1999-12-31T16:00:00Z", "gamer", "uuid", 1.23, "USD"));
        ouyaFacade.addReceipts(receipts);
        ouyaFacade.simulateReceiptListSuccess();
        assertEquals(2, receiptListView.getCount());
        View child0 = receiptListView.getChildAt(0);

        SimpleDateFormat dateParser = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
        dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = dateParser.parse("1999-12-31T16:00:00Z");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String correctDisplayDate = simpleDateFormat.format(date);

        assertEquals("sku1", ((TextView) child0.findViewById(R.id.productId)).getText());
        assertEquals(correctDisplayDate, ((TextView) child0.findViewById(R.id.date)).getText());
        assertEquals("$1.23", ((TextView) child0.findViewById(R.id.price)).getText());
    }

    @Test
    public void shouldShutDownFacadeOnDestroy() throws Exception {
        activity.onDestroy();
        assertTrue(ouyaFacade.shutdownWasCalled());
    }

    @Test
    public void productListFailure_shouldShowToast() throws Exception {
        ouyaFacade.simulateProductListFailure(5544, "SKU is bad", new Bundle());
        assertEquals("Could not fetch product information (error 5544: SKU is bad)", ShadowToast.getTextOfLatestToast());
    }

    @Ignore
    @Test
    public void receiptFailure_shouldPopToast() throws Exception {
        ouyaFacade.simulateReceiptListFailure(3423, "Some failure", new Bundle());
        assertEquals("Could not fetch receipts (error 3423: Some failure)", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void clickingUuid_fetchesUuidAndPopsAlert() throws Exception {
        Robolectric.clickOn(activity.findViewById(R.id.gamer_uuid_button));
        ouyaFacade.simulateGamerInfoSuccess("myUuid", "myUsername");
        assert(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().contains("myUuid"));
        assert(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().contains("myUsername"));
        assertEquals("IAP Sample App", shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getTitle());
    }

    @Test
    public void gamerUuidFailure_shouldPopToast() throws Exception {
        Robolectric.clickOn(activity.findViewById(R.id.gamer_uuid_button));
        ouyaFacade.simulateGamerInfoFailure(7766, "not fetchable", new Bundle());
        assertEquals("Unable to fetch gamer UUID (error 7766: not fetchable)", ShadowToast.getTextOfLatestToast());
    }

    private void initiateAFailingPurchase(int errorCode, String errorMessage)
            throws GeneralSecurityException, UnsupportedEncodingException, JSONException {
        Product product = new Product("BAD_SKU", "bogus thing", 100, 1., "USD", 1., 0, "");
        initializeWithProducts(product);

        activity.requestPurchase(product);
        ouyaFacade.simulatePurchaseFailureResponse(errorCode, errorMessage, new Bundle());
    }

    private void initializeWithProducts(Product... products) {
        ouyaFacade.addProducts(products);
        callOnCreateAndFindViews();
        ouyaFacade.simulateProductListSuccessResponse();
    }

    private void callOnCreateAndFindViews() {
        activity.onCreate(null);
        productListView = ((LinearLayout) activity.findViewById(R.id.products));
        receiptListView = ((ListView) activity.findViewById(R.id.receipts));
    }
}
