package com.hyunseo.hyunseorpg.activity;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Tracks player-placed activity blocks so activity rewards cannot be duplicated. */
public final class ActivityBlockRepository implements AutoCloseable {
    private final Connection connection;

    public ActivityBlockRepository(JavaPlugin plugin) throws SQLException {
        Objects.requireNonNull(plugin, "plugin");
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("Failed to create plugin data directory");
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC driver is missing from the plugin jar", exception);
        }

        File database = new File(plugin.getDataFolder(), "activity-blocks.db");
        File legacyDatabase = new File(plugin.getDataFolder(), "placed-job-blocks.db");
        if (!database.exists() && legacyDatabase.isFile()) {
            try {
                Files.copy(legacyDatabase.toPath(), database.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            } catch (Exception exception) {
                throw new SQLException("Failed to migrate placed-job-blocks.db", exception);
            }
        }

        this.connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS activity_blocks ("
                    + "world_uuid TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, "
                    + "block_type TEXT NOT NULL, placed_at INTEGER NOT NULL, "
                    + "PRIMARY KEY (world_uuid, x, y, z))");
            try {
                statement.executeUpdate("INSERT OR IGNORE INTO activity_blocks "
                        + "SELECT world_uuid, x, y, z, block_type, placed_at FROM placed_job_blocks");
            } catch (SQLException ignored) {
                // The legacy table is absent for new installations.
            }
        }
    }

    public synchronized boolean isRecorded(Block block) {
        if (block == null || block.getWorld() == null) return false;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM activity_blocks WHERE world_uuid = ? AND x = ? AND y = ? AND z = ?")) {
            bindPosition(statement, block);
            return statement.executeQuery().next();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read activity block", exception);
        }
    }

    public synchronized void record(Block block) {
        if (block == null || block.getWorld() == null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO activity_blocks "
                        + "(world_uuid, x, y, z, block_type, placed_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            bindPosition(statement, block);
            statement.setString(5, block.getType().name());
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to record activity block", exception);
        }
    }

    public synchronized void remove(Block block) {
        if (block == null || block.getWorld() == null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM activity_blocks WHERE world_uuid = ? AND x = ? AND y = ? AND z = ?")) {
            bindPosition(statement, block);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove activity block", exception);
        }
    }

    private void bindPosition(PreparedStatement statement, Block block) throws SQLException {
        statement.setString(1, block.getWorld().getUID().toString());
        statement.setInt(2, block.getX());
        statement.setInt(3, block.getY());
        statement.setInt(4, block.getZ());
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to close activity block database", exception);
        }
    }
}
