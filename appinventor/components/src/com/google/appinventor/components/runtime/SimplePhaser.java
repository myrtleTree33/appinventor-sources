package com.google.appinventor.components.runtime;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

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
  private Form form;

  WebViewInterface wvInterface;


  /**
   * Allows the setting of properties to be monitored from the javascript
   * in the WebView
   */
  public class WebViewInterface {
    Form webViewForm;

    /**
     * Instantiate the interface and set the context
     */
    WebViewInterface(Form webViewForm) {
      this.webViewForm = webViewForm;
    }
  }


  /**
   * Creates a new AndroidViewComponent.
   *
   * @param container container, component will be placed in
   */
  public SimplePhaser(ComponentContainer container) {
    super(container);

    this.form = container.$form();
    webview = new WebView(container.$context());
    webview.getSettings().setJavaScriptEnabled(true);
    webview.setFocusable(true);
// adds a way to send strings to the javascript
    wvInterface = new WebViewInterface(form);
    webview.addJavascriptInterface(wvInterface, "AppInventorMap");
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


//  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
//          defaultValue = "")
  @SimpleProperty
  public void LoadWebpage() {
    loadPage();
  }


  public void loadPage() {
    String markup = "<HTML>hello world</HTML>";
    webview.loadDataWithBaseURL(null, markup, "text/html", "utf-8", null);
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
}
