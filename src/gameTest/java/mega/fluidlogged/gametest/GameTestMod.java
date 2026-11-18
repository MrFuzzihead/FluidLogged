package mega.fluidlogged.gametest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

/**
 * Test-only mod: a separate, non-shipped {@code @Mod} that asserts FluidLogged works in the real game.
 *
 * <p>
 * It lives in the {@code gameTest} source set and is only put on the classpath by
 * {@code ./gradlew gameTestServer} (or {@code -PgameTest}), so it never reaches the published jar.
 * </p>
 *
 * <pre>
 * ./gradlew gameTestServer
 * </pre>
 */
@Mod(
    modid = GameTestMod.MOD_ID,
    name = "FluidLogged Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:fluidlogged")
public class GameTestMod {

    public static final String MOD_ID = "fluidloggedgametest";
    static final String OK_MARKER = "FLUIDLOGGED_GAMETEST_OK";
    static final String FAIL_MARKER = "FLUIDLOGGED_GAMETEST_FAILED";

    private boolean ran;

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (ran) {
            return;
        }
        ran = true;

        int failures;
        try {
            failures = ServerGameTests.runAll();
        } catch (Throwable t) {
            t.printStackTrace();
            failures = 1;
        }

        if (failures == 0) {
            System.out.println(OK_MARKER);
            FMLCommonHandler.instance()
                .exitJava(0, false);
        } else {
            System.out.println(FAIL_MARKER + ": " + failures + " assertion(s) failed");
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }
}
