package net.arcadiusmc.utils.io.source;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Objects;

public record UrlSource(URL url, String name) implements Source {

  @Override
  public StringBuffer read() throws IOException {
    InputStream stream = url.openStream();
    InputStreamReader reader = new InputStreamReader(stream, Sources.CHARSET);
    StringWriter strWriter = new StringWriter();

    reader.transferTo(strWriter);
    StringBuffer buf = strWriter.getBuffer();
    stream.close();

    return buf;
  }

  @Override
  public <S> DataResult<S> save(DynamicOps<S> ops) {
    var builder = ops.mapBuilder();
    var stringUrl = url.toString();

    builder.add("url", ops.createString(stringUrl));

    if (name != null && !Objects.equals(name, stringUrl)) {
      builder.add("name", ops.createString(name));
    }

    return builder.build(ops.empty());
  }
}