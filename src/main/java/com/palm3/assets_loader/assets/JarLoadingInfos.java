package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.PrettyLogging;
import com.palm3.assets_loader.assets.patchers.ModelsChanger;

import javax.annotation.Nullable;
import java.util.*;
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
     * <br>It's non-indexed since all the couples are not assigned to their jar file, the methods returns a simple list of all the couples from all jars.
     * @param logSimilar If similar couples should be logged. Logs similar old, new and both namespaces.
     * @return A {@link List} of {@link NamespaceCouple}.
     */
    public List<NamespaceCouple> getNonIndexedNamespaceCouplesList(boolean logSimilar) {
        List<NamespaceCouple> allNamespaceCouples = new ArrayList<>();
        namespaceCouplesByJarFile.forEach((modJarFile, namespaceCouples) -> {
            allNamespaceCouples.addAll(namespaceCouples);
        });

        if (logSimilar) {
            int size = allNamespaceCouples.size();
            for (int i = 0; i < size; i++) {
                NamespaceCouple namespaceCouple = allNamespaceCouples.get(i);
                for (int j = i + 1; j < size; j++) {
                    NamespaceCouple namespaceCouple1 = allNamespaceCouples.get(j);
                    if (namespaceCouple1.sameNewOf(namespaceCouple))
                        PL.logW("Namespace couple {" + namespaceCouple1 + "} has same NEW_NAMESPACE as {" + namespaceCouple + "}");
                    if (namespaceCouple1.sameOldOf(namespaceCouple))
                        PL.logW("Namespace couple {" + namespaceCouple1 + "} has same OLD_NAMESPACE as {" + namespaceCouple + "}");
                    if (namespaceCouple1.equals(namespaceCouple))
                        PL.logW("Namespace couple {" + namespaceCouple1 + "} IS THE SAME as {" + namespaceCouple + "}");
                }
            }
        }

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
