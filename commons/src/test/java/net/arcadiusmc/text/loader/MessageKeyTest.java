package net.arcadiusmc.text.loader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MessageKeyTest {

  @Test
  void testCanParse() {
    MessageKey key = safeParse("entry");
    assertEquals("entry", key.key());
    assertTrue(key.arguments().isEmpty());
  }

  @Test
  void testOnlyStartingParentheses() {
    MessageKey key = safeParse("entry1(");
    assertEquals("entry1", key.key());
    assertTrue(key.arguments().isEmpty());
  }

  @Test
  void testNoArguments() {
    MessageKey key = safeParse("entry2()");
    assertEquals("entry2", key.key());
    assertTrue(key.arguments().isEmpty());
  }

  @Test
  void testOneArgument() {
    MessageKey key = safeParse("entry3(arg=true)");
    assertEquals("entry3", key.key());
    assertEquals(1, key.arguments().size());
    assertEquals("true", key.arguments().get("arg"));
  }

  @Test
  void testOneArgumentNoValue() {
    MessageKey key = safeParse("entry3(arg)");
    assertEquals("entry3", key.key());
    assertEquals(1, key.arguments().size());
    assertEquals("", key.arguments().get("arg"));
  }

  @Test
  void testOneArgumentNoValueButEquals() {
    MessageKey key = safeParse("entry3(arg=)");
    assertEquals("entry3", key.key());
    assertEquals(1, key.arguments().size());
    assertEquals("", key.arguments().get("arg"));
  }

  @Test
  void testTwoArguments() {
    MessageKey key = safeParse("entry4(arg=false foo=bar)");
    assertEquals("entry4", key.key());
    assertEquals(2, key.arguments().size());
    assertEquals("false", key.arguments().get("arg"));
    assertEquals("bar", key.arguments().get("foo"));
  }

  @Test
  void testTwoArgumentsNoValue() {
    MessageKey key = safeParse("entry4(arg foo=bar)");
    assertEquals("entry4", key.key());
    assertEquals(2, key.arguments().size());
    assertEquals("", key.arguments().get("arg"));
    assertEquals("bar", key.arguments().get("foo"));
  }

  @Test
  void testArgumentNoEndingParentheses() {
    MessageKey key = safeParse("entry5(arg=false foo=bar");
    assertEquals("entry5", key.key());
    assertEquals(2, key.arguments().size());
    assertEquals("false", key.arguments().get("arg"));
    assertEquals("bar", key.arguments().get("foo"));
  }

  static MessageKey safeParse(String key) {
    return assertDoesNotThrow(() -> MessageKey.parse(key));
  }
}