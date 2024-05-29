package net.arcadiusmc.gradle

const val GRENADIER         = "net.forthecrown:grenadier:2.4.1"
const val GRENADIER_ANNOT   = "net.forthecrown:grenadier-annotations:1.3.3"

const val NBT_LIB           = "net.forthecrown:nbt:1.5.1"
const val PAPER_NBT         = "net.forthecrown:paper-nbt:1.7.3"

const val MATH_LIB          = "org.spongepowered:math:2.1.0-SNAPSHOT"
const val TOML              = "org.tomlj:tomlj:1.1.0"
const val CONFIGURATE       = "org.spongepowered:configurate-core:4.1.2"

const val WORLD_GUARD       = "com.sk89q.worldguard:worldguard-bukkit:7.0.7"
const val FAWE              = "com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.6.4"

const val PAPER_API         = "io.papermc.paper:paper-api:${MC_VERSION}-R0.1-SNAPSHOT"

const val BRIGADIER         = "com.mojang:brigadier:1.2.9"
const val DATA_FIXER_UPPER  = "com.mojang:datafixerupper:7.0.14"

const val PAPERMC_REPO      = "https://repo.papermc.io/repository/maven-public/"
const val MOJANG_REPO       = "https://libraries.minecraft.net"
const val WORLD_GUARD_REPO  = "https://maven.enginehub.org/repo/"
const val SONATYPE_SNAPSHOT = "https://s01.oss.sonatype.org/content/repositories/snapshots/"

val DEPENDENCIES = arrayOf(
    PAPER_API,
    GRENADIER,
    GRENADIER_ANNOT,
    NBT_LIB,
    PAPER_NBT,
    MATH_LIB,
    TOML,
    CONFIGURATE,
    WORLD_GUARD,
    FAWE,
    BRIGADIER,
    DATA_FIXER_UPPER
)

val REPOSITORIES = arrayOf(
    PAPERMC_REPO, MOJANG_REPO, WORLD_GUARD_REPO, SONATYPE_SNAPSHOT
)