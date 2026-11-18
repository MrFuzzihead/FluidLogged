package mega.fluidlogged.gametest;

import java.util.BitSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import mega.fluidlogged.api.FLBlockAccess;
import mega.fluidlogged.api.world.WorldDriver;
import mega.fluidlogged.internal.FLManager;
import mega.fluidlogged.internal.FLUtil;
import mega.fluidlogged.internal.mixin.hook.FLSubChunk;

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
        checkExplosionResistance(world);
        checkFireSuppression(world);
        checkNbtPersistence(world);

        clearCell(world, X, Y, Z);
        clearCell(world, X, Y - 1, Z);

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
        clearCell(world, X, Y - 1, Z);
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

    private static void checkExplosionResistance(WorldServer world) {
        int explosionX = 8;
        int explosionY = Y;
        int explosionZ = 4;
        int dryX = explosionX - 1;
        int wetX = explosionX + 1;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.setBlock(explosionX + dx, explosionY + dy, explosionZ + dz, Blocks.air, 0, 3);
                }
            }
        }

        world.setBlock(dryX, explosionY, explosionZ, Blocks.stone_stairs, 0, 3);
        world.setBlock(wetX, explosionY, explosionZ, Blocks.stone_stairs, 0, 3);
        FLBlockAccess access = (FLBlockAccess) world;
        access.fl$setFluid(wetX, explosionY, explosionZ, FluidRegistry.LAVA);

        world.rand.setSeed(12345L);
        Explosion explosion = new Explosion(world, null, explosionX + 0.5D, explosionY + 0.5D, explosionZ + 0.5D, 8.0F);
        explosion.doExplosionA();

        check(
            "explosion destroys the dry stone stairs",
            explosion.affectedBlockPositions.contains(new ChunkPosition(dryX, explosionY, explosionZ)));
        check(
            "stored lava protects the stone stairs from the explosion",
            !explosion.affectedBlockPositions.contains(new ChunkPosition(wetX, explosionY, explosionZ)));

        clearCell(world, dryX, explosionY, explosionZ);
        clearCell(world, wetX, explosionY, explosionZ);
    }

    private static void checkFireSuppression(WorldServer world) {
        int dryX = 12;
        int wetX = 14;
        int y = Y;
        int z = 0;

        world.setBlock(dryX, y, z, Blocks.oak_stairs, 0, 3);
        world.setBlock(wetX, y, z, Blocks.oak_stairs, 0, 3);
        ((FLBlockAccess) world).fl$setFluid(wetX, y, z, FluidRegistry.WATER);

        BlockFire fire = (BlockFire) Blocks.fire;
        int dryChance = fire.getChanceToEncourageFire(world, dryX, y, z, 0, ForgeDirection.UP);
        int wetChance = fire.getChanceToEncourageFire(world, wetX, y, z, 0, ForgeDirection.UP);

        check("dry wooden stairs encourage fire", dryChance > 0);
        check("waterlogged wooden stairs do not encourage fire", wetChance == 0);
        check("dry wooden stairs can catch fire", fire.canCatchFire(world, dryX, y, z, ForgeDirection.UP));
        check("waterlogged wooden stairs cannot catch fire", !fire.canCatchFire(world, wetX, y, z, ForgeDirection.UP));

        clearCell(world, dryX, y, z);
        clearCell(world, wetX, y, z);
    }

    private static void checkNbtPersistence(WorldServer world) {
        Chunk chunk = world.getChunkFromBlockCoords(X, Z);
        FLManager manager = new FLManager();

        ExtendedBlockStorage source = new ExtendedBlockStorage(0, false);
        ((FLSubChunk) source).fl$setFluidLog(new Fluid[] { FluidRegistry.WATER, null, FluidRegistry.LAVA });
        NBTTagCompound nbt = new NBTTagCompound();
        manager.writeSubChunkToNBT(chunk, source, nbt);

        check("new NBT format carries a version", nbt.getByte("fl$v") == 2);
        check("new NBT format stores fluid names", nbt.hasKey("fl$names"));
        check("new NBT format no longer stores block ids", !nbt.hasKey("fl$ids"));

        ExtendedBlockStorage loaded = new ExtendedBlockStorage(0, false);
        manager.readSubChunkFromNBT(chunk, loaded, nbt);
        Fluid[] roundTripped = ((FLSubChunk) loaded).fl$getFluidLog();
        check(
            "fluid names round-trip with empty cells preserved",
            roundTripped != null && roundTripped[0] == FluidRegistry.WATER
                && roundTripped[1] == null
                && roundTripped[2] == FluidRegistry.LAVA);

        BitSet presence = new BitSet(1);
        presence.set(0);
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setInteger("fl$len", 1);
        legacy.setByteArray("fl$pres", presence.toByteArray());
        legacy.setIntArray("fl$ids", new int[] { Block.getIdFromBlock(Blocks.water) });

        ExtendedBlockStorage legacyLoaded = new ExtendedBlockStorage(0, false);
        manager.readSubChunkFromNBT(chunk, legacyLoaded, legacy);
        Fluid[] legacyFluids = ((FLSubChunk) legacyLoaded).fl$getFluidLog();
        check("legacy block-id NBT still loads", legacyFluids != null && legacyFluids[0] == FluidRegistry.WATER);
    }

    private static void clearCell(WorldServer world, int x, int y, int z) {
        ((FLBlockAccess) world).fl$setFluid(x, y, z, null);
        world.setBlockToAir(x, y, z);
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
