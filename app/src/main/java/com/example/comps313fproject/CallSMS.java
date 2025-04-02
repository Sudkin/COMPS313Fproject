
package com.example.comps313fproject;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class CallSMS {

    private final Context context;

    public CallSMS(Context context) {
        this.context = context;
    }

    public void sendSMS(String phoneNumber, String message) {

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Fail" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }


    }
}
