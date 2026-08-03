package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.PrettyLogging;
import com.palm3.assets_loader.assets.patchers.ModelsChanger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides all the information about the jar files, icon and namespaces needed to load a pack.
 */
public class JarLoadingInfos {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);

    /**
     * Defines the jar icon file for a resourcepack.
     * @param iconJarFile The name of the jar file where the icon is located.
     * @param iconFileName The name of the icon file inside the jar.
     */
    public record JarIconFile(String iconJarFile, String iconFileName) {}

    public final LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile;
    public final JarIconFile jarIconFile;
    public final boolean forceCopyAssets;
    public final @Nullable ModelsChanger.Loader oldObjLoader;
    public final AssetsLoader.LogCopyOption logCopyOption;

    public JarLoadingInfos(LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile, JarIconFile jarIconFile, boolean forceCopyAssets, @Nullable ModelsChanger.Loader oldObjLoader, AssetsLoader.LogCopyOption logCopyOption) {
        if (namespaceCouplesByJarFile.isEmpty()) {
            throw new IllegalArgumentException("Given mod jar files HashMap is empty, nothing to load!");
        }

        namespaceCouplesByJarFile.forEach((jarFile, jarNamespaces) -> {
            if (jarNamespaces.isEmpty())
                throw new IllegalArgumentException("Given namespace couples list at HashMap index (jar file) '" + jarFile + "' is empty, nothing to load!");
        });
        // Now all checked.
        this.namespaceCouplesByJarFile = namespaceCouplesByJarFile;
        this.jarIconFile = jarIconFile;
        this.forceCopyAssets = forceCopyAssets;
        this.oldObjLoader = oldObjLoader;
        this.logCopyOption = logCopyOption;
    }

    public static LinkedHashMap<String, List<NamespaceCouple>> createCouplesList(List<String> modJarFiles, List<List<NamespaceCouple>> namespaceCouplesList) {
        LinkedHashMap<String, List<NamespaceCouple>> namespaceCouples = new LinkedHashMap<>();
        AtomicInteger couplesIndex = new AtomicInteger();
        modJarFiles.forEach(modJarFile -> namespaceCouples.put(modJarFile, namespaceCouplesList.get(couplesIndex.getAndIncrement())));
        return namespaceCouples;
    }

    /**
     * Used to get a list of all the jar files in the class instance.
     * @return A {@link List} of the jar files as {@link String}.
     */
    public List<String> getJarFilesList() {
        List<String> jarFilesList = new ArrayList<>();
        namespaceCouplesByJarFile.forEach((jarFile, namespaceCouples) -> jarFilesList.add(jarFile));
        return jarFilesList;
    }

    public List<NamespaceCouple> getNonIndexedNamespaceCouplesList() {
        List<NamespaceCouple> allNamespaceCouples = new ArrayList<>();
        namespaceCouplesByJarFile.forEach((modJarFile, namespaceCouples) -> {
            allNamespaceCouples.addAll(namespaceCouples);
        });
        return allNamespaceCouples;
    }
}
