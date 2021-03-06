/*
 *   Copyright 2011-2012 Shell M. Shrader
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.shellware.CarHome.helpers;

import com.shellware.CarHome.CarHomeActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class PokeBackReceiver extends BroadcastReceiver {
    private static final String TAG = "PokeBack";
    private static final String POKED_KEYPHRASE = "WHERE ARE YOU";
    private static final String SEE_KEYPHRASE = "WHAT DO YOU SEE";
    private static final String REBOOT_KEYPHRASE = "REBOOT NOW";
    
	@Override
	public void onReceive(Context ctx, Intent intent) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        
		if (!prefs.getBoolean("receiver_active", true)) {
        	Log.d(TAG, "Service Disabled");
			return;
		}
		
        Bundle bundle = intent.getExtras();        
        SmsMessage[] msgs = null;

        if (bundle != null) {   
        	final long lastMessageTimestamp = prefs.getLong("last_message_timestamp", 0);
        	
        	Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            

            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
            	final String msg = msgs[i].getMessageBody();

                if (msgs[i].getMessageBody() != null && msg.toUpperCase().contains(POKED_KEYPHRASE) &&
                 		msgs[i].getTimestampMillis() > lastMessageTimestamp) {

                	CarHomeActivity.whereAreYou(msgs[i].getOriginatingAddress());

	            	Editor edit = prefs.edit();
                	edit.putLong("last_message_timestamp", msgs[i].getTimestampMillis());
                	edit.commit();
                	
                	Log.d(TAG, "poked at: " + msgs[i].getTimestampMillis());
                	
                	abortBroadcast();
                } else {
                    if (msgs[i].getMessageBody() != null && msg.toUpperCase().contains(SEE_KEYPHRASE) &&
                     		msgs[i].getTimestampMillis() > lastMessageTimestamp) {

                    	CarHomeActivity.whatDoYouSee(msgs[i].getOriginatingAddress());

    	            	Editor edit = prefs.edit();
                    	edit.putLong("last_message_timestamp", msgs[i].getTimestampMillis());
                    	edit.commit();
                    	
                    	Log.d(TAG, "what do you see at: " + msgs[i].getTimestampMillis());
                    	
                    	abortBroadcast();
	                } else {
	                    if (msgs[i].getMessageBody() != null && msg.toUpperCase().contains(REBOOT_KEYPHRASE) &&
	                     		msgs[i].getTimestampMillis() > lastMessageTimestamp) {
		
	    	            	Editor edit = prefs.edit();
	                    	edit.putLong("last_message_timestamp", msgs[i].getTimestampMillis());
	                    	edit.commit();
	                    	
	                    	Log.d(TAG, "reboot now at: " + msgs[i].getTimestampMillis());
	                    	CarHomeActivity.rebootWithSU();                    	
	                    	abortBroadcast();
	                    }	
	                }
                }
            }
        }                         		
	}

}
