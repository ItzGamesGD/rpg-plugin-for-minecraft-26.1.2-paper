from pathlib import Path


def replace_once(path: str, old: str, new: str = "") -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))


def remove_between(path: str, start_marker: str, end_marker: str) -> None:
    p = Path(path)
    text = p.read_text()
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    p.write_text(text[:start] + text[end:])


plugin = "src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java"
for line in (
    "import com.hyunseo.hyunseorpg.exploration.ExplorationModule;\n",
    "import com.hyunseo.hyunseorpg.exploration.integration.ExistingHyunseoRpgAdapters;\n",
    "import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;\n",
    "import com.hyunseo.hyunseorpg.exploration.integration.BukkitExplorationPorts;\n",
    "    private ExplorationModule explorationModule;\n",
    "        reloadService.register(\"exploration\", explorationModule::reload);\n",
    "        give.setExplorationModule(explorationModule);\n",
):
    replace_once(plugin, line)
remove_between(
    plugin,
    "        this.explorationModule = new ExplorationModule(this,\n",
    "        this.gatewayPrototypeService = new GatewayPrototypeService(this, configService);\n",
)
replace_once(
    plugin,
    "        if (!explorationModule.start()) {\n"
    "            getLogger().severe(\"Exploration module failed to start; keeping it disabled for this boot.\");\n"
    "        }\n",
)
replace_once(
    plugin,
    "        if (explorationModule != null) {\n"
    "            explorationModule.stop();\n"
    "        }\n",
)

command = "src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java"
for line in (
    "import com.hyunseo.hyunseorpg.exploration.ExplorationModule;\n",
    "import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationStatusSnapshot;\n",
    "    private ExplorationModule explorationModule;\n",
):
    replace_once(command, line)
remove_between(
    command,
    "    public void setExplorationModule(ExplorationModule explorationModule) {\n",
    "    @Override\n",
)
replace_once(
    command,
    "        if (args.length >= 1 && args[0].equalsIgnoreCase(\"exploration\")) {\n"
    "            handleExploration(sender, args);\n"
    "            return true;\n"
    "        }\n",
)
remove_between(command, "    private void handleExploration(", "    private void handleFarming(")
replace_once(
    command,
    "        if (args.length == 2 && args[0].equalsIgnoreCase(\"exploration\")) {\n"
    "            return explorationActionCompletion(args[1]);\n"
    "        }\n",
)
replace_once(
    command,
    'return filterCompletion(List.of("give", "pending", "reload", "doctor", "migrate", "exploration", "farming", "effect", "alchemy", "debug"), prefix);',
    'return filterCompletion(List.of("give", "pending", "reload", "doctor", "migrate", "farming", "effect", "alchemy", "debug"), prefix);',
)
remove_between(
    command,
    "    static List<String> explorationActionCompletion(",
    "    static List<String> farmingActionCompletion(",
)
replace_once(
    command,
    'return filterCompletion(List.of("configs", "items", "mobs", "players", "farming",\n'
    '                "alchemy", "exploration", "legacy", "cleanup", "all"), prefix);',
    'return filterCompletion(List.of("configs", "items", "mobs", "players", "farming",\n'
    '                "alchemy", "legacy", "cleanup", "all"), prefix);',
)

migration = "src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java"
replace_once(
    migration,
    "            if (normalized.equals(\"exploration\") || normalized.equals(\"all\")) {\n"
    "                migrateExploration(lines, changedFiles);\n"
    "            }\n",
)
replace_once(
    migration,
    'List.of("configs", "items", "mobs", "players", "farming", "alchemy", "exploration", "legacy", "cleanup", "all")',
    'List.of("configs", "items", "mobs", "players", "farming", "alchemy", "legacy", "cleanup", "all")',
)
replace_once(
    migration,
    '    private static final List<String> EXPLORATION_FILES = List.of("exploration/structures.yml");\n',
)
remove_between(
    migration,
    "    private void migrateExploration(List<String> lines, List<File> changedFiles) {\n",
    "    /** Explicit activation for the implemented A/B production scope. */\n",
)

architecture = "src/test/java/com/hyunseo/hyunseorpg/architecture/LegacyPlayerRpgCoreRemovalArchitectureTest.java"
p = Path(architecture)
text = p.read_text()
start = text.index("    @Test\n    void questRuntimeIsAbsentWhileWorldExplorationRemainsWired() throws IOException {")
end = text.index("    @Test\n    void rpgLevelDataAndPersistenceRemainActive()", start)
replacement = (
    "    @Test\n"
    "    void questAndLegacyExplorationRuntimeStayRetiredWhileOceanModelRemains() throws IOException {\n"
    "        String plugin = Files.readString(PRODUCTION.resolve(\"com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java\"));\n"
    "        String command = Files.readString(PRODUCTION.resolve(\"com/hyunseo/hyunseorpg/command/RPGGiveCommand.java\"));\n"
    "        String migration = Files.readString(PRODUCTION.resolve(\"com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java\"));\n"
    "        for (String removed : List.of(\"QuestRegistry\", \"QuestService\", \"AutoQuestService\",\n"
    "                \"QuestProgressListener\", \"reloadQuestsConfig\", \"questRegistry\")) {\n"
    "            assertFalse(plugin.contains(removed));\n"
    "        }\n"
    "        assertFalse(plugin.contains(\"ExplorationModule\"));\n"
    "        assertFalse(plugin.contains(\"explorationModule\"));\n"
    "        assertFalse(command.contains(\"setExplorationModule\"));\n"
    "        assertFalse(command.contains(\"handleExploration\"));\n"
    "        assertFalse(migration.contains(\"migrateExploration\"));\n"
    "        assertFalse(migration.contains(\"exploration/structures.yml\"));\n"
    "        assertFalse(migration.contains(\"normalized.equals(\\\"exploration\\\")\"));\n"
    "        Path exploration = PRODUCTION.resolve(\"com/hyunseo/hyunseorpg/exploration\");\n"
    "        assertFalse(Files.exists(exploration.resolve(\"ExplorationModule.java\")));\n"
    "        assertFalse(Files.exists(exploration.resolve(\"runtime\")));\n"
    "        assertFalse(Files.exists(exploration.resolve(\"pyramid\")));\n"
    "        assertFalse(Files.exists(exploration.resolve(\"raid\")));\n"
    "        assertTrue(Files.exists(exploration.resolve(\"ocean/OceanMonumentProgress.java\")));\n"
    "        assertTrue(Files.exists(exploration.resolve(\"ocean/MonumentPhase.java\")));\n"
    "    }\n\n"
)
p.write_text(text[:start] + replacement + text[end:])

doc = "docs/gameplay-subsystem-simplification-stage-6.md"
replace_once(
    doc,
    "World-native exploration encounters remain bootstrapped, reloadable, and cleanly shut down. Pyramid, Outpost, Shipwreck, and Ocean Monument behavior stays independent from the removed quest runtime; Gateway Boss remains independent and active.",
    "The legacy ExplorationModule runtime and its Pyramid, Outpost, Shipwreck, and generic structure framework are retired from production. Only the runtime-independent Ocean Monument logical state model is retained as dormant design code for a possible future vanilla/datapack-native implementation; it is not bootstrapped, reloaded, or exposed through commands. Gateway Boss remains independent and active.",
)

mobs = "src/main/resources/mobs.yml"
replace_once(
    mobs,
    "    drop-table: skeleton_king\n    region: boss\n",
    "    drop-table: \"\"\n    region: boss\n",
)
