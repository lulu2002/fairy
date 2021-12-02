package io.fairytest;

import io.fairyproject.plugin.PluginManager;
import io.fairytest.entity.FairyTestingPlatform;
import io.fairyproject.FairyPlatform;
import io.fairytest.plugin.PluginMock;
import org.junit.BeforeClass;

public abstract class TestingBase {

    public static PluginMock PLUGIN;
    private static boolean INITIALIZED = false;

    @BeforeClass
    public static void setup() {
        if (INITIALIZED) {
            return;
        }
        INITIALIZED = true;
        FairyPlatform fairyPlatform = new FairyTestingPlatform();
        FairyPlatform.INSTANCE = fairyPlatform;

        PLUGIN = new PluginMock();

        fairyPlatform.load();
        PluginManager.INSTANCE.addPlugin(PLUGIN);
        PluginManager.INSTANCE.onPluginInitial(PLUGIN);
        PLUGIN.onInitial();

        fairyPlatform.enable();
        PLUGIN.onPreEnable();
        PluginManager.INSTANCE.onPluginEnable(PLUGIN);
        PLUGIN.onPluginEnable();
    }

}
