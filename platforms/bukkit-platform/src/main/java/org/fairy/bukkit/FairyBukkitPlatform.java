/*
 * MIT License
 *
 * Copyright (c) 2021 Imanity
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.fairy.bukkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.fairy.bukkit.events.PostServicesInitialEvent;
import org.fairy.bukkit.impl.ComponentHolderBukkitListener;
import org.fairy.bukkit.listener.events.Events;
import org.fairy.bukkit.plugin.FairyInternalPlugin;
import org.fairy.bukkit.util.Players;
import org.fairy.bukkit.util.SpigotUtil;
import org.fairy.bean.ComponentRegistry;
import org.fairy.FairyPlatform;
import org.fairy.bukkit.impl.BukkitPluginHandler;
import org.fairy.bukkit.impl.BukkitTaskScheduler;
import org.fairy.library.Library;
import org.fairy.ExtendedClassLoader;
import org.fairy.mc.MCPlayer;
import org.fairy.plugin.PluginManager;
import org.fairy.task.ITaskScheduler;
import org.fairy.util.terminable.TerminableConsumer;
import org.fairy.util.terminable.composite.CompositeTerminable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

public final class FairyBukkitPlatform extends FairyPlatform implements TerminableConsumer {

    private static final Logger LOGGER = LogManager.getLogger();

    public static FairyBukkitPlatform INSTANCE;
    public static FairyInternalPlugin PLUGIN = new FairyInternalPlugin();

    private final ExtendedClassLoader classLoader;
    private final File dataFolder;
    private final CompositeTerminable compositeTerminable;

    @NotNull
    @Override
    public <T extends AutoCloseable> T bind(@NotNull T terminable) {
        return this.compositeTerminable.bind(terminable);
    }

    public FairyBukkitPlatform(File dataFolder) {
        this.dataFolder = dataFolder;
        this.compositeTerminable = CompositeTerminable.create();
        this.classLoader = new ExtendedClassLoader(this.getClass().getClassLoader());
    }

    @Override
    public void load() {
        super.load();

        PluginManager.initialize(new BukkitPluginHandler());
        MCPlayer.Companion.PLAYER_TO_UUID = Players::tryGetUniqueId;
    }

    @Override
    public void enable() {
        SpigotUtil.init();
        ComponentRegistry.registerComponentHolder(new ComponentHolderBukkitListener());

        super.enable();
    }

    @Override
    public void disable() {
        super.disable();
    }

    @Override
    public void onPostServicesInitial() {
        Events.call(new PostServicesInitialEvent());
    }

    @Override
    public void saveResource(String name, boolean replace) {
        if (name != null && !name.equals("")) {
            name = name.replace('\\', '/');
            InputStream in = this.getResource(name);
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + name + "' cannot be found");
            } else {
                File outFile = new File(this.dataFolder, name);
                int lastIndex = name.lastIndexOf(47);
                File outDir = new File(this.dataFolder, name.substring(0, Math.max(lastIndex, 0)));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                try {
                    if (outFile.exists() && !replace) {
                        LOGGER.warn("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
                    } else {
                        OutputStream out = new FileOutputStream(outFile);
                        byte[] buf = new byte[1024];

                        int len;
                        while((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        out.close();
                        in.close();
                    }
                } catch (IOException var10) {
                    LOGGER.info("Could not save " + outFile.getName() + " to " + outFile, var10);
                }

            }
        } else {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
    }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        } else {
            try {
                URL url = this.getClass().getClassLoader().getResource(filename);
                if (url == null) {
                    return null;
                } else {
                    URLConnection connection = url.openConnection();
                    connection.setUseCaches(false);
                    return connection.getInputStream();
                }
            } catch (IOException var4) {
                return null;
            }
        }
    }

    @Override
    public ExtendedClassLoader getClassloader() {
        return this.classLoader;
    }

    @Override
    public File getDataFolder() {
        return this.dataFolder;
    }

    @Override
    public void shutdown() {
        Bukkit.shutdown();
    }

    @Override
    public boolean isRunning() {
        return true; // TODO
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public ITaskScheduler createTaskScheduler() {
        return new BukkitTaskScheduler();
    }

    @Override
    public Set<Library> getDependencies() {
        Set<Library> libraries = new HashSet<>();
        if (SpigotUtil.SPIGOT_TYPE != SpigotUtil.SpigotType.IMANITY) {
            libraries.add(Library.FAST_UTIL);
        }
        return libraries;
    }
}
