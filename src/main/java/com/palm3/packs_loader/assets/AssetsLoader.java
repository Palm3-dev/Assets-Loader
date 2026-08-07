package com.palm3.packs_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.common.*;
import com.palm3.packs_loader.logging.Markers;
import com.palm3.packs_loader.logging.PrettyLogging;
import com.palm3.packs_loader.temp.JarLoadingInfos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;

import static com.palm3.packs_loader.PacksLoaderMain.*;
import static com.palm3.packs_loader.logging.PrettyLogging.*;

/**
 * {@link AssetsLoader} is used to load Minecraft mods assets that cannot be used and published directly from your mod.
 * This class is used to extract those assets from a normally downloaded mod {@code .jar} file and use them in-game, with some other features.
 * The cool thing is that the mod version and loader are completely ignored.
 * <br><b>NOTE:</b> the loader could tell the user that an old or unsupported loader mod is unable to load, but you can still play the game just fine.
 * All the incompatible mods will be auto-removed by this mod. See {@link com.palm3.packs_loader.common.IncompatibleModsRemover} for more info.
 */
@ParametersAreNonnullByDefault
@Deprecated(forRemoval = true)
public class AssetsLoader {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    private static final FilesCopier COPIER = new FilesCopier(PL);

    public final Path tempDirectoryPath;
    public final String loaderModName;  // Mod name?

    /**
     * Used to create an instance of {@link AssetsLoader}. The class should be something like this:
     * <pre>
     *     {@code
     *      // If the directory starts with a point it will be hidden.
     *      public static final AssetsLoader LOADER =  AssetsLoader.newAssetLoader(Main.MOD_ID, ".temp_assets");
     *     }
     * </pre>
     * <br><b>IMPORTANT:</b> only one instance of this class should be created for your mod, unless you have specific requirements to have more, obviously.
     * @param mod_id The mod id of <b>your</b> mod.
     * @param tempDirectory The temporary directory where all the loading processes and loading files will happen.
     */
    @Deprecated(forRemoval = true)
    public AssetsLoader(String mod_id, Path tempDirectory, boolean hideContent) {
        this.loaderModName = mod_id;
        tempDirectoryPath = tempDirectory;
        COPIER.createDirectory(tempDirectoryPath, hideContent);
    }


    //================= STATIC LOADERS - FULL =================
    /* *
     * Loads a pack in-game.
     * <p>
     *     <h3>How to use the method:</h3>
     *     <pre>
     *         {@code
     *          // Inside your main mod class (annotated with @Mod(MyMainClass.MOD_ID))
     *
     *          // You should have the global static AssetsLoader instance.
     *          public static final String MOD_ID = "my_mod_id";
     *          public static final AssetsLoader LOADER =  AssetsLoader.newAssetLoader(Main.MOD_ID, ".temp_assets");
     *
     *          public MyMainClass(IEventBus modEventBus) {
     *              modEventBus.addListener(MyMainClass::addPackFinders);  // Add the subscribed method here.
     *          }
     *
     *          @SubscribeEvent
     *          public static void addPackFinders(AddPackFindersEvent event) {
     *              // Here you need these record instances:
     *              JarLoadingInfos jarLoadingInfos = new JarLoadingInfos(...);
     *              ResourcePackInfos packInfos = new ResourcePackInfos(...);
     *
     *              // Having these classes, now create a PackLoadingContext and pass the previous classes to the constructor.
     *              PackLoadingContext ctx = new PackLoadingContext(LOADER, jarLoadingInfos, packInfos);
     *
     *              // Finally, load your pack.
     *              AssetsLoader.loadPack(event, ctx);
     *          }
     *         }
     *     </pre>
     * </p>
     * @param event The {@link AddPackFindersEvent}.
     * @param context An instance of {@link ResourcePackLoadingContext}.
     * @see JarLoadingInfos
     * @see ResourcePackInfos
     * @see ResourcePackLoadingContext
     * @see net.neoforged.fml.common.Mod
     * @see net.neoforged.bus.api.SubscribeEvent
     * @see AddPackFindersEvent
     */
    @Deprecated(forRemoval = true)
    public void loadPackAssets(AddPackFindersEvent event/*, ResourcePackLoadingContext context*/, String packFolderName, LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile, boolean forceCopyAssets, FilesCopier.LogCopyOption logCopyOption, JarLoadingInfos.JarIconFile jarIconFile, Component packTitle, Component packDescription, boolean requiredPack, boolean deletePack) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        //JarLoadingInfos jarLoadingInfos = context.jarLoadingInfos();
        //ResourcePackInfos packInfos = context.packInfos();
        Path packPath = this.tempDirectoryPath.resolve(packFolderName);

        PL.logCenteredI("Loading pack '" + packFolderName + "' for mod '" + this.loaderModName + "'", DEF_LINE);

        PrettyLogging.StepProcessLogger jarsSPL = PL.new StepProcessLogger("Loading assets from mod jar file:", namespaceCouplesByJarFile.size(), Markers.EXTRACT.marker);
        namespaceCouplesByJarFile.forEach((modJarFile, namespaceCouples) -> {
            jarsSPL.incrementAndLog();
            PrettyLogging.StepProcessLogger coupleSPL = PL.new StepProcessLogger("Mod jar file: '" + modJarFile + "', namespace couple:", namespaceCouples.size(), Markers.EXTRACT.marker);

            Path jarFilePath = MOD_DIR.resolve(modJarFile);
            boolean iconCopied = false;
            for (NamespaceCouple namespaceCouple : namespaceCouples) {
                coupleSPL.incrementAndLog("{" + namespaceCouple.toString() + "}");

                JarFilesCopyContext assetsCtx = new JarFilesCopyContext(
                        jarFilePath,
                        packPath.resolve("assets").resolve(namespaceCouple.newOrSameNamespace()),
                        namespaceCouple.oldNamespace(),
                        PackType.CLIENT_RESOURCES,
                        forceCopyAssets,
                        logCopyOption
                );

                COPIER.copyFilesFromJar(assetsCtx);

                if (jarIconFile.jarFile().equals(modJarFile) && !iconCopied) {
                    iconCopied = true; // Otherwise if more namespace couples in same jar we copy the icon every time.
                    JarIconCopyContext iconCtx = new JarIconCopyContext(
                            jarFilePath,
                            jarIconFile.iconFileName(),
                            packPath,
                            "pack"
                    );

                    COPIER.copyJarIcon(iconCtx);
                }
            }
        });

        PL.logI("Adding resourcepack with internal id '" + packFolderName + "' to game packs.", Markers.LOAD.marker);
        event.addRepositorySource(FilesCopier.createPackRepositorySource(packFolderName, packTitle, packDescription, packPath, requiredPack));

        if (deletePack) {
            GameClosedTask.create(() -> COPIER.deleteDirectory(packPath, true), true, "Pack '" + packFolderName + "' deletion.");
        }

        PL.logCenteredI("Pack '" + packFolderName + "' loading complete!", DEF_LINE);
    }
}