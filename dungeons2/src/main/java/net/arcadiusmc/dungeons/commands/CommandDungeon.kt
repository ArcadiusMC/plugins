package net.arcadiusmc.dungeons.commands

import com.google.gson.JsonElement
import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.serialization.JsonOps
import net.arcadiusmc.Loggers
import net.arcadiusmc.command.BaseCommand
import net.arcadiusmc.command.Commands
import net.arcadiusmc.command.Exceptions
import net.arcadiusmc.command.arguments.Arguments
import net.arcadiusmc.command.arguments.UserParseResult
import net.arcadiusmc.command.help.UsageFactory
import net.arcadiusmc.dungeons.*
import net.arcadiusmc.dungeons.gen.DungeonGenerator
import net.arcadiusmc.text.Messages
import net.arcadiusmc.text.page.*
import net.arcadiusmc.user.User
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.io.PluginJar
import net.arcadiusmc.utils.io.SerializationHelper
import net.forthecrown.grenadier.CommandSource
import net.forthecrown.grenadier.Completions
import net.forthecrown.grenadier.GrenadierCommand
import net.forthecrown.grenadier.internal.SimpleVanillaMapped
import net.forthecrown.grenadier.types.ArgumentTypes
import net.forthecrown.grenadier.types.ParsedPosition
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.spongepowered.math.vector.Vector3i
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CompletableFuture

class CommandDungeon : BaseCommand {

  private val LOGGER = Loggers.getLogger()

  private val plugin: DungeonsPlugin
  private val pageFormat: PageFormat<DungeonSession>

  constructor(plugin: DungeonsPlugin) : super("dungeons") {
    this.plugin = plugin
    this.pageFormat = createPageFormat()

    setDescription("Dungeons admin command")
    register()
  }

  override fun populateUsages(factory: UsageFactory) {
    factory.usage("reload-config")
      .addInfo("Reloads the dungeons config.")

    factory.usage("generate [<cell position>]")
      .addInfo("Generates dungeons, if cell position is not set")
      .addInfo("A random cell is used")
  }

  private fun createPageFormat(): PageFormat<DungeonSession> {
    val f: PageFormat<DungeonSession> = PageFormat.create()

    f.header = Header.create<DungeonSession?>().title { it, writer, ctx ->
      writer.write(Messages.render("dungeons.sessionList.header"))
    }
    f.footer = Footer.ofButton("/dungeons sessions list %s %s")

    f.entry = PageEntry.create()
    f.entry.entryDisplay = PageEntry.EntryDisplay{ writer, entry, idx, ctx, it ->
      writer.write(entry.displayName)
    }

    return f
  }

