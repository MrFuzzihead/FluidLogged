package mega.fluidlogged.gametest;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.api.world.WorldDriver;
import mega.fluidlogged.internal.FLUtil;

/**
 * In-game assertions for behaviour that cannot be tested headlessly: block registries, a live world,
 * chunk storage and the vanilla liquid simulation.
 *
 * <p>
 * Each {@code check*} method logs its failures with the {@code [FLUIDLOGGED-GAMETEST] FAIL} prefix so the
 * log and the process exit code agree.
 * </p>
 */
final class ServerGameTests {

    private static final String FAIL = "[FLUIDLOGGED-GAMETEST] FAIL ";
    private static final String PASS = "[FLUIDLOGGED-GAMETEST] ok   ";

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private static int failures;
    private static int checks;

    private ServerGameTests() {}

    static int runAll() {
        failures = 0;
        checks = 0;

        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available", false);
            return failures;
        }

        // The test uses the spawn region, but load it explicitly for a dedicated server with no players.
        world.getChunkProvider()
            .loadChunk(X >> 4, Z >> 4);
        world.setBlock(X, Y, Z, Blocks.air, 0, 3);
        world.setBlock(X, Y - 1, Z, Blocks.air, 0, 3);

        checkHolderRules();
        checkStorageAndLight(world);
        checkFlow(world);
        checkBreakLeavesSource(world);

        world.setBlockToAir(X, Y, Z);
        world.setBlockToAir(X, Y - 1, Z);

        System.out.println("[FLUIDLOGGED-GAMETEST] " + (checks - failures) + "/" + checks + " checks passed");
        return failures;
    }

    private static void checkHolderRules() {
        check(
            "stone stairs may hold water",
            WorldDriver.getCanBeFluidLogged(Blocks.stone_stairs, 0, FluidRegistry.WATER));
        check(
            "cobblestone walls may hold water",
            WorldDriver.getCanBeFluidLogged(Blocks.cobblestone_wall, 0, FluidRegistry.WATER));
        check("leaves may hold water", WorldDriver.getCanBeFluidLogged(Blocks.leaves, 0, FluidRegistry.WATER));
        check("solid stone may not hold water", !WorldDriver.getCanBeFluidLogged(Blocks.stone, 0, FluidRegistry.WATER));
    }

    private static void checkStorageAndLight(WorldServer world) {
        world.setBlock(X, Y, Z, Blocks.stone_stairs, 0, 3);
        FLBlockAccess access = (FLBlockAccess) world;
        access.fl$setFluid(X, Y, Z, FluidRegistry.WATER);

        check("fluid is stored in the stairs", access.fl$getFluid(X, Y, Z) == FluidRegistry.WATER);
        check(
            "fluid block is visible through the access",
            FLUtil.getFluidOrBlock(world, X, Y, Z) == FluidRegistry.WATER.getBlock());

        Block water = FluidRegistry.WATER.getBlock();
        check(
            "stored water contributes light opacity",
            world.getBlockLightOpacity(X, Y, Z) >= (water == null ? 0 : water.getLightOpacity()));
    }

    private static void checkFlow(WorldServer world) {
        world.setBlock(X, Y - 1, Z, Blocks.air, 0, 3);
        Fluid water = FluidRegistry.WATER;
        FLUtil.simulate(world, X, Y, Z, new Random(1L), water);

        Block below = world.getBlock(X, Y - 1, Z);
        check("stored water flows down into air", below == Blocks.flowing_water || below == Blocks.water);
        world.setBlockToAir(X, Y - 1, Z);
    }

    private static void checkBreakLeavesSource(WorldServer world) {
        world.setBlock(X, Y, Z, Blocks.stone_stairs, 0, 3);
        FLBlockAccess access = (FLBlockAccess) world;
        access.fl$setFluid(X, Y, Z, FluidRegistry.WATER);

        world.setBlockToAir(X, Y, Z);
        Block remaining = world.getBlock(X, Y, Z);
        check(
            "breaking a waterlogged block leaves a water source",
            remaining == Blocks.water || remaining == Blocks.flowing_water);
    }

    private static void check(String name, boolean ok) {
        checks++;
        if (ok) {
            System.out.println(PASS + name);
        } else {
            failures++;
            System.out.println(FAIL + name);
        }
    }
}
