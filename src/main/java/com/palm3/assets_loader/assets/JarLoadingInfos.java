package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.ModLoader;
import com.palm3.assets_loader.PrettyLogging;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides all the information about the jar files, icon and namespaces needed to load a pack.
 */
public record JarLoadingInfos(LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile, JarLoadingInfos.JarIconFile jarIconFile,
                              boolean forceCopyAssets, AssetsLoader.LogCopyOption logCopyOption) {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);

    /**
     * Defines the jar icon file for a resourcepack.
     * @param iconJarFile  The name of the jar file where the icon is located.
     * @param iconFileName The name of the icon file inside the jar.
     */
    public record JarIconFile(String iconJarFile, String iconFileName) {}

    /**
     * Creates an instance of this record.
     * @param namespaceCouplesByJarFile an {@link HashMap} containing as keys the mod jar files and as values
     *                                  {@link List}s of namespace couples for every jar file.
     * @param jarIconFile An instance of the record {@link JarIconFile} used to know which icon file to use for the pack.
     * @param forceCopyAssets If the assets copy should be forced: this means that if the files are already there they will get overwritten.
     * @param logCopyOption The option for the jar copy logs. See {@link com.palm3.assets_loader.assets.AssetsLoader.LogCopyOption}
     */
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
     * <br><b>NOTE:</b> It's non-indexed since all the couples are not assigned to their jar file, the method returns a simple list of all the couples from all jars.
     * @param logSimilar If similar couples should be logged. Logs if the couple has similar old, new or both namespaces.
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

    /**
     * Creates a map of mod jar files and the namespace couples for those files.
     * @param modJarFiles A list of mod jar files as {@link String}.
     * @param namespaceCouplesList A list of namespace couples, for every jar file.
     *                             <br><b>Remember to keep the same namespaces order:</b>
     *                             <pre>
     *                             {@code
     *                              // Example of correct list params:
     *                              List.of("mod_jar_file_1.jar", "mod_jar_file_2.jar"),  // Mod jar files
     *                              List.of(
     *                                  List.of(  // List of couples for jar 1.
     *                                      new NamespaceCouple("oldNamespace_A_jar_1", "newNamespace_A_jar_1"),
     *                                      new NamespaceCouple("oldNamespace_B_jar_1", "newNamespace_B_jar_1")
     *                                  ),
     *                                  List.of(  // List of couples for jar 2.
     *                                      new NamespaceCouple("oldNamespace_A_jar_2", "newNamespace_A_jar_2"),
     *                                      new NamespaceCouple("oldNamespace_B_jar_2", "newNamespace_B_jar_2")
     *                                  )
     *                              )
     *                             }
     *                             </pre>
     *
     *                             <pre>
     *                             {@code
     *                              // Example of unmatching list params:
     *                              List.of("mod_jar_file_1.jar", "mod_jar_file_2.jar"),  // Mod jar files
     *                              List.of(
     *                                  List.of(  // List of couples for jar 1.
     *                                      new NamespaceCouple("oldNamespace_A_jar_1", "newNamespace_A_jar_2"),  // Unmatching
     *                                      new NamespaceCouple("oldNamespace_B_jar_1", "newNamespace_B_jar_1")
     *                                  ),
     *                                  List.of(  // List of couples for jar 2.
     *                                      new NamespaceCouple("oldNamespace_A_jar_2", "newNamespace_A_jar_1"),  // Unmatching
     *                                      new NamespaceCouple("oldNamespace_B_jar_2", "newNamespace_B_jar_2")
     *                                  )
     *                              )
     *
     *                              // With this the pack will load, but you won't get what you expect. This is the first thing to check if you have unwanted namespaces.
     *                             }
     *                             </pre>
     *
     *                             <br><b>The sizes of the two main lists needs to be the same, or you'll get an {@link IllegalArgumentException}:</b>
     *                             <pre>
     *                             {@code
     *                              // Incorrect:
     *                              List.of("mod_jar_file_1.jar", "mod_jar_file_2.jar"),  // Contains two mod jar files
     *                              List.of(  // Contains three lists.
     *                                  List.of(...),
     *                                  List.of(...),
     *                                  List.of(...)
     *                              )
     *                             }
     *                             </pre>
     * @return A {@link LinkedHashMap} with keys the mod jar files and with values the {@link List}s of {@link NamespaceCouple}s.
     */
    public static LinkedHashMap<String, List<NamespaceCouple>> createJarNamespacesMap(List<String> modJarFiles, List<List<NamespaceCouple>> namespaceCouplesList) {
        if (modJarFiles.size() != namespaceCouplesList.size()) {
            String biggerList = modJarFiles.size() > namespaceCouplesList.size() ? "mod_jar_files" : "namespace_couples_list";
            throw new IllegalArgumentException("The given lists need to be the same size! Bigger list: " + biggerList);
        }
        LinkedHashMap<String, List<NamespaceCouple>> namespaceCouples = new LinkedHashMap<>();
        AtomicInteger couplesIndex = new AtomicInteger();
        modJarFiles.forEach(modJarFile -> namespaceCouples.put(modJarFile, namespaceCouplesList.get(couplesIndex.getAndIncrement())));
        return namespaceCouples;
    }
}
