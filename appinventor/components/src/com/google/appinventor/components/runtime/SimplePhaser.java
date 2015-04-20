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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;

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

  private Map<String, String> keyStore;

  private Map<String, SpriteState> statesDb;  // accumulator to store an array of sprite states

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
//      Toast.makeText(webview.getContext(), "MSG PLACED==" + payload + " uuid=" + uuid, Toast.LENGTH_SHORT).show();
      keyStore.put(uuid, payload);
    }

    @JavascriptInterface
    public void makeSpriteStates(String payload) {
      JSONArray jsonArr;
      try {
        jsonArr = new JSONArray(payload);
        double x,y,velX,velY;
        String name;

        statesDb.clear();  // only clear after JSON is successfully parsed

        for (int i = 0; i < jsonArr.length(); i++) {
          JSONObject json = jsonArr.getJSONObject(i);
          name = json.getString("name");
          x = json.getDouble("x");
          y = json.getDouble("y");
          velX = json.getDouble("velX");
          velY = json.getDouble("velY");
          statesDb.put(name, new SpriteState(x,y,velX,velY));
        }

      } catch (JSONException e) {
      }
    }


    @JavascriptInterface
    public void onTouch(String event, int x, int y) {
      if (event.equals("tap")) {
        EventFingerTap(x,y);

      } else if (event.equals("down")) {
        EventFingerDown(x,y);

      } else if (event.equals("up")) {
        EventFingerUp(x, y);

      } else if (event.equals("dragged")) {
        EventFingerDrag(x, y);
      }
    }


    @JavascriptInterface
    public void onSwipe(String direction) {
      if (direction.equals("up")) {
        EventSwipeUp();

      } else if (direction.equals("down")) {
        EventSwipeDown();

      } else if (direction.equals("left")) {
        EventSwipeLeft();

      } else if (direction.equals("right")) {
        EventSwipeRight();

      } else if (direction.equals("swipe")) {
        EventSwipe();

      }
    }
  }


  /**
   * Creates a new AndroidViewComponent.
   *
   * @param container container, component will be placed in
   */
  public SimplePhaser(ComponentContainer container) {
    super(container);

    // sprite states
    statesDb = new ConcurrentHashMap<String, SpriteState>();

//    keyStore = new ConcurrentHashMap<String, String>();
    keyStore = Collections.synchronizedMap(new HashMap<String, String>());

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
//    Toast.makeText(webview.getContext(), "Mailbox: " + keyStore.size(), Toast.LENGTH_SHORT).show();
    while (!keyStore.containsKey(uuid)) {
      // empty pend
    }
//    Toast.makeText(webview.getContext(), "GOT WIDTH DATA", Toast.LENGTH_SHORT).show();
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
    // not used
  }


  @SimpleProperty
  public int GameHeight() {
    String uuid = makeUuid();
    webview.loadUrl("javascript:api.GetGameHeight(" + dumpStr(uuid) + ");");
    int height = Integer.parseInt(getMessage(uuid));
    return height;
  }


  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "")
  @SimpleProperty
  public void GameHeight(int height) {
    // not used
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

  /**
   * Simple class that stores an array of states
   */
  public class SpriteState {
    public double x;
    public double y;
    public double velX;
    public double velY;

    public SpriteState(double x, double y, double velX, double velY) {
      this.x = x;
      this.y = y;
      this.velX = velX;
      this.velY = velY;
    }
  }

  private String dumpStr(String str) {
    return "\'" + str + "\'";
  }

  @SimpleEvent(
          description = "Triggered when game starts."
  )
  public void EventGameReady() {
    generateGame();
    EventDispatcher.dispatchEvent(this, "EventGameReady");
  }

  @SimpleEvent(
          description = "Triggered when sprites collide."
  )
  public void EventSpriteCollide(String aName, String aGroup,
                                 String bName, String bGroup) {
//    Toast.makeText(webview.getContext(), "collision!", Toast.LENGTH_SHORT).show();
    EventDispatcher.dispatchEvent(this, "EventSpriteCollide",
            aName, aGroup, bName, bGroup);
  }


  @SimpleEvent(
          description = "Triggered on finger tap."
  )
  public void EventFingerTap(int x, int y) {
    EventDispatcher.dispatchEvent(this, "EventFingerTap", x, y);
  }


  @SimpleEvent(
          description = "Triggered on finger down."
  )
  public void EventFingerDown(int x, int y) {
    EventDispatcher.dispatchEvent(this, "EventFingerDown", x, y);
  }


  @SimpleEvent(
          description = "Triggered on finger up."
  )
  public void EventFingerUp(int x, int y) {
    EventDispatcher.dispatchEvent(this, "EventFingerUp", x, y);
  }


  @SimpleEvent(
          description = "Triggered on finger drag."
  )
  public void EventFingerDrag(int x, int y) {
    EventDispatcher.dispatchEvent(this, "EventFingerDrag", x, y);
  }


  @SimpleEvent(
          description = "Triggered on Swipe up."
  )
  public void EventSwipeUp() {
    EventDispatcher.dispatchEvent(this, "EventSwipeUp");
  }


  @SimpleEvent(
          description = "Triggered on swipe down."
  )
  public void EventSwipeDown() {
    EventDispatcher.dispatchEvent(this, "EventSwipeDown");
  }


  @SimpleEvent(
          description = "Triggered on swipe left."
  )
  public void EventSwipeLeft() {
    EventDispatcher.dispatchEvent(this, "EventSwipeLeft");
  }


  @SimpleEvent(
          description = "Triggered on swipe right."
  )
  public void EventSwipeRight() {
    EventDispatcher.dispatchEvent(this, "EventSwipeRight");
  }

  @SimpleEvent(
          description = "Triggered on swipe."
  )
  public void EventSwipe() {
    EventDispatcher.dispatchEvent(this, "EventSwipe");
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
          description = "Creates a tiled background.  Type= rock|sand"
  )
  public void CreateTiledBackground(String type, int width, int height) {
    webview.loadUrl("javascript:api.CreateTiledBackground("
            + dumpStr(type) + ","
            + width + ","
            + height + ")");
  }


  @SimpleFunction(
          description = "Creates a simple platform."
  )
  public void CreatePlatform(String group, int x, int y) {
    webview.loadUrl("javascript:api.CreatePlatform(" + dumpStr(group) + "," + x + "," + y + ")");
  }


  @SimpleFunction(
          description = "Creates a simple platform."
  )
  public void CreateTilePlatform(String group, int x, int y, int width, int height) {
    webview.loadUrl("javascript:api.CreateTilePlatform(" + dumpStr(group) + "," + x + "," + y + "," + width + "," + height + ")");
  }


  @SimpleFunction(
          description = "Creates a rock object."
  )
  public void CreateRock(String group, String name, int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreateRock(" +
            dumpStr(group) + "," +
            dumpStr(name) + "," +
            x + "," +
            y + "," +
            gravity + ")");
  }


  @SimpleFunction(
          description = "Creates a tree object."
  )
  public void CreateTree(String group, String name, int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreateTree(" +
            dumpStr(group) + "," +
            dumpStr(name) + "," +
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
          description = "Creates a new player sprite."
  )
  public void CreatePlayer(String group, String name, int x, int y, int gravity) {
    webview.loadUrl("javascript:api.CreatePlayer(" +
            dumpStr(group) + "," +
            dumpStr(name) + "," +
            x + "," +
            y + "," +
            gravity + "," +
            ")");
  }

  @SimpleFunction(
          description = "Deletes a sprite object."
  )
  public void DeleteSprite(String name) {
    webview.loadUrl("javascript:api.DeleteSprite(" +
            dumpStr(name) + ")");
  }


  @SimpleFunction(
          description = "Sets the game world size."
  )
  public void SetGameSize(int width, int height) {
    webview.loadUrl("javascript:api.SetGameSize("
            + width + ","
            + height + ")");
  }


  @SimpleFunction(
          description = "Sets the camera position."
  )
  public void SetCameraPos(int x, int y) {
    webview.loadUrl("javascript:api.SetCameraPos("
            + x + ","
            + y + ")");
  }


  @SimpleFunction(
          description = "Set the camera to follow a sprite."
  )
  public void SetCameraFollow(String name) {
    webview.loadUrl("javascript:api.SetCameraFollow("
            + name + ")");
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


  private SpriteState getSpriteState(String name) {
    return (SpriteState) statesDb.get(name);
  }


  @SimpleFunction(
          description = "Gets the X coordinate of a sprite object."
  )
  public double GetSpriteX(String name) {
    SpriteState state = getSpriteState(name);
    if (state == null) {
      return 0; // invalid
    }
    return state.x;
  }


  @SimpleFunction(
          description = "Gets the Y coordinate of a sprite object."
  )
  public double GetSpriteY(String name) {
    SpriteState state = getSpriteState(name);
    if (state == null) {
      return 0; // invalid
    }
    return state.y;
  }


  @SimpleFunction(
          description = "Gets the X velocity of a sprite object."
  )
  public double GetSpriteVelX(String name) {
    SpriteState state = getSpriteState(name);
    if (state == null) {
      return 0; // invalid
    }
    return state.velX;
  }


  @SimpleFunction(
          description = "Gets the Y velocity of a sprite object."
  )
  public double GetSpriteVelY(String name) {
    SpriteState state = getSpriteState(name);
    if (state == null) {
      return 0; // invalid
    }
    return state.velY;
  }





  private void pause(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  @SimpleFunction(
          description = "Sets the X coordinate of a sprite object."
  )
  public void SetSpriteX(String name, int x) {
    webview.loadUrl("javascript:api.SetSpriteX(" + dumpStr(name) + "," + x + ");");
  }


  @SimpleFunction(
          description = "Sets the Y coordinate of a sprite object."
  )
  public void SetSpriteY(String name, int y) {
    webview.loadUrl("javascript:api.SetSpriteY(" + dumpStr(name) + "," + y + ");");
  }


  @SimpleFunction(
          description = "Sets the X velocity of a sprite object."
  )
  public void SetSpriteVelX(String name, int velX) {
    webview.loadUrl("javascript:api.SetSpriteVelX(" + dumpStr(name) + "," + velX + ");");
  }


  @SimpleFunction(
          description = "Sets the Y velocity of a sprite object."
  )
  public void SetSpriteVelY(String name, int velY) {
    webview.loadUrl("javascript:api.SetSpriteVelY(" + dumpStr(name) + "," + velY + ");");
  }


  @SimpleFunction(
          description = "Sets the state of a player object."
  )
  public void SetState(String name, String state) {
    webview.loadUrl("javascript:api.SetState("
            + dumpStr(name) + ","
            + dumpStr(state) + ");");
  }


//  @SimpleFunction(
//          description = "Generates the game."
//  )
  private void generateGame() {
    webview.loadUrl("javascript:api.GenerateGame()");
  }

}
