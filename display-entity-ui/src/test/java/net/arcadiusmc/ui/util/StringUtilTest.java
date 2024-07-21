package net.arcadiusmc.ui.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringUtilTest {

  @Test
  void containsWord() {
    assertTrue(StringUtil.containsWord(" word", "word"));
    assertTrue(StringUtil.containsWord(" word ", "word"));
    assertTrue(StringUtil.containsWord("word ", "word"));
    assertTrue(StringUtil.containsWord("asdads word ", "word"));
    assertTrue(StringUtil.containsWord("asdads word asdasdad", "word"));
    assertTrue(StringUtil.containsWord("word asdasdad", "word"));
    assertTrue(StringUtil.containsWord("word", "word"));

    assertFalse(StringUtil.containsWord("asdwordasd", "word"));
    assertFalse(StringUtil.containsWord("asd wordasd", "word"));
    assertFalse(StringUtil.containsWord("asdword asd", "word"));
    assertFalse(StringUtil.containsWord("wor", "word"));
    assertFalse(StringUtil.containsWord("wor ", "word"));
    assertFalse(StringUtil.containsWord(" wor ", "word"));
    assertFalse(StringUtil.containsWord(" wor", "word"));
  }
}