  override fun createCommand(command: GrenadierCommand) {
    val sessionArg = SessionArgument()

    command
      .then(literal("reload-config")
        .executes { c ->
          plugin.reloadConfig()

          c.source.sendSuccess(Messages.renderText("dungeons.reloadedConfig", c.source))
          SINGLE_SUCCESS
        }
      )

      .then(literal("generate")
        .executes { c ->
          generateDungeon(c.source, null, null)
          SINGLE_SUCCESS
        }

        .then(argument("cell", ArgumentTypes.blockPosition2d())
          .executes { c ->
            val parsePos = c.getArgument("cell", ParsedPosition::class.java)

            val x = parsePos.xCoordinate.value.toInt()
            val z = parsePos.zCoordinate.value.toInt()

            generateDungeon(c.source, x, z)
            SINGLE_SUCCESS
          }
        )
      )

      .then(literal("user-settings")
        .then(literal("query")
          .then(argument("player", Arguments.USER)
            .executes {
              val player = Arguments.getUser(it, "player")
              val settings = plugin.settings[player.uniqueId]

              it.source.sendSuccess(
                Messages.render("dungeons.usersettings.query")
                  .addValue("player", player)
                  .addValue("difficulty", settings.difficulty)
                  .addValue("levelSize", settings.size)
                  .create(it.source)
              )
              SINGLE_SUCCESS
            }
          )
        )

        .then(literal("set")
          .then(literal("difficulty")
            .then(argument("player", Arguments.USER)
              .then(argument("diff", ArgumentTypes.enumType(DungeonDifficulty::class.java))
                .executes {
                  val player = Arguments.getUser(it, "player")
                  val settings = plugin.settings[player.uniqueId]
                  val difficulty = it.getArgument("diff", DungeonDifficulty::class.java)

                  settings.difficulty = difficulty

                  it.source.sendSuccess(
                    Messages.render("dungeons.usersettings.difficulty")
                      .addValue("player", player)
                      .addValue("difficulty", difficulty)
                      .create(it.source)
                  )
                  SINGLE_SUCCESS
                }
              )
            )
          )

          .then(literal("size")
            .then(argument("player", Arguments.USER)
              .then(argument("size", ArgumentTypes.enumType(LevelSize::class.java))
                .executes {
                  val player = Arguments.getUser(it, "player")
                  val settings = plugin.settings[player.uniqueId]
                  val size = it.getArgument("diff", LevelSize::class.java)

                  settings.size = size

                  it.source.sendSuccess(
                    Messages.render("dungeons.usersettings.levelSize")
                      .addValue("player", player)
                      .addValue("levelSize", size)
                      .create(it.source)
                  )
                  SINGLE_SUCCESS
                }
              )
            )
          )
        )
      )

      .then(literal("sessions")
        .then(literal("list")
          .then(argument("page", IntegerArgumentType.integer(1))
            .executes { c ->
              val page = c.getArgument("page", Int::class.java)
              listSessions(c.source, page)
            }

            .then(argument("page-size", IntegerArgumentType.integer(5, 20))
              .executes { c ->
                val page = c.getArgument("page", Int::class.java)
                val pageSize = c.getArgument("pageSize", Int::class.java)
                listSessions(c.source, page, pageSize)
              }
            )
          )

          .executes { c -> listSessions(c.source) }
        )

        .then(literal("info")
          .then(argument("session", sessionArg)
            .executes { c -> SINGLE_SUCCESS }
          )
        )

        .then(literal("start")
          .then(argument("session", sessionArg)
            .executes {
              val session = getSession(it)
              session.start()

              it.source.sendSuccess(
                Messages.render("dungeons.sessions.started")
                  .addValue("session", session.displayName)
                  .create(it.source)
              )

              SINGLE_SUCCESS
            }
          )
        )

        .then(literal("create")
          .then(argument("players", Arguments.ONLINE_USERS)
            .executes {
              val users = Arguments.getUsers(it, "players")

              for (user in users) {
                val opt = plugin.manager.getSession(user.player)

                if (opt.isEmpty) {
                  continue
                }

                throw Messages.render("dungeons.error.alreadyInSession")
                  .addValue("player", user)
                  .exception(it.source)
              }

              val session = plugin.manager.newSession()
              for (user in users) {
                session.addPlayer(user.player)
              }

              it.source.sendSuccess(
                Messages.render("dungeons.sessions.created")
                  .addValue("players", users.size)
                  .addValue("session", session.displayName)
                  .create(it.source)
              )
              SINGLE_SUCCESS
            }
          )
        )

        .then(literal("add-players")
          .then(argument("session", sessionArg)
            .then(argument("players", Arguments.ONLINE_USERS)
              .executes {
                val session = getSession(it)
                val users = Arguments.getUsers(it, "players")

                for (user in users) {
                  session.addPlayer(user.player)
                }

                it.source.sendSuccess(
                  Messages.render("dungeons.sessions.addedPlayers")
                    .addValue("session", session.displayName)
                    .addValue("players", users.size)
                    .create(it.source)
                )
                SINGLE_SUCCESS
              }
            )
          )
        )
        .then(literal("remove-players")
          .then(argument("session", sessionArg)
            .then(argument("players", Arguments.ONLINE_USERS)
              .executes {
                val session = getSession(it)
                val users = Arguments.getUsers(it, "players")

                for (user in users) {
                  session.removePlayer(user.player)
                }

                it.source.sendSuccess(
                  Messages.render("dungeons.sessions.removedPlayers")
                    .addValue("session", session.displayName)
                    .addValue("players", users.size)
                    .create(it.source)
                )
                SINGLE_SUCCESS
              }
            )
          )
        )

        .then(literal("close")
          .then(argument("session", sessionArg)
            .executes { c ->
              val session = getSession(c)
              val display = session.displayName

              session.close()

              c.source.sendSuccess(
                Messages.render("dungeons.sessions.closed")
                  .addValue("session", display)
                  .create(c.source)
              )
              SINGLE_SUCCESS
            }
          )
        )
      )
  }

  fun listSessions(source: CommandSource, page: Int = 1, pageSize: Int = 5): Int {
    val sessionList = ArrayList(plugin.manager.sessions)
    Commands.ensurePageValid(page, pageSize, sessionList.size)

    val it = PagedIterator.of(sessionList, page - 1, pageSize)
    val formatted = pageFormat.format(it)

    source.sendSuccess(formatted)
    return SINGLE_SUCCESS
  }

