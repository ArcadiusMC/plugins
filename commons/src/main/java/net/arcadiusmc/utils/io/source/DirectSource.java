package net.arcadiusmc.utils.io.source;

import com.google.common.base.Strings;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

record DirectSource(CharSequence src, String name) implements Source {

  @Override
  public StringBuffer read() {
    return new StringBuffer(src);
  }

  @Override
  public <S> DataResult<S> save(DynamicOps<S> ops) {
    var builder = ops.mapBuilder();

    if (!Strings.isNullOrEmpty(name)) {
      builder.add("name", ops.createString(name));
    }

    return builder
        .add("raw", ops.createString(src.toString()))
        .build(ops.empty());
  }
}