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
include("user-titles")
include("server-list")
include("dialogues")
include("sell-shop")
include("auto-afk")