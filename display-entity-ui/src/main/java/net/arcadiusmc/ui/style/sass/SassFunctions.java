package net.arcadiusmc.ui.style.sass;

public interface SassFunctions {

  SassFunction LIGHTEN = new BrightnessFunction(false);
  SassFunction DARKEN = new BrightnessFunction(true);

  SassFunction RGB = new RgbFunction(false);
  SassFunction RGBA = new RgbFunction(true);
}
