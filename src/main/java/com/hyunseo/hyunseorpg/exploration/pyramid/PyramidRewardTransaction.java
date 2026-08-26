package com.hyunseo.hyunseorpg.exploration.pyramid;

/** Durable state machine for the single-recipient Pyramid completion mailbox. */
public final class PyramidRewardTransaction {
    public enum State {
        NOT_STARTED("not_started"), RESERVED("reserved"), DELIVERY_PENDING("pending"),
        DELIVERED("delivered"), FINALIZED("finalized");
        private final String value;
        State(String value) { this.value = value; }
        public String value() { return value; }
        public boolean terminal() { return this == FINALIZED; }
        public static State parse(String raw) {
            if (raw == null || raw.isBlank()) return NOT_STARTED;
            for (State state : values()) if (state.value.equalsIgnoreCase(raw.trim())) return state;
            if ("delivering".equalsIgnoreCase(raw.trim())) return DELIVERY_PENDING;
            return NOT_STARTED;
        }
    }

    private PyramidRewardTransaction() { }

    public static State reserve(State current) {
        return current == State.NOT_STARTED ? State.RESERVED : current;
    }

    public static State pending(State current) {
        return current == State.RESERVED ? State.DELIVERY_PENDING : current;
    }

    public static State delivered(State current) {
        return current == State.DELIVERY_PENDING || current == State.RESERVED
                ? State.DELIVERED : current;
    }

    public static State finalize(State current) {
        return current == State.DELIVERED ? State.FINALIZED : current;
    }
}
