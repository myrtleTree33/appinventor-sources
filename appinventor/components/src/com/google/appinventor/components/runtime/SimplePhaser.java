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

import java.util.AbstractMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

  private AbstractMap<String, String> keyStore;

  WebViewInterface wvInterface;


  /**
   *
   * Allows the setting of properties to be monitored from the javascript
   * in the WebView
   */
  public class WebViewInterface {
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

    @JavascriptInterface
    public void onCollision(String aName, String aGroup, String bName, String bGroup) {
      EventSpriteCollide(aName, aGroup, bName, bGroup);
    }

    @JavascriptInterface
    public void sendMessage(String uuid, String payload) {
      keyStore.put(uuid, payload);
    }
  }


  /**
   * Creates a new AndroidViewComponent.
   *
   * @param container container, component will be placed in
   */
  public SimplePhaser(ComponentContainer container) {
    super(container);

    keyStore = new ConcurrentHashMap<String, String>();

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

    // load the webpage automatically
    loadPage();
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


  private String makeUuid() {
    return UUID.randomUUID().toString();
  }


  private String getMessage(String uuid) {
    Toast.makeText(webview.getContext(), "Mailbox: " + keyStore.size(), Toast.LENGTH_SHORT).show();
    while (!keyStore.containsKey(uuid)) {
      // empty pend
    }
    Toast.makeText(webview.getContext(), "GOT WIDTH DATA", Toast.LENGTH_SHORT).show();
    return keyStore.remove(uuid);
  }


  /**
   *
   * To add a property,remember to add to TranslationComponentProperty and OdeMessages classes.
   * for it to be visible.
   *
   */

  @SimpleProperty
  public int GameWidth() {
    String uuid = makeUuid();
    webview.loadUrl("javascript:api.GetGameWidth(" + dumpStr(uuid) + ");");
    int width = Integer.parseInt(getMessage(uuid));
    return width;
  }


  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "")
  @SimpleProperty
  public void GameWidth(int width) {
    // do some setting info here
  }

//  @SimpleFunction(
//          description = "Loads the Google page."
//  )
//  public void LoadWebpage() {
////    loadPage();
//  }
//

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

  private String dumpStr(String str) {
    return "\'" + str + "\'";
  }

  @SimpleEvent
  public void EventGameReady() {
    Toast.makeText(webview.getContext(), "Dispatched event!!", Toast.LENGTH_SHORT).show();
    EventDispatcher.dispatchEvent(this, "EventGameReady");
  }

  @SimpleEvent
  public void EventSpriteCollide(String aName, String aGroup,
                                 String bName, String bGroup) {
    Toast.makeText(webview.getContext(), "collision!", Toast.LENGTH_SHORT).show();
    EventDispatcher.dispatchEvent(this, "EventSpriteCollide",
            aName, aGroup, bName, bGroup);
  }


  /**********************************************/
  /*************** Instantiators ****************/
  /**
   * ******************************************
   */

  @SimpleFunction(
          description = "Creates a background of the sky."
  )
  public void CreateSky() {
    webview.loadUrl("javascript:api.CreateSky()");
  }


  @SimpleFunction(
          description = "Creates a simple platform."
  )
  public void CreatePlatform(String group, int x, int y) {
    webview.loadUrl("javascript:api.CreatePlatform(" + dumpStr(group) + "," + x + "," + y + ")");
  }


  @SimpleFunction(
          description = "Creates a rock object."
  )
  public void CreateRock(String group, int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreateRock(" +
            dumpStr(group) + "," +
            x + "," +
            y + "," +
            gravity + ")");
  }


  @SimpleFunction(
          description = "Creates a tree object."
  )
  public void CreateTree(String group, int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreateTree(" +
            dumpStr(group) + "," +
            x + "," +
            y + "," +
            gravity + ")");
  }


  @SimpleFunction(
          description = "Creates a new flying bullet that destroys on impact."
  )
  public void CreateBullet(int x, int y, int gravity,
                           int xVel, int yVel) {
    webview.loadUrl("javascript:api.CreateBullet(" +
            x + "," +
            y + "," +
            gravity + "," +
            xVel + "," +
            yVel + ")");
  }


  @SimpleFunction(
          description = "Deletes a sprite object."
  )
  public void DeleteSprite(String name) {
    webview.loadUrl("javascript:api.CreateTree(" +
            dumpStr(name) + ")");
  }


  @SimpleFunction(
          description = "Sets position of a sprite object."
  )
  public void SetPosition(String name, int x, int y) {
    webview.loadUrl("javascript:api.SetPosition(" +
            dumpStr(name) + "," +
            x + "," +
            y + ")");
  }


  @SimpleFunction(
          description = "Generates the game."
  )
  public void GenerateGame() {
    webview.loadUrl("javascript:api.GenerateGame()");
  }

}
