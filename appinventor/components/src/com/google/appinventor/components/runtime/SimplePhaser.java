package com.google.appinventor.components.runtime;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.FroyoUtil;
import com.google.appinventor.components.runtime.util.SdkLevel;

/**
 * Created by joel on 3/12/15.
 */
@DesignerComponent(version = YaVersion.SIMPLEPHASOR_COMPONENT_VERSION,
        category = ComponentCategory.USERINTERFACE,
        description = "Creates a Simple Phaser game.")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
public class SimplePhaser extends AndroidViewComponent {

  private final WebView webview;

  WebViewInterface wvInterface;


  /**
   * Allows the setting of properties to be monitored from the javascript
   * in the WebView
   */
  public class WebViewInterface {
    Form webViewForm;
    Context mContext;
    String webViewString;

    /**
     * Instantiate the interface and set the context
     */
    WebViewInterface(Context c) {
      mContext = c;
      webViewString = " ";
    }

    @JavascriptInterface
    public void setGameLoadedFlag() {
      EventGameReady();
    }
  }


  /**
   * Creates a new AndroidViewComponent.
   *
   * @param container container, component will be placed in
   */
  public SimplePhaser(ComponentContainer container) {
    super(container);

    webview = new WebView(container.$context());
    resetWebViewClient();       // Set up the web view client
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setFocusable(true);
// adds a way to send strings to the javascript
    wvInterface = new WebViewInterface(webview.getContext());
    webview.addJavascriptInterface(wvInterface, "Android");
//We had some issues with rendering of maps on certain devices; using caching seems to solve it
    webview.setDrawingCacheEnabled(false);
    webview.setDrawingCacheEnabled(true);
// Support for console APIs -- only available in API level 8+ (here only for debugging).
//TODO (jos) will this crash in lower level phones?
    webview.setWebChromeClient(new WebChromeClient() {
      public boolean onConsoleMessage(ConsoleMessage cm) {
        Log.d("WEBMAP", cm.message() + " -- From line "
                + cm.lineNumber() + " of "
                + cm.sourceId());
        return true;
      }
    });
    container.$add(this);
    webview.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
          case MotionEvent.ACTION_UP:
            if (!v.hasFocus()) {
              v.requestFocus();
            }
            break;
        }
        return false;
      }
    });

// set the initial default properties. Height and Width
// will be fill-parent, which will be the default for the web viewer.
    Width(LENGTH_FILL_PARENT);
    Height(LENGTH_FILL_PARENT);

  }

  // Create a class so we can override the default link following behavior.
  // The handler doesn't do anything on its own.  But returning true means that
  // this do nothing will override the default WebVew behavior.  Returning
  // false means to let the WebView handle the Url.  In other words, returning
  // true will not follow the link, and returning false will follow the link.
  private class WebViewerClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      boolean followLinks = true;
      return !followLinks;
    }
  }

  private void resetWebViewClient() {
    if (SdkLevel.getLevel() >= SdkLevel.LEVEL_FROYO) {
      boolean ignoreSslErrors = false;
      boolean followLinks = true;
      webview.setWebViewClient(FroyoUtil.getWebViewClient(ignoreSslErrors, followLinks, container.$form(), this));
    } else {
      webview.setWebViewClient(new WebViewerClient());
    }
  }


  //  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//          defaultValue = "")
  @SimpleProperty
  public void GetSomeProperty(String doNothing) {
    //Do nothing with doNothing :)
//    loadPage();
  }

  @SimpleFunction(
          description = "Loads the Google page."
  )
  public void LoadWebpage() {
    loadPage();
  }


  public void loadPage() {
//    String markup = "<HTML>hello world!! Hihi there!!</HTML>";
//    webview.loadDataWithBaseURL(null, markup, "text/html", "utf-8", null);
    webview.loadUrl("http://joeltong.org/phaser/");
  }

  @Override
  public View getView() {
    return webview;
  }

  // Components don't normally override Width and Height, but we do it here so that
  // the automatic width and height will be fill parent.
  @Override
  @SimpleProperty()
  public void Width(int width) {
    if (width == LENGTH_PREFERRED) {
      width = LENGTH_FILL_PARENT;
    }
    super.Width(width);
  }

  @Override
  @SimpleProperty()
  public void Height(int height) {
    if (height == LENGTH_PREFERRED) {
      height = LENGTH_FILL_PARENT;
    }
    super.Height(height);
  }

  /*************************************************************/
  /*************** JAVASCRIPT ENDPOINTS ************************/
  /**
   * *********************************************************
   */

  @SimpleEvent
  public void EventGameReady() {
//    Toast.makeText(webview.getContext(), "Dispatched event!!", Toast.LENGTH_SHORT).show();
    EventDispatcher.dispatchEvent(this, "EventGameReady");
  }

  @SimpleFunction(
          description = "Creates a background of the sky."
  )
  public void CreateSky() {
    webview.loadUrl("javascript:api.CreateSky()");
  }


  @SimpleFunction(
          description = "Creates a simple platform."
  )
  public void CreatePlatform(int x, int y) {
    webview.loadUrl("javascript:api.CreatePlatform(" + x + "," + y + ")");
  }


  @SimpleFunction(
          description = "Creates a rock object."
  )
  public void CreateRock(int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreateRock(" +
            x + "," +
            y + "," +
            gravity + ")");
  }


  @SimpleFunction(
          description = "Generates the game."
  )
  public void GenerateGame() {
    webview.loadUrl("javascript:api.GenerateGame()");
  }

}
