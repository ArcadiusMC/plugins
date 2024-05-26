package net.arcadiusmc.ui.render;

interface Visitor<C, R> {

  R visitText(TextRenderObject object, C c);

  R visitItem(ItemRenderObject object, C c);
}
