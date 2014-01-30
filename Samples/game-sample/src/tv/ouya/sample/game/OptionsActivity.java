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

package tv.ouya.sample.game;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import tv.ouya.console.api.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tv.ouya.sample.game.Options.Level.*;
import static tv.ouya.sample.game.Options.getInstance;

public class OptionsActivity extends Activity {
    /**
     * The Log Tag for this Activity
     */

    private static final String LOG_TAG = "OptionsActivity";

    /**
     * The application key. This is used to decrypt encrypted receipt responses. This should be replaced with the
     * application key obtained from the OUYA developers website.
     */

    private static byte[] applicationKey;


    private Map<Options.Level, RadioButton> levelToRadioButton;
    private Map<RadioButton, Options.Level> radioButtonToLevel;

    static private final String PREV_SELECTED_LEVEL_KEY = "last_selected_level";

    private static final String DEVELOPER_ID = "310a8f51-4d6e-4ae5-bda0-b93878e5f5d0";

    private OuyaFacade mOuyaFacade;

    /**
     * The cryptographic key for this application
     */

    private PublicKey mPublicKey;

    /**
     * The outstanding purchase request UUIDs mapped to the purchasable the request relates to.
     */

    private final Map<String, String> mOutstandingPurchaseRequests = new HashMap<String, String>();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);

        levelToRadioButton = new HashMap<Options.Level, RadioButton>();
        levelToRadioButton.put(FREEDOM, ((RadioButton) findViewById(R.id.radio_freedom)));
        levelToRadioButton.put(ALLEYWAY, ((RadioButton) findViewById(R.id.radio_alleyway)));
        levelToRadioButton.put(BOXY, ((RadioButton) findViewById(R.id.radio_boxy)));

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.key);
            applicationKey = new byte[inputStream.available()];
            inputStream.read(applicationKey);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a reverse map
        radioButtonToLevel = new HashMap<RadioButton, Options.Level>();
        for(Options.Level level : levelToRadioButton.keySet()) {
            radioButtonToLevel.put(levelToRadioButton.get(level), level);
        }

        // Initialize the UI
        processReceipts(null);
        toggleProgressIndicator(true);

        mOuyaFacade = OuyaFacade.getInstance();
        mOuyaFacade.init(this, DEVELOPER_ID);

        String prevSelectedLevel = mOuyaFacade.getGameData(PREV_SELECTED_LEVEL_KEY);
        if (prevSelectedLevel != null) {
            Options.Level prevLevel = Options.Level.valueOf(prevSelectedLevel);
            Options.getInstance().setLevel(prevLevel);
        }

        levelToRadioButton.get(Options.getInstance().getLevel()).setChecked(true);

        Button quit = (Button) findViewById(R.id.back_button);
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(applicationKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            mPublicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to create encryption key", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestReceipts();
    }

    @Override
    protected void onDestroy() {
        mOuyaFacade.shutdown();
        super.onDestroy();
    }

    private void requestReceipts() {
        mOuyaFacade.requestReceipts(new OuyaResponseListener<String>() {
            @Override
            public void onSuccess(String receiptResponse) {
                OuyaEncryptionHelper helper = new OuyaEncryptionHelper();
                List<Receipt> receipts;
                try {
                    JSONObject object = new JSONObject(receiptResponse);
                    receipts = helper.decryptReceiptResponse(object, mPublicKey);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                processReceipts(receipts);
            }

            @Override
            public void onFailure(int errorCode, String errorMessage, Bundle optionalData) {
                processReceipts(null);
                Toast.makeText(OptionsActivity.this, "Error checking purchases!\nAdditional levels not available...\n\nError " + errorCode + ": " + errorMessage + ")", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancel() {
                processReceipts(null);
                Toast.makeText(OptionsActivity.this, "You cancelled checking purchases!\nAdditional levels not available...", Toast.LENGTH_LONG).show();
            }
        });
    }

    private final String purchaseText = " [NEEDS PURCHASING]";
    private void setNeedsPurchaseText(RadioButton rb, boolean needsToBePurchased) {
        String text = rb.getText().toString();

        if (needsToBePurchased) {
            if (!text.endsWith(purchaseText)) {
                text += purchaseText;
            }
        } else {
            if (text.endsWith(purchaseText)) {
                text = text.replace(purchaseText, "");
            }
        }

        rb.setText(text);
    }

    private boolean needsPurchasing(RadioButton rb) {
        String text = rb.getText().toString();
        return text.endsWith(purchaseText);
    }

    private String getProductIdForLevel(Options.Level level) {
        switch(level) {
            case ALLEYWAY:
                return "level_alleyway";
            case BOXY:
                return "level_boxy";
        }
        return null;
    }

    private void toggleProgressIndicator(boolean progressVisible) {
        findViewById(R.id.progress_indicator).setVisibility(progressVisible ? View.VISIBLE : View.GONE);
        findViewById(R.id.levels).setVisibility(progressVisible ? View.GONE : View.VISIBLE);
    }

    private void processReceipts( List<Receipt> receipts ) {
        setNeedsPurchaseText(levelToRadioButton.get(ALLEYWAY), true);
        setNeedsPurchaseText(levelToRadioButton.get(BOXY), true);

        toggleProgressIndicator(false);

        if (receipts == null) {
            return;
        }

        for (Receipt r : receipts) {
            if (r.getIdentifier().equals(getProductIdForLevel(ALLEYWAY))) {
                setNeedsPurchaseText(levelToRadioButton.get(ALLEYWAY), false);
            } else if (r.getIdentifier().equals(getProductIdForLevel(BOXY))) {
                setNeedsPurchaseText(levelToRadioButton.get(BOXY), false);
            }
        }
    }

    private void requestPurchase(final Options.Level level)
        throws GeneralSecurityException, JSONException, UnsupportedEncodingException {
        final String productId = getProductIdForLevel(level);

        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        // This is an ID that allows you to associate a successful purchase with
        // it's original request. The server does nothing with this string except
        // pass it back to you, so it only needs to be unique within this instance
        // of your app to allow you to pair responses with requests.
        String uniqueId = Long.toHexString(sr.nextLong());

        JSONObject purchaseRequest = new JSONObject();
        purchaseRequest.put("uuid", uniqueId);
        purchaseRequest.put("identifier", productId);
        purchaseRequest.put("testing", "true"); // This value is only needed for testing, not setting it results in a live purchase
        String purchaseRequestJson = purchaseRequest.toString();

        byte[] keyBytes = new byte[16];
        sr.nextBytes(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        byte[] ivBytes = new byte[16];
        sr.nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] payload = cipher.doFinal(purchaseRequestJson.getBytes("UTF-8"));

        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, mPublicKey);
        byte[] encryptedKey = cipher.doFinal(keyBytes);

        Purchasable purchasable =
                new Purchasable(
                        productId,
                        Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
                        Base64.encodeToString(ivBytes, Base64.NO_WRAP),
                        Base64.encodeToString(payload, Base64.NO_WRAP) );

        synchronized (mOutstandingPurchaseRequests) {
            mOutstandingPurchaseRequests.put(uniqueId, productId);
        }
        mOuyaFacade.requestPurchase(purchasable, new OuyaResponseListener<String>() {
            @Override
            public void onSuccess(String result) {
                String responseProductId;
                try {
                    OuyaEncryptionHelper helper = new OuyaEncryptionHelper();

                    JSONObject response = new JSONObject(result);
                    String responseUUID = helper.decryptPurchaseResponse(response, mPublicKey);
                    synchronized (mOutstandingPurchaseRequests) {
                        responseProductId = mOutstandingPurchaseRequests.remove(responseUUID);
                    }
                    if(responseProductId == null || !responseProductId.equals(productId)) {
                        onFailure(OuyaErrorCodes.THROW_DURING_ON_SUCCESS, "Purchased product is not the same as purchase request product", Bundle.EMPTY);
                        return;
                    }
                } catch (JSONException e) {
                    onFailure(OuyaErrorCodes.THROW_DURING_ON_SUCCESS, e.getMessage(), Bundle.EMPTY);
                    return;
                } catch (ParseException e) {
                    onFailure(OuyaErrorCodes.THROW_DURING_ON_SUCCESS, e.getMessage(), Bundle.EMPTY);
                    return;
                } catch (IOException e) {
                    onFailure(OuyaErrorCodes.THROW_DURING_ON_SUCCESS, e.getMessage(), Bundle.EMPTY);
                    return;
                } catch (GeneralSecurityException e) {
                    onFailure(OuyaErrorCodes.THROW_DURING_ON_SUCCESS, e.getMessage(), Bundle.EMPTY);
                    return;
                }

                if (responseProductId.equals(getProductIdForLevel(level))) {
                    setNeedsPurchaseText(levelToRadioButton.get(level), false);
                    Toast.makeText(OptionsActivity.this, "Level purchased!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(int errorCode, String errorMessage, Bundle optionalData) {
                levelToRadioButton.get(FREEDOM).setChecked(true);
                Toast.makeText(OptionsActivity.this, "Error making purchase!\n\nError " + errorCode + "\n" + errorMessage + ")", Toast.LENGTH_LONG).show();

            }

            @Override
            public void onCancel() {
                levelToRadioButton.get(FREEDOM).setChecked(true);
                Toast.makeText(OptionsActivity.this, "You cancelled the purchase!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onLevelRadioButtonClicked(View view) {
        RadioButton rb = ((RadioButton) view);
        if (!rb.isChecked()) {
            return;
        }

        if (needsPurchasing(rb)) {
            try {
                requestPurchase(radioButtonToLevel.get(rb));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Problem trying to purchase level", e);
            }
        } else {
            Options.Level level = radioButtonToLevel.get(view);
            selectLevel(level);
        }
    }

    private void selectLevel(Options.Level level) {
        getInstance().setLevel(level);
        mOuyaFacade.putGameData(PREV_SELECTED_LEVEL_KEY, level.toString());
    }
}
