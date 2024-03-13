package net.arcadiusmc.usables.items;

public class ItemActionType extends ItemType<ItemAction> {

  public static final ItemActionType GIVE = new ItemActionType(true);
  public static final ItemActionType TAKE = new ItemActionType(false);

  private final boolean give;

  public ItemActionType(boolean give) {
    this.give = give;
  }

  @Override
  ItemAction construct(ItemProvider provider) {
    return new ItemAction(provider, give);
  }
}
