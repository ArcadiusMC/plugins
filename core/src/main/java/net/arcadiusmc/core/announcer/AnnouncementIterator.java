package net.arcadiusmc.core.announcer;

import net.arcadiusmc.text.ViewerAwareMessage;

interface AnnouncementIterator {

  boolean hasNext();

  void reset();

  ViewerAwareMessage next();
}
