package com.hyunseo.hyunseorpg.prototype.thousandeyes;

/** Pure state ownership used by the runtime controller and lifecycle regression tests. */
public final class ThousandEyesLifecycle {
    private boolean active;
    private ThousandEyesState state = ThousandEyesState.IDLE;

    public void activate() {
        active = true;
        state = ThousandEyesState.IDLE;
    }

    public boolean start(ThousandEyesState initialState) {
        if (!active || state != ThousandEyesState.IDLE || initialState == ThousandEyesState.IDLE) return false;
        state = initialState;
        return true;
    }

    /** Returns true exactly when the ninth marker transitions recording into dash. */
    public boolean markerRecorded(int markerCount) {
        if (!active || state != ThousandEyesState.PATH_RECORDING || markerCount != 9) return false;
        state = ThousandEyesState.PATH_DASH;
        return true;
    }

    public void centralReleased() {
        if (active && state == ThousandEyesState.CENTRAL_LASER_CHARGE) state = ThousandEyesState.CENTRAL_LASER_RELEASE;
    }

    public void recover() {
        if (active) state = ThousandEyesState.RECOVERING;
    }

    public void idle() {
        if (active) state = ThousandEyesState.IDLE;
    }

    public void remove() {
        active = false;
        state = ThousandEyesState.IDLE;
    }

    public boolean active() {
        return active;
    }

    public ThousandEyesState state() {
        return state;
    }
}
