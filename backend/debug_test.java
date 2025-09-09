import io.github.redouanebali.generation.util.SeedPlacementUtil;
import java.util.List;
public class debug_test {
    public static void main(String[] args) {
        System.out.println("Seeds for drawSize=64, nbSeeds=16:");
        List<Integer> seeds16 = SeedPlacementUtil.getSeedsPositions(64, 16);
        System.out.println("Size: " + seeds16.size() + ", values: " + seeds16);
        System.out.println("\nSeeds for drawSize=64, nbSeeds=24:");
        try {
            List<Integer> seeds24 = SeedPlacementUtil.getSeedsPositions(64, 24);
            System.out.println("Size: " + seeds24.size() + ", values: " + seeds24);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
