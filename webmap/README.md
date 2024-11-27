# WebMap Implementation Module
Provides 2 implementations for the WebMap API defined in the 
`commons` module.
  
The implementation used is based on what plugins are available
in the server the plugin is running in:
- If `BlueMap` exists, the BlueMap implementation is used
- If `DynMap` exists, the dynmap implementation is used.
- If neither of those exist, the plugin disables itself.