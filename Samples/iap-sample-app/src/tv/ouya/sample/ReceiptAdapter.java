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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import tv.ouya.console.api.Receipt;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ReceiptAdapter extends ArrayAdapter<Receipt> {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final LayoutInflater inflater;

    public ReceiptAdapter(Context context, Receipt[] receipts) {
        super(context, R.layout.receipt_item, R.id.productId, receipts);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            view = inflater.inflate(R.layout.receipt_item, null);
        }

        Receipt receipt = getItem(position);
        ((TextView) view.findViewById(R.id.date)).setText(formatter.format(receipt.getPurchaseDate()));
        ((TextView) view.findViewById(R.id.productId)).setText(receipt.getIdentifier());
        ((TextView) view.findViewById(R.id.price)).setText(receipt.getFormattedPrice());
        return view;
    }

    @Deprecated // for testing only
    public void setTimezone(TimeZone timeZone) {
        formatter.setTimeZone(timeZone);
    }
}
