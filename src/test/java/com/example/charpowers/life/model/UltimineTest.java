package com.example.charpowers.life.model;

import com.example.charpowers.life.model.Ultimine.P;
import com.example.charpowers.life.model.Ultimine.Shape;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class UltimineTest {
    private static final Predicate<P> ANY = p -> true;
    private static final P O = new P(0, 64, 0);

    private static List<P> run(Shape s, int face, int facing, int max) {
        return Ultimine.cells(s, O, face, facing, 60, max, ANY, ANY);
    }

    @Test
    void theHitBlockIsAlwaysFirst() {
        for (Shape s : Shape.values()) assertEquals(O, run(s, 2, 2, 20).get(0), s.id);
    }

    @Test
    void nothingWhenTheHitBlockCantBreak() {
        assertTrue(Ultimine.cells(Shape.SQUARE, O, 1, 2, 60, 10, p -> false, ANY).isEmpty());
    }

    @Test
    void neverMoreThanTheLimitAndNoRepeats() {
        for (Shape s : Shape.values()) {
            List<P> cells = run(s, 4, 3, 17);
            assertTrue(cells.size() <= 17, s.id);
            assertEquals(cells.size(), new HashSet<>(cells).size(), s.id);
        }
    }

    @Test
    void squaresAreFlatOnTheFaceYouHit() {
        List<P> small = run(Shape.SMALL_SQUARE, 1, 2, 100);
        assertEquals(9, small.size());
        assertTrue(small.stream().allMatch(p -> p.y() == 64));
        List<P> big = run(Shape.SQUARE, 2, 2, 100);
        assertEquals(25, big.size());
        assertTrue(big.stream().allMatch(p -> p.z() == 0));
    }

    @Test
    void tunnelsGoAwayFromTheFace() {
        // hit the north face of a block: the tunnel runs south (+z)
        List<P> line = run(Shape.SMALL_TUNNEL, 2, 2, 5);
        assertEquals(5, line.size());
        for (int i = 0; i < 5; i++) assertEquals(new P(0, 64, i), line.get(i));
        // hit the top: it digs down
        List<P> down = run(Shape.SMALL_TUNNEL, 1, 2, 3);
        assertEquals(new P(0, 62, 0), down.get(2));
    }

    @Test
    void largeTunnelIsThreeByThree() {
        List<P> t = run(Shape.LARGE_TUNNEL, 3, 3, 18);
        assertEquals(18, t.size());
        Set<Integer> depths = new HashSet<>();
        t.forEach(p -> depths.add(p.z()));
        assertEquals(2, depths.size());
    }

    @Test
    void miningTunnelTakesTheBlockBelowWhenYouHitAHighOne() {
        List<P> t = Ultimine.cells(Shape.MINING_TUNNEL, new P(0, 62, 0), 2, 2, 60, 4, ANY, ANY);
        assertEquals(List.of(new P(0, 62, 0), new P(0, 61, 0), new P(0, 62, 1), new P(0, 61, 1)), t);
        List<P> low = Ultimine.cells(Shape.MINING_TUNNEL, new P(0, 60, 0), 2, 2, 60, 2, ANY, ANY);
        assertEquals(new P(0, 61, 0), low.get(1));
    }

    @Test
    void escapeTunnelClimbsAndDiagonalGoesDown() {
        List<P> up = run(Shape.ESCAPE_TUNNEL, 2, 5, 6);
        assertTrue(up.contains(new P(1, 65, 0)));
        List<P> down = run(Shape.DIAGONAL_TUNNEL, 2, 5, 6);
        assertTrue(down.contains(new P(1, 63, 0)));
    }

    @Test
    void shapelessFollowsOnlyTheSameKindThatIsJoinedUp() {
        Set<P> ore = new HashSet<>(List.of(O, new P(1, 64, 0), new P(2, 65, 1), new P(9, 64, 0)));
        List<P> vein = Ultimine.cells(Shape.SHAPELESS, O, 1, 2, 60, 50, ANY, ore::contains);
        assertEquals(3, vein.size());
        assertFalse(vein.contains(new P(9, 64, 0)));
        assertEquals(2, Ultimine.cells(Shape.SHAPELESS, O, 1, 2, 60, 2, ANY, ore::contains).size());
    }

    @Test
    void unbreakableBlocksAreSkipped() {
        List<P> t = Ultimine.cells(Shape.SMALL_SQUARE, O, 1, 2, 60, 100, p -> p.x() != 1, ANY);
        assertEquals(6, t.size());
    }
}
