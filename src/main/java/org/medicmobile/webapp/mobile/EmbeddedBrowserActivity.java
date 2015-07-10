package org.medicmobile.webapp.mobile;

import android.app.*;
import android.content.*;
import android.util.Log;
import android.webkit.*;
import android.os.*;
import android.view.*;

import java.io.File;
import java.util.regex.*;

public class EmbeddedBrowserActivity extends Activity {
	private static final boolean DEBUG = BuildConfig.DEBUG;

	private SettingsStore settings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.settings = SettingsStore.in(this);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		WebView container = (WebView) findViewById(R.id.WebViewContainer);

		if(DEBUG) enableWebviewLogging(container);
		enableJavascript(container);
		enableStorage(container);
		handleAuth(container);

		String url = settings.getAppUrl() + "/_design/medic/_rewrite/";
		if(DEBUG) log("Pointing browser to %s", url);
		container.loadUrl(url);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.web_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.mnuSettings:
				openSettings();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings() {
		startActivity(new Intent(this,
				SettingsDialogActivity.class));
		finish();
	}

	private void handleAuth(WebView container) {
		final String url = settings.getAppUrl();
		if(DEBUG) log("Setting up Basic Auth credentials for %s...", url);

		final Matcher m = Settings.URL_PATTERN.matcher(url);
		if(!m.matches()) {
			throw new IllegalArgumentException("URL does not appear valid: " + url);
		}
		final String authHost = m.group(1);
		final String authPort = m.group(2);
		final String authRealm = "couch";

		final String username = settings.getUsername();
		final String password = settings.getPassword();

		if(DEBUG) log("username=%s, password=%s, host=%s, port=%s, realm=%s",
				username, password, authHost, authPort, authRealm);

		container.setHttpAuthUsernamePassword(authHost, authRealm,
				username, password);

		container.setWebViewClient(new WebViewClient() {
			public void onReceivedHttpAuthRequest(
					WebView view,
					HttpAuthHandler handler,
					String requestHost,
					String requestRealm) {
				if(DEBUG) log("requestHost = " + requestHost);
				if(DEBUG) log("requestRealm = " + requestRealm);
				if(!((requestHost.equals(authHost) || requestHost.equals(authHost + authPort) &&
						requestRealm.equals(authRealm)))) {
					log("Not providing credntials for %s|%s",
							requestHost, requestRealm);
					return;
				}
				if(DEBUG) log("Providing credentials %s:%s to %s|%s",
					username, password,
					requestHost, requestRealm);
				handler.proceed(username, password);
			}
		});
	}

	private void enableWebviewLogging(WebView container) {
		container.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("Medic Mobile", cm.message() + " -- From line "
						+ cm.lineNumber() + " of "
						+ cm.sourceId());
				return true;
			}
		});
	}

	private void enableJavascript(WebView container) {
		container.getSettings().setJavaScriptEnabled(true);
		container.addJavascriptInterface(
				new MedicAndroidJavascript(this.settings),
				"medicmobile_android"
		);
	}

	private void enableStorage(WebView container) {
		WebSettings webSettings = container.getSettings();
		webSettings.setDatabaseEnabled(true);
		webSettings.setDomStorageEnabled(true);
		File dir = getCacheDir();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		webSettings.setAppCachePath(dir.getPath());
		webSettings.setAppCacheEnabled(true);
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | EmbeddedBrowserActivity::" +
				String.format(message, extras));
	}
}
