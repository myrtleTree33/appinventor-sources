package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.simple.components.MockVisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Created by joel on 3/12/15.
 */
public class MockSimplePhaser extends MockVisibleComponent {

  public static final String TYPE = "SimplePhaser";

  private final Image largeImage = new Image(images.webviewerbig());

  /**
   * Creates a new instance of a visible component.
   *
   * @param editor editor of source file the component belongs to
   */
  public MockSimplePhaser(SimpleEditor editor) {
    super(editor, TYPE, images.simplePhasor());
    // Initialize mock WebViewer UI
    SimplePanel webViewerWidget = new SimplePanel();
    webViewerWidget.setStylePrimaryName("ode-SimpleMockContainer");

    webViewerWidget.addStyleDependentName("centerContents");
    webViewerWidget.setWidget(largeImage);
    initComponent(webViewerWidget);
  }

  // If these are not here, then we don't see the icon as it's
  // being dragged from the pelette
  @Override
  public int getPreferredWidth() {
    return largeImage.getWidth();
  }

  @Override
  public int getPreferredHeight() {
    return largeImage.getHeight();
  }

  // override the width and height hints, so that automatic will in fact be fill-parent
  @Override
  int getWidthHint() {
    int widthHint = super.getWidthHint();
    if (widthHint == LENGTH_PREFERRED) {
      widthHint = LENGTH_FILL_PARENT;
    }
    return widthHint;
  }

  @Override int getHeightHint() {
    int heightHint = super.getHeightHint();
    if (heightHint == LENGTH_PREFERRED) {
      heightHint = LENGTH_FILL_PARENT;
    }
    return heightHint;
  }

//  // override the width and height hints, so that automatic will in fact be fill-parent
//  @Override
//  int getWidthHint() {
//    int widthHint = super.getPreferredWidth();
//    if (widthHint == LENGTH_PREFERRED) {
//      widthHint = LENGTH_FILL_PARENT;
//    }
//    return widthHint;
//  }
//
//  @Override
//  int getHeightHint() {
//    int heightHint = super.getPreferredHeight();
//    if (heightHint == LENGTH_PREFERRED) {
//      heightHint = LENGTH_FILL_PARENT;
//    }
//    return heightHint;
//  }

}
