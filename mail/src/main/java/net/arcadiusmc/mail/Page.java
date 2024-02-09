package net.arcadiusmc.mail;

import static net.arcadiusmc.command.UserMapTopCommand.DEF_PAGE_SIZE;

import net.arcadiusmc.user.User;

public record Page(User player, int page, int pageSize) {
  public static final Page EMPTY = new Page(null, 1, DEF_PAGE_SIZE);

  @Override
  public String toString() {
    return String.format("page=%s page_size=%s player=%s",
        page,
        pageSize,
        player == null ? "@s" : player.getName()
    );
  }
}
