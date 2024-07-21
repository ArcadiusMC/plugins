package net.arcadiusmc.ui.resource;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.struct.Attr;
import net.arcadiusmc.ui.struct.BodyElement;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.struct.Element;
import net.arcadiusmc.ui.struct.Elements;
import net.arcadiusmc.ui.struct.ItemElement;
import net.arcadiusmc.ui.struct.Node;
import net.arcadiusmc.ui.struct.TextNode;
import net.arcadiusmc.ui.style.DocumentStyles;
import net.arcadiusmc.ui.style.Stylesheet;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class PageXmlLoader {

  static final String ROOT_ELEMENT = "page";
  static final String HEADER_ELEMENT = "header";
  static final String BODY_ELEMENT = Elements.BODY;
  static final String OPTION_ELEMENT = "option";
  static final String SCREEN_ELEMENT = "screen";
  static final String STYLE_ELEMENT = "style";

  static final String ATTR_WIDTH = "width";
  static final String ATTR_HEIGHT = "height";

  private static final Logger LOGGER = Loggers.getLogger();

  private final BufferedReader reader;
  private final Document view;
  private final PageResources resources;

  private final String uri;

  public PageXmlLoader(String uri, BufferedReader reader, Document view, PageResources resources) {
    this.uri = uri;
    this.reader = reader;
    this.view = view;
    this.resources = resources;
  }

  public Optional<ParsedDocument> parse()
      throws ParserConfigurationException, SAXException, IOException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    SAXParser parser = factory.newSAXParser();

    InputSource source = new InputSource(reader);
    source.setPublicId(uri);

    LoadingHandler handler = new LoadingHandler(view, resources);
    parser.parse(source, handler);

    if (handler.failed) {
      return Optional.empty();
    }

    DocumentStyles styles = view.getStyles();
    handler.sheets.forEach(styles::addStylesheet);

    view.getOptions().putAll(handler.options);

    ParsedDocument parsedDoc = new ParsedDocument(
        (BodyElement) handler.root,
        handler.width,
        handler.height,
        handler.options,
        handler.sheets
    );

    return Optional.ofNullable(parsedDoc);
  }

  class LoadingHandler extends DefaultHandler {

    private final Document doc;
    private final PageResources resources;

    private boolean failed = false;

    private final Stack<Node> nodes = new Stack<>();
    private final Stack<LoadMode> mode = new Stack<>();
    private final Map<String, String> options = new HashMap<>();
    private final List<Stylesheet> sheets = new ArrayList<>();

    private int depth = 0;
    private Integer ignoreDepth = null;
    private String ignoreElement = null;
    private boolean ignoreWarningLogged = false;

    private float width = 0.0f;
    private float height = 0.0f;

    private Node root;

    private Locator locator;

    public LoadingHandler(Document doc, PageResources resources) {
      this.doc = doc;
      this.resources = resources;
    }

    LoadMode mode() {
      return mode.isEmpty() ? LoadMode.NONE : mode.peek();
    }

    void beginIgnoringChildren(String tagName) {
      this.ignoreElement = tagName;
      this.ignoreDepth = depth;
      this.ignoreWarningLogged = false;
    }

    void stopIgnoringChildren() {
      this.ignoreElement = null;
      this.ignoreDepth = null;
    }

    void warnChildrenIgnored() {
      if (Strings.isNullOrEmpty(ignoreElement) || ignoreWarningLogged) {
        return;
      }

      warn("<%s/> elements cannot have children... ignoring", ignoreElement);
      ignoreWarningLogged = true;
    }

    void pushNode(Node n) {
      if (root == null) {
        root = n;
      }

      if (!nodes.isEmpty()) {
        Node p = nodes.peek();
        p.addChild(n);
      }

      nodes.push(n);
    }

    void popNode() {
      nodes.pop();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
      this.locator = locator;
    }

    @Override
    public void startDocument() throws SAXException {
      depth++;
      //mode.push(LoadMode.DOCUMENT);
    }

    @Override
    public void endDocument() throws SAXException {
      depth--;
      //mode.pop();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
      depth++;

      if (ignoreDepth != null) {
        warnChildrenIgnored();
        return;
      }

      LoadMode m = mode();

      switch (m) {
        case DOCUMENT:
        case NONE:
          switch (qName) {
            case ROOT_ELEMENT:
              mode.push(LoadMode.DOCUMENT);
              return;

            case HEADER_ELEMENT:
              mode.push(LoadMode.HEADER);
              return;

            default:
              return;

            case BODY_ELEMENT:
              // Fall through to BODY case
          }

        case BODY:
          Element e = doc.createElement(qName);
          pushNode(e);
          mode.push(LoadMode.BODY);

          for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            String value = attributes.getValue(i);
            e.setAttribute(name, value);
          }

          if (e instanceof ItemElement item) {
            String itemSrc = attributes.getValue(Attr.SOURCE);
            beginIgnoringChildren(item.getTagName());

            if (!Strings.isNullOrEmpty(itemSrc)) {
              resources.loadItemStack(itemSrc).ifPresent(item::setItem);
            }
          }

          break;

        case HEADER:
          mode.push(LoadMode.HEADER);
          headerElement(qName, attributes);
          break;

        default:
          break;
      }
    }

    private String validateAttribute(String elementName, String attrib, Attributes attributes) {
      String value = attributes.getValue(attrib);
      return validateAttribute(elementName, attrib, value);
    }

    private String validateAttribute(String elementName, String attrib, String value) {
      if (!Strings.isNullOrEmpty(value)) {
        return value;
      }

      warn("Missing '%s' attribute on %s", attrib, elementName);
      return null;
    }

    private void headerElement(String name, Attributes attributes) throws SAXException {
      switch (name) {
        case OPTION_ELEMENT -> {
          beginIgnoringChildren(OPTION_ELEMENT);

          String key = validateAttribute(name, Attr.KEY, attributes);
          String value = validateAttribute(name, Attr.VALUE, attributes);

          if (Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(value)) {
            return;
          }

          options.put(key, value);
        }

        case STYLE_ELEMENT -> {
          beginIgnoringChildren(STYLE_ELEMENT);

          String src = validateAttribute(name, Attr.SOURCE, attributes);

          if (Strings.isNullOrEmpty(src)) {
            return;
          }

          resources.loadStylesheet(src).ifPresent(sheets::add);
        }

        case SCREEN_ELEMENT -> {
          beginIgnoringChildren(SCREEN_ELEMENT);

          String widthStr = validateAttribute(name, ATTR_WIDTH, attributes);
          String heightStr = validateAttribute(name, ATTR_HEIGHT, attributes);

          if (Strings.isNullOrEmpty(widthStr) || Strings.isNullOrEmpty(heightStr)) {
            return;
          }

          parseScreenDimension(widthStr, ATTR_WIDTH, t -> this.width = t);
          parseScreenDimension(heightStr, ATTR_HEIGHT, t -> this.height = t);
        }

        default -> {
          // :shrug: idk, it's not a valid header element, so it doesn't really matter
          // but should it be logged? I don't care
        }
      }
    }

    private void parseScreenDimension(String str, String dim, FloatConsumer consumer) {
      float f;

      try {
        f = Float.parseFloat(str);
      } catch (NumberFormatException exc) {
        error("Failed to convert '%s' to number for screen %s", str, dim);
        return;
      }

      if (f < 1) {
        error("Screen %s cannot be less than 1, value: %s", dim, f);
        return;
      }

      consumer.accept(f);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      int d = depth--;

      if (ignoreDepth != null) {
        if (d > ignoreDepth) {
          return;
        }

        stopIgnoringChildren();
      }

      LoadMode first = mode();
      mode.pop();
      LoadMode prev = mode();

      if (prev == LoadMode.BODY || first == LoadMode.BODY) {
        popNode();
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (ignoreDepth != null) {
        warnChildrenIgnored();
        return;
      }

      if (mode() != LoadMode.BODY) {
        return;
      }

      String s = String.valueOf(ch, start, length).trim();

      if (Strings.isNullOrEmpty(s) || s.isBlank()) {
        return;
      }

      TextNode textNode = doc.createText();
      textNode.setTextContent(s);

      pushNode(textNode);
      popNode();
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      saxException(Level.WARN, e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      saxException(Level.ERROR, e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      saxException(Level.ERROR, e);
    }

    private void warn(String message, Object... args) {
      log(Level.WARN, message, args);
    }

    private void error(String message, Object... args) {
      log(Level.ERROR, message, args);
    }

    private void log(Level level, String message, Object... args) {
      saxException(level, new SAXParseException(String.format(message, args), locator));
    }

    private void saxException(Level level, SAXParseException exc) {
      if (level == Level.ERROR) {
        failed = true;
      }

      LOGGER.atLevel(level)
          .setMessage("XML loading error at {}#{}:{}: {}")
          .addArgument(exc.getPublicId())
          .addArgument(exc.getLineNumber())
          .addArgument(exc.getColumnNumber())
          .addArgument(exc.getMessage())
          .log();
    }
  }

  private enum LoadMode {
    NONE,
    DOCUMENT,
    HEADER,
    BODY;
  }
}
