package dev.manit.networktoggle;

/** Pure selection policy, shared by Android code and host-side tests. */
public final class DataSimPolicy {
    private DataSimPolicy() {}

    public static int resolve(int activeDataId, int defaultDataId, int[] activeSimIds) {
        // An active-data ID includes temporary data switching during calls.
        // Fall back to the configured data SIM only when Android reports no active data ID.
        int candidate = activeDataId >= 0 ? activeDataId : defaultDataId;
        if (candidate < 0) return -1;
        for (int id : activeSimIds) {
            if (id == candidate) return candidate;
        }
        // Never guess SIM 1, reuse a persisted choice, or use an inactive subscription.
        return -1;
    }
}
