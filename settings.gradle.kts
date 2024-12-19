rootProject.name = "arcadius-plugins"

// Core functionality
include("commons")
include("core")
include("menus")
include("punishments")

// Scripting
include("scripting-impl")
include("scripting")

// Service providers, tools
include("vanilla-hook")
include("webmap")
include("vault-hook")
include("class-loader-tools")
include("early-shutdown")
include("discord")

// Regular plugins
include("extended-items")
include("extended-entities")
include("user-titles")
include("server-list")
include("dialogues")
include("sell-shop")
include("auto-afk")
include("staff-chat")
include("sign-shops")
include("player-markets")
include("holograms")
include("waypoints")
include("factions")
include("usables")
include("dungeons2")
include("structures")
include("kingmaker")
include("merchants")
include("voicechat-hook")
include("cosmetics")
include("bank")
include("emperors-powers")
include("voting-hook")

include("pirate-quests")

include("mail")
include("mail-impl")