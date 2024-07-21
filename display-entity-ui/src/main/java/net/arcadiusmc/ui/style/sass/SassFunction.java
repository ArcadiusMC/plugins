package net.arcadiusmc.ui.style.sass;

import net.arcadiusmc.ui.util.ParserErrors;

public interface SassFunction {

  Object evaluate(String functionName, ArgsParser parser, ParserErrors errors);
}
