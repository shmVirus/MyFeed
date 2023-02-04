package shmVirus.MyFeed;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ActivityWebView extends AppCompatActivity {
    WebView webView;
    String uri, title;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        Bundle Extra = getIntent().getExtras();
        uri = Extra.getString("uri");
        title = Extra.getString("title");
        setTitle(title);

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());      // setting WebView
        webView.getSettings().setJavaScriptEnabled(true);   // enabling JavaScript for WebView
        webView.loadUrl(uri);                               // loading URL in WebView
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web_view, menu);  // setting menu options for WebView
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {  // listening click on menus
        int itemID = item.getItemId();      // getting id of clicked menu

        // taking appropriate actions for different menus
        if (itemID == R.id.mBack) {
            finish();   // closing this WebView activity, and this allows to go back to previous activity
        } else if (itemID == R.id.mClose) {
            finishAffinity();   // closing all activities, in short closing application
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {  // preventing closing WebView on swipe back or on click on back button of navigation bar
            webView.goBack();       // going back to previous URIs, when user click more URIs after launching WebView
        } else {
            super.onBackPressed();  // when there's no previous URI, moving to previous activity
        }
    }
}