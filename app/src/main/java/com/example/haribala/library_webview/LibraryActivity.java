package com.example.haribala.library_webview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

/*
 * This goes in conjunction with haribala.pythonanywhere.com;
 * In "lending" section, there will be a button called "Scan". This will be available only in
 * mobile version. When "Scan" button is clicked, the Javascript code will call into
 * Android function. At this time, both android and Javascript try to upload  a book name into
 * the database. In its final version, when scan is clicked, the IntentIntegrator code needs to
 * be called to scan in the ISBN, identify the book and pass in the bookname; it can upload it directly
 * or let Javascript handle it.
 */

public class LibraryActivity extends ActionBarActivity {
    LibraryActivity myActivity;

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                uploadUrl(urls[0]);
                return "OK";
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //textView.setText(result);
            return;
        }

        // Given a URL, establishes an HttpUrlConnection and posts
        //

        private boolean uploadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            //int len = 500;

            try {
                String urlParameters  = "bookname0=FromAndroidOnly2";
                //byte[] postData       = urlParameters.getBytes(StandardCharsets.UTF_8 );
                byte[] postData = urlParameters.getBytes(Charset.forName("UTF-8"));
                int    postDataLength = postData.length;
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setDoInput(true);
                conn.setUseCaches(false);
                try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                    wr.write( postData );
                }

                // Starts the query
               // conn.connect();
                int response = conn.getResponseCode();
                Log.d("XXX", "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                //String contentAsString = readIt(is, len);
                //return contentAsString;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
                return true;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }


    private class MySendFile {

        public boolean postFile() {

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                // fetch data
                new DownloadWebpageTask().execute("http://haribala.pythonanywhere.com/lend_books");
                return true;

            } else {
                // display error
                return false;
            }
        }

    }
    private class LibraryWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //view.loadUrl(url);
            //all links must load in webview for now
            return false;
        }
    }
    public class WebAppInterface {
        Context mContext;
        //protected GoogleApiClient mGoogleApiClient;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        //Toast do not work on webApp. So this functions returns a string
        //back to Javascript and JS uses that string to change
        //some UI element. Clicking some button in Javascript would
        //result in calling of this function.
        @JavascriptInterface
        public String showToast(String toast) {
            Log.d("XXX", "I am going to run a scan");
/*
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Scopes.PLUS_LOGIN)
                    .addScope(Scopes.PLUS_ME)
                    .build();

            mGoogleApiClient.connect();
  */

/*
            IntentIntegrator integrator = new IntentIntegrator(myActivity);

            //we can pass some args limiting what kind of items do we expect to scan.
            //ISBN Barcodes are supposed to use EAN_13 formatted code.
            //returns Dialog box that was created, if dialogbox was needed
            Collection<String> desiredBarCode = Collections.singleton("EAN_13");
            AlertDialog dialog = integrator.initiateScan(desiredBarCode);
 */

            MySendFile upload = new MySendFile();
            upload.postFile();
            Log.d("XXX", "after initiating scan");

            return ("Android");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_library);

        WebView webview = new WebView(this);
        webview.setWebViewClient(new LibraryWebViewClient());
        this.setContentView(webview);
        webview.loadUrl("http://haribala.pythonanywhere.com");
        myActivity = this;
        //boolean back = webview.canGoBack();
        //webview.findAll("lend");

        //boolean progress = getWindow().hasFeature(Window.FEATURE_PROGRESS);

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        //The standard behavior for an Activity is to be destroyed and recreated when the device orientation or any other configuration changes.
        // This will cause the WebView to reload the current page. If you don't want that, you can set your Activity to handle the orientation and
        // keyboardHidden changes, and then just leave the WebView alone. It'll automatically re-orient itself as appropriate.
        // Read Handling Runtime Changes for more information about how to handle configuration changes during runtime.

        webview.addJavascriptInterface(new WebAppInterface(this), "Android");
        //webView.addJavascriptInterface
        //We will have to have a special button in the Javascript file which will be only enabled for mobile devices.
        //When this button is clicked, via JavascriptInterface, control will pass from javascript to AndroidActivity.
        //That activity would perform the scan of barcode ie ISBN reading to identify the book name and details.


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            // handle scan result
            Log.d("XXX", "got scan result");
            //scanResult.getContents() is expected to return the book description; or it could be the
            //barcode, in which case, we need to convert the bar code to Book Description.
            //In any case, the book description must be stored in some class variable.
            //The Javascript will poll back to this code, say every 10 seconds, to check
            //if the conversion happened and the book description was got.
            //The javascript code then will make HTTP request adding the book into the library
            // web code.
        } else {
            Log.d("XXX", "failed to get scan result");
        }
    }
}
