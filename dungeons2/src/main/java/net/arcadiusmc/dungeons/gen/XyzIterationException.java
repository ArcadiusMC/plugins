package net.arcadiusmc.dungeons.gen;

import lombok.Getter;

@Getter
public class XyzIterationException extends RuntimeException {

  private final int x;
  private final int y;
  private final int z;

  public XyzIterationException(Throwable cause, int x, int y, int z) {
    super("At " + x + " " + y + " " + z, cause);
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
