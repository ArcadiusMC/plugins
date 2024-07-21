package net.arcadiusmc.ui.style;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.Nullable;

public final class StylePropertyMap {

  private Object[] ruleValues;

  private boolean isEmpty() {
    if (ruleValues == null || ruleValues.length < 1) {
      return true;
    }

    for (Object ruleValue : ruleValues) {
      if (ruleValue == null) {
        continue;
      }

      return false;
    }

    return true;
  }

  public void putAll(StylePropertyMap from) {
    RuleIterator it = from.iterator();
    while (it.hasNext()) {
      it.next();
      set(it.rule(), it.value());
    }
  }

  public <T> boolean has(Rule<T> rule) {
    if (ruleValues == null || ruleValues.length <= rule.id) {
      return false;
    }

    T v = (T) ruleValues[rule.id];
    return v != null;
  }

  public <T> T get(Rule<T> rule) {
    if (!has(rule)) {
      return rule.getDefaultValue();
    }

    return (T) ruleValues[rule.id];
  }

  public <T> void set(Rule<T> rule, @Nullable T value) {
    Objects.requireNonNull(rule, "Null rule");

    int id = rule.id;

    if (value == null) {
      remove(rule);
      return;
    }

    if (ruleValues == null) {
      ruleValues = new Object[id + 1];
      ruleValues[id] = value;
      return;
    }

    ruleValues = ObjectArrays.ensureCapacity(ruleValues, id + 1);
    ruleValues[id] = value;
  }

  public <T> void remove(Rule<T> rule) {
    if (!has(rule)) {
      return;
    }

    ruleValues[rule.id] = null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("[");

    if (ruleValues == null || ruleValues.length < 1) {
      builder.append("]");
      return builder.toString();
    }

    boolean anyPrinted = false;

    for (int i = 0; i < ruleValues.length; i++) {
      Object v = ruleValues[i];

      if (v == null) {
        continue;
      }

      Rule r = Rules.REGISTRY.orNull(i);

      if (r == null) {
        continue;
      }

      if (anyPrinted) {
        builder.append(", ");
      }

      builder.append(r.key)
          .append("=")
          .append(v);

      anyPrinted = true;
    }

    builder.append("]");
    return builder.toString();
  }

  public void clear() {
    if (ruleValues == null || ruleValues.length < 1) {
      return;
    }

    Arrays.fill(ruleValues, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof StylePropertyMap rset)) {
      return false;
    }

    if (isEmpty()) {
      return rset.isEmpty();
    }

    if (rset.isEmpty()) {
      return false;
    }

    int slen = this.length();
    int olen = rset.length();
    int max = Math.max(slen, olen);

    for (int i = 0; i < max; i++) {
      Object self = this.safeGet(i);
      Object other = rset.safeGet(i);

      if (!Objects.equals(self, other)) {
        return false;
      }
    }

    return true;
  }

  private int length() {
    return ruleValues == null ? 0 : ruleValues.length;
  }

  private Object safeGet(int i) {
    if (ruleValues == null) {
      return null;
    }
    if (i < 0 || i >= ruleValues.length) {
      return null;
    }
    return ruleValues[i];
  }

  public RuleIterator iterator() {
    return new RuleIterator();
  }

  public DifferenceIterator difference(StylePropertyMap other) {
    return new DifferenceIterator(this, other);
  }

  public static class DifferenceIterator {

    private int idx = 0;
    private Rule<Object> rule;
    private Object self;
    private Object other;

    final StylePropertyMap selfSet;
    final StylePropertyMap otherSet;

    public DifferenceIterator(StylePropertyMap selfSet, StylePropertyMap otherSet) {
      this.selfSet = selfSet;
      this.otherSet = otherSet;
    }

    public boolean hasNext() {
      if (idx >= Rules.REGISTRY.size()) {
        return false;
      }

      while (idx < Rules.REGISTRY.size()) {
        Rule<Object> r = (Rule<Object>) Rules.REGISTRY.orNull(idx);

        if (r == null) {
          idx++;
          continue;
        }

        Object self = selfSet.get(r);
        Object other = otherSet.get(r);

        if (Objects.equals(self, other)) {
          idx++;
          continue;
        }

        return true;
      }

      return false;
    }

    public void next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      rule = (Rule<Object>) Rules.REGISTRY.orNull(idx);
      self = selfSet.get(rule);
      other = otherSet.get(rule);

      idx++;
    }

    private void ensureSet() {
      if (rule == null) {
        throw new IllegalStateException("next() has not been called once");
      }
    }

    public Rule<Object> rule() {
      ensureSet();
      return rule;
    }

    public Object selfValue() {
      ensureSet();
      return self;
    }

    public Object otherValue() {
      ensureSet();
      return other;
    }
  }

  public class RuleIterator {

    int index = 0;
    int currentId = -1;

    public boolean hasNext() {
      if (ruleValues == null) {
        return false;
      }

      while (index < ruleValues.length) {
        Object o = ruleValues[index];

        if (o == null) {
          index++;
          continue;
        }

        return true;
      }

      return false;
    }

    public void next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      currentId = index;
      index++;
    }

    private void ensureCurrent() {
      if (currentId >= 0) {
        return;
      }

      throw new IllegalStateException("next() has not been called");
    }

    public Object value() {
      ensureCurrent();
      return ruleValues[currentId];
    }

    public Rule<Object> rule() {
      ensureCurrent();
      return (Rule<Object>) Rules.REGISTRY.orThrow(currentId);
    }
  }
}
