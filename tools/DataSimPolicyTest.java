import dev.manit.networktoggle.DataSimPolicy;

public final class DataSimPolicyTest {
    private static int checks;

    private static void check(String name, int expected, int actual) {
        checks++;
        if (actual != expected) throw new AssertionError(name + ": " + actual + " != " + expected);
        System.out.println("PASS " + name);
    }

    public static void main(String[] args) {
        int[] sims = {3, 7};
        check("data on SIM 1", 3, DataSimPolicy.resolve(3, 3, sims));
        check("data changes to SIM 2", 7, DataSimPolicy.resolve(7, 7, sims));
        check("temporary data switch overrides default", 7, DataSimPolicy.resolve(7, 3, sims));
        check("no active bearer uses configured data SIM", 7, DataSimPolicy.resolve(-1, 7, sims));
        check("no SIM refuses mutation", -1, DataSimPolicy.resolve(-1, -1, new int[0]));
        check("removed SIM refuses mutation", -1, DataSimPolicy.resolve(7, 3, new int[]{3}));
        check("invalid default refuses mutation", -1, DataSimPolicy.resolve(-1, 9, sims));
        check("subscription zero is valid", 0, DataSimPolicy.resolve(0, 0, new int[]{0}));
        check("reinserted SIM gets new ID", 12, DataSimPolicy.resolve(12, 12, new int[]{12}));
        System.out.println(checks + " policy checks passed (host JVM; not Android/device tests).");
    }
}