  @Throws(CommandSyntaxException::class)
  fun generateDungeon(source: CommandSource, cellX: Int?, cellZ: Int?) {
    val manager = DungeonsPlugin.plugin().manager
    val cellId: Long

    if (cellX == null || cellZ == null) {
      cellId = manager.findFreeCell()
    } else {
      cellId = toCellId(toWorld(cellX), toWorld(cellZ))
    }

    val center = toCellCenter(cellId)
    val placePos = Vector3i(center.x(), 0, center.y())

    val cfg = loadConfig()
    val random = cfg.createRandom()
    var world: World? = DungeonWorld.get()

    if (world == null) {
      world = DungeonWorld.reset()
    }

    Tasks.runAsync {
      val rootPiece = DungeonGenerator.generateLevel(cfg, random)

      Tasks.runSync {
        source.sendSuccess(text("Generated dungeon tree, decorating...", NamedTextColor.GRAY))
      }

      LOGGER.debug("Generated async level")

      val generator = DungeonGenerator(rootPiece, random, cfg)
      generator.position = placePos

      val dungeonBuffer = generator.generateDungeon()

      LOGGER.debug("Starting dungeon placement")

      dungeonBuffer.place(world).whenComplete { unused, throwable ->
        if (throwable != null) {
          LOGGER.error("Failed to place dungeon buffer in world", throwable)
          return@whenComplete
        }

        Tasks.runSync {
          val positionText = text("[${placePos.x()} ${placePos.y()} ${placePos.z()}]")
            .color(NamedTextColor.AQUA)
            .clickEvent(ClickEvent.runCommand(
              "/tp_exact x=${placePos.x()} y=${placePos.y()} z=${placePos.z()} world=${world!!.name}"
            ))
            .hoverEvent(text("Click to teleport!"))

          source.sendSuccess(
            text("Dungeons placed in world at ", NamedTextColor.GRAY)
              .append(positionText)
          )
        }
      }
    }
  }

  @Throws(CommandSyntaxException::class)
  fun loadConfig(): DungeonConfig {
    val p = PluginJar.saveResources("gen-config.yml")

    if (!Files.exists(p)) {
      throw Exceptions.create("Failed to create $p")
    }

    val el: JsonElement
    try {
      el = SerializationHelper.readAsJson(p)
    } catch (exc: IOException) {
      LOGGER.error("Error reading gen config file at {}", p, exc)
      throw Exceptions.create("IO Error reading generator config, check console")
    }

    return DungeonConfig.CODEC.parse(JsonOps.INSTANCE, el)
      .getOrThrow { s: String ->
        LOGGER.error("Failed to parse gen config file at {}: {}", p, s)
        Exceptions.create("Failed to parse generator config: $s")
      }
  }

  @Throws(CommandSyntaxException::class)
  fun getSession(c: CommandContext<CommandSource>): DungeonSession {
    val res = c.getArgument("session", SessionResult::class.java)
    return res.get(c.source)
  }
}

class SessionArgument : ArgumentType<SessionResult>, SimpleVanillaMapped {
  @Throws(CommandSyntaxException::class)
  override fun parse(reader: StringReader): SessionResult {
    if (reader.peek() == '@') {
      val selectorResult = Arguments.ONLINE_USER.parse(reader)
      return SessionFromUser(selectorResult)
    }

    val start = reader.cursor

    try {
      val cellId: Long = reader.readLong()
      return SessionFromId(cellId)
    } catch (exc: CommandSyntaxException) {
      // Ignored
    }

    reader.cursor = start

    val selectorResult = Arguments.ONLINE_USER.parse(reader)
    return SessionFromUser(selectorResult)
  }

  override fun <S> listSuggestions(
    context: CommandContext<S>,
    builder: SuggestionsBuilder
  ): CompletableFuture<Suggestions> {
    val combined = Completions.combine<S>(
      Arguments.ONLINE_USERS::listSuggestions,
      { c, b ->
        val manager = DungeonsPlugin.plugin().manager

        for (usedCellId in manager.usedCellIds) {
          val str = usedCellId.toString()
          if (!Completions.matches(b.remainingLowerCase, str)) {
            continue
          }
          b.suggest(str)
        }

        b.buildFuture()
      }
    )

    return combined.getSuggestions(context, builder)
  }

  override fun getVanillaType(): ArgumentType<*> {
    return Arguments.ONLINE_USERS.vanillaType
  }
}

class SessionFromUser(val result: UserParseResult) : SessionResult {

  override fun get(source: CommandSource): DungeonSession {
    val user: User = result.get(source, false)
    val manager = DungeonsPlugin.plugin().manager

    val sessionOpt: Optional<DungeonSession> = manager.getSession(user.player)

    val sess: DungeonSession = sessionOpt.orElseThrow {
      Messages.render("dungeons.error.notInDungeon")
        .addValue("player", user)
        .exception(source)
    }

    return sess
  }
}

class SessionFromId(val cellId: Long) : SessionResult {
  override fun get(source: CommandSource): DungeonSession {
    val manager: SessionManager = DungeonsPlugin.plugin().manager

    return manager.getSessionByCell(cellId).orElseThrow {
      Messages.render("dungeons.error.unusedCellId").exception(source)
    }
  }
}

interface SessionResult {
  @Throws(CommandSyntaxException::class)
  fun get(source: CommandSource): DungeonSession
}