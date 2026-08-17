package com.hyunseo.hyunseorpg.core.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** Reloads YAML-backed registries without replacing service instances. */
public final class RPGReloadService {
    private final ConfigService configService;
    private final Map<String, Supplier<ReloadOutcome>> reloaders = new LinkedHashMap<>();

    public RPGReloadService(ConfigService configService) {
        this.configService = Objects.requireNonNull(configService, "configService");
    }

    public void register(String id, BooleanSupplier reloader) {
        if (id == null || id.isBlank() || reloader == null) return;
        reloaders.put(id.trim().toLowerCase(Locale.ROOT), () -> {
            boolean success = reloader.getAsBoolean();
            return success ? ReloadOutcome.pass(id) : ReloadOutcome.fail(id, "validation returned false");
        });
    }

    public void registerDetailed(String id, Supplier<ReloadOutcome> reloader) {
        if (id == null || id.isBlank() || reloader == null) return;
        reloaders.put(id.trim().toLowerCase(Locale.ROOT), reloader);
    }

    public ReloadResult reload(String rawId) {
        String id = rawId == null || rawId.isBlank() ? "all" : rawId.trim().toLowerCase(Locale.ROOT);
        Supplier<ReloadOutcome> reloader = reloaders.get(id);
        if (reloader == null) {
            return new ReloadResult(false, "알 수 없는 리로드 항목: " + id, null);
        }
        try {
            ReloadOutcome outcome = reloader.get();
            if (!outcome.success()) {
                StringBuilder message = new StringBuilder("Reload FAILED: ").append(id);
                for (ReloadDetail detail : outcome.details()) {
                    message.append("\n* ").append(detail.id()).append(": ").append(detail.status());
                    for (String error : detail.details()) message.append("\n  - ").append(error);
                }
                configService.getPlugin().getLogger().warning(message.toString());
                return new ReloadResult(false, message.toString(), null, outcome.details());
            }
            configService.getPlugin().getLogger().info("HyunseoRPG reload succeeded: " + id);
            return new ReloadResult(true, "Reload SUCCEEDED: " + id, null, outcome.details());
        } catch (RuntimeException exception) {
            configService.getPlugin().getLogger().log(Level.SEVERE, "HyunseoRPG reload failed: " + id, exception);
            return new ReloadResult(false, "Reload FAILED: " + id + "\n* exception: " + exception.getMessage(), exception,
                    List.of(ReloadDetail.fail(id, exception.getMessage())));
        }
    }

    public String[] ids() {
        return reloaders.keySet().toArray(String[]::new);
    }

    public record ReloadResult(boolean success, String message, Throwable error, List<ReloadDetail> details) {
        public ReloadResult(boolean success, String message, Throwable error) {
            this(success, message, error, List.of());
        }
    }

    public record ReloadOutcome(boolean success, List<ReloadDetail> details) {
        public static ReloadOutcome pass(String id) {
            return new ReloadOutcome(true, List.of(ReloadDetail.pass(id)));
        }

        public static ReloadOutcome fail(String id, String detail) {
            return new ReloadOutcome(false, List.of(ReloadDetail.fail(id, detail)));
        }
    }

    public record ReloadDetail(String id, String status, List<String> details) {
        public static ReloadDetail pass(String id) { return new ReloadDetail(id, "PASS", List.of()); }
        public static ReloadDetail fail(String id, String detail) {
            return new ReloadDetail(id, "FAIL", detail == null || detail.isBlank() ? List.of() : List.of(detail));
        }
        public static ReloadDetail skip(String id, String detail) {
            return new ReloadDetail(id, "SKIPPED", detail == null ? List.of() : List.of(detail));
        }
    }
}
