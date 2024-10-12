package net.arcadiusmc.bank.menus;

import com.google.common.base.Strings;
import java.util.Arrays;
import net.arcadiusmc.bank.BankPlugin;
import net.arcadiusmc.bank.BankRun;
import net.arcadiusmc.dom.Document;
import net.arcadiusmc.dom.Element;
import net.arcadiusmc.dom.TextNode;
import net.arcadiusmc.dom.event.EventListener;
import net.arcadiusmc.dom.event.EventTypes;
import net.arcadiusmc.dom.event.MouseEvent;

public class NumpadPage {

  static final char EMPTY = '_';

  private final Document document;
  private final BankRun run;
  private final char[] code;

  private final Element codeOutput;
  private final TextNode codeOutputText;

  public NumpadPage(Document document, BankRun run) {
    this.document = document;
    this.run = run;
    this.code = new char[run.getInnerVaultCode().length];

    this.codeOutput = document.getElementById("code-output");
    if (codeOutput != null) {
      codeOutputText = (TextNode) codeOutput.firstChild();
    } else {
      codeOutputText = null;
    }

    clearCode();
    updateCodeElement();

    document.getGlobalTarget().addEventListener(EventTypes.CLICK, new NumpadPressListener());
  }

  public static void onDomInitialize(Document document) {
    document.addEventListener(EventTypes.DOM_LOADED, event -> onDomLoaded(document));
  }

  private static void onDomLoaded(Document document) {
    var view = document.getView();
    var vaultKey = view.getPath().getQuery("vault");

    if (Strings.isNullOrEmpty(vaultKey)) {
      return;
    }

    BankPlugin plugin = BankPlugin.getPlugin();

    plugin.getSessionMap().values()
        .stream()
        .filter(bankRun -> bankRun.getVaultKey().equals(vaultKey))
        .findFirst()
        .ifPresent(bankRun -> new NumpadPage(document, bankRun));
  }

  private void clearCode() {
    Arrays.fill(code, EMPTY);
  }

  private int freeIndex() {
    for (int i = 0; i < code.length; i++) {
      char ch = code[i];
      if (ch == EMPTY) {
        return i;
      }
    }

    return -1;
  }

  private void updateCodeElement() {
    if (codeOutputText == null) {
      return;
    }

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < code.length; i++) {
      if (i != 0) {
        builder.append(' ');
      }

      builder.append(code[i]);
    }

    codeOutputText.setTextContent(builder.toString());
  }

  boolean testCodeCorrect() {
    char[] correct = run.getInnerVaultCode();

    for (int i = 0; i < correct.length; i++) {
      char correctCh = correct[i];
      char ch = code[i];

      if (ch == correctCh) {
        continue;
      }

      return false;
    }

    run.setInnerVault(true);
    document.getView().close();

    return true;
  }

  class NumpadPressListener implements EventListener.Typed<MouseEvent> {

    @Override
    public void handleEvent(MouseEvent event) {
      Element target = event.getTarget();
      if (target == null) {
        return;
      }

      String numpadValue = target.getAttribute("numpad");
      if (Strings.isNullOrEmpty(numpadValue)) {
        return;
      }

      int idx = freeIndex();

      switch (numpadValue) {
        case "0":
        case "1":
        case "2":
        case "3":
        case "4":
        case "5":
        case "6":
        case "7":
        case "8":
        case "9":
          if (idx == -1) {
            clearCode();
            idx = 0;
          }

          code[idx] = numpadValue.charAt(0);
          if (testCodeCorrect()) {
            return;
          }

          updateCodeElement();
          break;

        case "clear":
          clearCode();
          updateCodeElement();
          break;

        case "back":
          if (idx == -1) {
            code[code.length - 1] = EMPTY;
          } else if (idx > 0) {
            code[idx - 1] = EMPTY;
          } else {
            return;
          }

          updateCodeElement();
          break;

        default:
          // idk lol
          break;
      }
    }
  }
}
