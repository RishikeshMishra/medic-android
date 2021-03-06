package org.medicmobile.webapp.mobile;

import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import static org.medicmobile.webapp.mobile.BuildConfig.DEBUG;

final class Utils {
	private Utils() {}

	static JSONObject json(Object... keyVals) throws JSONException {
		if(DEBUG && keyVals.length % 2 != 0) throw new AssertionError();
		JSONObject o = new JSONObject();
		for(int i=keyVals.length-1; i>0; i-=2) {
			o.put(keyVals[i-1].toString(), keyVals[i]);
		}
		return o;
	}

	static boolean intentHandlerAvailableFor(Context ctx, Intent intent) {
		return intent.resolveActivity(ctx.getPackageManager()) != null;
	}
}
