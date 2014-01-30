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

package tv.ouya.console.api;

import android.content.Context;
import android.os.Bundle;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.Assert.assertEquals;

public class TestOuyaFacade extends OuyaFacade {
    private ArrayList<Product> products = new ArrayList<Product>();
    private OuyaResponseListener<ArrayList<Product>> productListListener;
    private List<Purchasable> expectedProductListIds;
    private Purchasable purchaseRequestId;
    private Purchasable expectedPurchaseRequestId;
    private OuyaResponseListener<String> purchaseRequestListener;
    private Context contextFromInit;
    private String developerId;
    private String receipt = new String();
    private OuyaResponseListener<String> receiptListListener;
    private boolean shutdownWasCalled;
    private boolean requestReceiptsWasCalled;
    private OuyaResponseListener<GamerInfo> gamerInfoListener;

    public TestOuyaFacade() {
        OuyaFacade.setInstance(this);
        clear();
    }

    public void addProducts(Product... products) {
        this.products.addAll(Arrays.asList(products));
    }

    @Override
    public void init(Context context, String developerId) {
        this.contextFromInit = context;
        this.developerId = developerId;
    }

    @Override
    public void requestProductList(List<Purchasable> purchasables, OuyaResponseListener<ArrayList<Product>> productListListener) {
        this.productListListener = productListListener;
        if (expectedProductListIds != null) {
            assertEquals(expectedProductListIds, purchasables);
        }
    }

    @Override
    public void requestPurchase(Purchasable purchasable, OuyaResponseListener<String> purchaseListener) {
        this.purchaseRequestId = purchasable;
        this.purchaseRequestListener = purchaseListener;
        if (expectedPurchaseRequestId != null) {
            assertEquals(expectedPurchaseRequestId, purchasable);
        }
    }

    @Override
    public void requestReceipts(OuyaResponseListener<String> receiptListListener) {
        this.receiptListListener = receiptListListener;
        requestReceiptsWasCalled = true;
    }

    @Override
    public void requestGamerInfo(OuyaResponseListener<GamerInfo> gamerUuidListener) {
        this.gamerInfoListener = gamerUuidListener;
    }

    @Override
    public void shutdown() {
        shutdownWasCalled = true;
    }

    public void simulateProductListSuccessResponse() {
        productListListener.onSuccess(products);
    }

    public void expectRequestedIDs(List<Purchasable> expectedIDs) {
        this.expectedProductListIds = expectedIDs;
    }

    public void expectPurchaseRequestID(String expectedID) {
        this.expectedPurchaseRequestId = new Purchasable(expectedID);
    }

    public boolean requestPurchaseWasCalled() {
        return this.purchaseRequestId != null;
    }

    public boolean shutdownWasCalled() {
        return shutdownWasCalled;
    }

    public void simulatePurchaseSuccessResponse(String product) {
        purchaseRequestListener.onSuccess(product);
    }

    public void simulatePurchaseFailureResponse(int errorCode, String errorMessage, final Bundle optionalData) {
        purchaseRequestListener.onFailure(errorCode, errorMessage, optionalData);
    }

    public void simulateProductListFailure(int errorCode, String errorMessage, final Bundle optionalData) {
        productListListener.onFailure(errorCode, errorMessage, optionalData);
    }

    public void simulateReceiptListFailure(int errorCode, String errorMessage, final Bundle optionalData) {
        receiptListListener.onFailure(errorCode, errorMessage, optionalData);
    }

    public Context getContextFromInit() {
        return contextFromInit;
    }

    public String getDeveloperId() {
        return developerId;
    }

    public void clear() {
        products = newArrayList();
        productListListener = null;
        expectedProductListIds = null;
        purchaseRequestId = null;
        expectedPurchaseRequestId = null;
        purchaseRequestListener = null;
    }

    public void addReceipts(ArrayList<Receipt> receipts) {
        OuyaEncryptionHelper helper = new OuyaEncryptionHelper();
        List<Receipt> currentReceipts = new ArrayList<Receipt>();
        try {
            if (receipt != null && !receipt.isEmpty()) {
                currentReceipts = helper.parseJSONReceiptResponse(receipt);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currentReceipts.addAll(receipts);

        Receipt[] receiptArray = currentReceipts.toArray(new Receipt[currentReceipts.size()]);
        ObjectMapper mapper = new ObjectMapper();
        try {
            receipt = mapper.writeValueAsString(receiptArray);
            // TODO: add fake encryption here (and remove the "if" in decryptReceiptResponse)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void simulateReceiptListSuccess() {
        receiptListListener.onSuccess(receipt);
    }

    public void simulateGamerInfoSuccess(String gamerUuid, String gamerUsername) {
        gamerInfoListener.onSuccess(new GamerInfo(gamerUuid, gamerUsername));
    }

    public void simulateGamerInfoFailure(int errorCode, String errorMessage, final Bundle optionalData) {
        gamerInfoListener.onFailure(errorCode, errorMessage, new Bundle());
    }

    public boolean requestReceiptsWasCalled() {
        return requestReceiptsWasCalled;
    }

    public void resetCalledFlags() {
        requestReceiptsWasCalled = shutdownWasCalled = false;
        purchaseRequestId = null;
    }
}
