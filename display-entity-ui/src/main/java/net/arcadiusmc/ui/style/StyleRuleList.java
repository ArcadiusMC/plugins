package net.arcadiusmc.ui.style;

import java.util.NoSuchElementException;

public class StyleRuleList {

  private final Object[] ruleValues;

  public StyleRuleList() {
    ruleValues = new Object[StyleOption.OPTIONS.size()];
  }

  public void mergeFrom(StyleRuleList source) {
    for (int i = 0; i < source.ruleValues.length; i++) {
      Object sourceValue = source.ruleValues[i];

      if (sourceValue == null) {
        continue;
      }

      ruleValues[i] = sourceValue;
    }
  }

  public <T> T get(StyleOption<T> option) {
    return (T) ruleValues[option.id()];
  }

  public <T> T set(StyleOption<T> option, T value) {
    int id = option.id();
    T current = (T) ruleValues[id];
    ruleValues[id] = value;
    return current;
  }

  public RuleIterator iterator() {
    return new RuleIterator();
  }

  public class RuleIterator {

    private int index = -1;

    private boolean hasNext() {
      Object value;

      if (index < 0) {
        throw new IllegalStateException("advance() has not been called yet");
      }

      while (index < ruleValues.length) {
        value = ruleValues[index];

        if (value != null) {
          return true;
        }

        index++;
      }

      return false;
    }

    public boolean advance() {
      index++;
      return hasNext();
    }

    public int ruleId() {
      return index;
    }

    public Object value() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return ruleValues[index];
    }

    public StyleOption option() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return StyleOption.OPTION_ID_LOOKUP[index];
    }
  }
}
