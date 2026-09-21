package com.hyunseo.hyunseorpg.command.paper;

import java.util.List;

/** Single metadata source for commands registered through Paper's command lifecycle. */
public record PaperCommandSpec(String name, String description, List<String> aliases) {
    public PaperCommandSpec {
        aliases = List.copyOf(aliases);
    }
}
