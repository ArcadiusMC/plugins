package net.arcadiusmc.usables.items;

public class ItemTestType extends ItemType<ItemTest> {

  public static final ItemTestType CONTAINS = new ItemTestType(true);
  public static final ItemTestType NOT = new ItemTestType(false);

  private final boolean required;

  public ItemTestType(boolean required) {
    this.required = required;
  }

  @Override
  ItemTest construct(ItemProvider provider) {
    return new ItemTest(provider, required);
  }
}
