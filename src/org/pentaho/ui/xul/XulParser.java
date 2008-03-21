/**
 * 
 */
package org.pentaho.ui.xul;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.pentaho.ui.xul.containers.XulWindow;
import org.pentaho.ui.xul.dom.Document;
import org.pentaho.ui.xul.dom.DocumentFactory;
import org.pentaho.ui.xul.dom.Element;
import org.pentaho.ui.xul.util.XulUtil;

/**
 * @author nbaker
 *
 */
public class XulParser {
  Document xulDocument;

  static Map<String, Object> handlers = new HashMap<String, Object>();

  private XulDomContainer xulDomContainer;

  public XulParser() throws XulException {
    try {
      xulDocument = DocumentFactory.createDocument();
    } catch (Exception e) {
      throw new XulException("Error getting Document instance", e);
    }
  }

  public void setContainer(XulDomContainer xulDomContainer) {
    this.xulDomContainer = xulDomContainer;
    xulDomContainer.addDocument(xulDocument);
  }

  public Document parseDocument(org.dom4j.Element rootSrc) throws XulException {
    XulElement root = parse(rootSrc, null);

    //give root reference to runner for service calls
    if(root instanceof XulWindow){
      ((XulWindow)root).setXulDomContainer(this.xulDomContainer);
    }

    xulDocument.addChild((Element) root);
    return xulDocument;
  }

  public XulElement parse(org.dom4j.Element rootSrc, XulContainer parent) throws XulException {
    //parse element
    XulElement root = getElement(rootSrc, parent);

    //descend down a level and parse children (root would be a container in the case)
    for (Object child : rootSrc.elements()) {
      XulElement childElement = parse((org.dom4j.Element) child, (XulContainer) root);

      //TODO: remove once exception handling in place
      if (childElement == null) {
        continue;
      }

      // Add to the XML DOM tree ...
      root.addChild(childElement);

      // ... then add to the UI component tree.
      if (root instanceof XulContainer) //more of an assert, should be true.
        ((XulContainer) root).addComponent(childElement);
    }
    if (root != null) {
      root.layout();
    }

    return root;
  }

  protected XulElement getElement(org.dom4j.Element srcEle, XulContainer parent) throws XulException {

    Object handler = handlers.get(srcEle.getName().toUpperCase());

    if (handler == null) {
      System.out.println("handler not found: " + srcEle.getName().toUpperCase());
      return null;
      //throw new XulException(String.format("No handler available for input: %s", srcEle.getName()));
    }

    String tagName = srcEle.getName();
    Class<?> c;
    try {
      c = Class.forName((String) handler);
      Constructor<?> constructor = c
          .getConstructor(new Class[] { XulElement.class, XulDomContainer.class, String.class });
      XulElement ele = (XulElement) constructor.newInstance(parent, xulDomContainer, tagName);

      Map<String, String> attributesMap = XulUtil.AttributesToMap(srcEle.attributes());
      BeanUtils.populate(ele, attributesMap);
      return ele;
    } catch (Exception e) {
      throw new XulException(e);
    }

  }

  public void registerHandler(String type, String handler) {

    XulParser.handlers.put(type.toUpperCase(), handler);

  }

  public Document getDocumentRoot() {
    return this.xulDocument;
  }

}
