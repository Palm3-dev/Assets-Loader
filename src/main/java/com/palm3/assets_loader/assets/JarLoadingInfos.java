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
public record JarLoadingInfos(LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile, JarLoadingInfos.JarIconFile jarIconFile,
                              boolean forceCopyAssets, @Nullable ModelsChanger.Loader oldObjLoader, AssetsLoader.LogCopyOption logCopyOption) {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);

    /**
     * Defines the jar icon file for a resourcepack.
     * @param iconJarFile  The name of the jar file where the icon is located.
     * @param iconFileName The name of the icon file inside the jar.
     */
    public record JarIconFile(String iconJarFile, String iconFileName) {}

    public JarLoadingInfos {
        if (namespaceCouplesByJarFile.isEmpty()) {
            throw new IllegalArgumentException("Given mod jar files HashMap is empty, nothing to load!");
        }

        namespaceCouplesByJarFile.forEach((jarFile, jarNamespaces) -> {
            if (jarNamespaces.isEmpty())
                throw new IllegalArgumentException("Given namespace couples list at HashMap index (jar file) '" + jarFile + "' is empty, nothing to load!");
        });
    }

    /**
     * Used to get a list of all the jar files in the record.
     * @return A {@link List} of the jar files as {@link String}.
     */
    public List<String> getJarFilesList() {
        List<String> jarFilesList = new ArrayList<>();
        namespaceCouplesByJarFile.forEach((jarFile, namespaceCouples) -> jarFilesList.add(jarFile));
        return jarFilesList;
    }

    /**
     * Used to get all the namespace couples in the record.
     * <br>It's non-indexed since all the couples are not assigned their jar file, the methods returns a simple list of all the couples.
     * @return A {@link List} of {@link NamespaceCouple}.
     */
    public List<NamespaceCouple> getNonIndexedNamespaceCouplesList() {
        List<NamespaceCouple> allNamespaceCouples = new ArrayList<>();
        namespaceCouplesByJarFile.forEach((modJarFile, namespaceCouples) -> {
            allNamespaceCouples.addAll(namespaceCouples);
        });
        return allNamespaceCouples;
    }

    //todo check cause idk if fine, can be better and add variations for single namespace
    public static LinkedHashMap<String, List<NamespaceCouple>> createCouplesList(List<String> modJarFiles, List<List<NamespaceCouple>> namespaceCouplesList) {
        LinkedHashMap<String, List<NamespaceCouple>> namespaceCouples = new LinkedHashMap<>();
        AtomicInteger couplesIndex = new AtomicInteger();
        modJarFiles.forEach(modJarFile -> namespaceCouples.put(modJarFile, namespaceCouplesList.get(couplesIndex.getAndIncrement())));
        return namespaceCouples;
    }
}
