package com.yoyolegrand.partyxpshare;

import com.yoyolegrand.partyxpshare.ShareMath.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ShareMathTest {

    static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4), S = new UUID(0, 9);

    static Settings s(Mode m) { return new Settings(m, 30, 5, 0, 50, 0); }

    static Map<UUID, Integer> map(List<Share> l) {
        Map<UUID, Integer> m = new HashMap<>();
        for (Share x : l) m.put(x.id(), x.xp());
        return m;
    }

    @Test public void damageMode_100xp_20pct() {
        var r = map(ShareMath.compute(100, List.of(new Participant(A, 80, true), new Participant(B, 20, false)), List.of(), s(Mode.DAMAGE), new Random(1)));
        assertEquals(80, (int) r.get(A));
        assertEquals(20, (int) r.get(B));
    }

    @Test public void equalMode_4players_25each() {
        var p = List.of(new Participant(A, 70, true), new Participant(B, 10, false), new Participant(C, 10, false), new Participant(D, 10, false));
        var r = map(ShareMath.compute(100, p, List.of(), s(Mode.EQUAL), new Random(1)));
        for (UUID id : List.of(A, B, C, D)) assertEquals(25, (int) r.get(id));
    }

    @Test public void hybrid_killerBonusPlusDamage() {
        var r = map(ShareMath.compute(100, List.of(new Participant(A, 50, true), new Participant(B, 50, false)), List.of(), s(Mode.HYBRID), new Random(1)));
        assertEquals(65, (int) r.get(A));   // 30 + 35
        assertEquals(35, (int) r.get(B));
    }

    @Test public void minContribution_excludesLeecherAndRedistributes() {
        var r = map(ShareMath.compute(1000, List.of(new Participant(A, 60, true), new Participant(B, 37, false), new Participant(C, 3, false)), List.of(), s(Mode.DAMAGE), new Random(1)));
        assertNull(r.get(C));
        assertEquals(1000, r.get(A) + r.get(B), 1);
        assertEquals(619, (int) r.get(A), 1);   // 60 / 97
    }

    @Test public void singleRecipient_returnsNull() {
        assertNull(ShareMath.compute(100, List.of(new Participant(A, 10, true)), List.of(), s(Mode.DAMAGE), new Random(1)));
    }

    @Test public void below_threshold_alone_means_vanilla() {
        assertNull(ShareMath.compute(100, List.of(new Participant(A, 97, true), new Participant(B, 3, false)), List.of(), s(Mode.DAMAGE), new Random(1)));
    }

    @Test public void groupBonus_capped() {
        Settings st = new Settings(Mode.EQUAL, 0, 0, 10, 15, 0);
        var p = List.of(new Participant(A, 1, true), new Participant(B, 1, false), new Participant(C, 1, false));
        var r = map(ShareMath.compute(90, p, List.of(), st, new Random(1)));
        int total = r.values().stream().mapToInt(i -> i).sum();
        assertTrue(total >= 102 && total <= 105);   // 90 * 1.15 = 103.5
    }

    @Test public void supportShare() {
        Settings st = new Settings(Mode.DAMAGE, 0, 5, 0, 0, 10);
        var r = map(ShareMath.compute(100, List.of(new Participant(A, 10, true)), List.of(S), st, new Random(1)));
        assertEquals(90, (int) r.get(A));
        assertEquals(10, (int) r.get(S));
    }

    @Test public void randomRounding_keepsAverage() {
        Random rng = new Random(42);
        long sum = 0;
        for (int i = 0; i < 100000; i++) sum += ShareMath.randomRound(1.25, rng);
        assertEquals(1.25, sum / 100000.0, 0.01);
    }

    @Test public void conservation_damage() {
        Random rng = new Random(7);
        double total = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            var r = ShareMath.compute(5, List.of(new Participant(A, 33, true), new Participant(B, 33, false), new Participant(C, 34, false)), List.of(), s(Mode.DAMAGE), rng);
            for (Share x : r) total += x.xp();
        }
        assertEquals(5.0, total / n, 0.05);
    }
}
