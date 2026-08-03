package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.PrettyLogging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Provides all the information about the jar files, icon and namespaces needed to load a pack.
 */
public class JarLoadingInfos {
    public static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);

    /**
     * Defines the jar icon file for a resourcepack.
     * @param iconJarFile The name of the jar file where the icon is located.
     * @param iconFileName The name of the icon file inside the jar.
     */
    public record JarIconFile(String iconJarFile, String iconFileName) {}

    public final boolean isSingleJar;
    public final LinkedHashMap<String, Boolean> jarLoadsSingleNamespace = new LinkedHashMap<>();
    public final LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile;
    public final JarIconFile jarIconFile;

    protected JarLoadingInfos(LinkedHashMap<String, List<NamespaceCouple>> namespaceCouplesByJarFile, JarIconFile jarIconFile) {
        if (namespaceCouplesByJarFile.isEmpty()) {  // Size 0
            throw new IllegalArgumentException("Given mod jar files HashMap is empty, nothing to load!");
        }

        this.isSingleJar = namespaceCouplesByJarFile.size() == 1;
        namespaceCouplesByJarFile.forEach((jarFile, jarNamespaces) -> {
            if (jarNamespaces.isEmpty()) throw new IllegalArgumentException("Given namespace couples list at HashMap index (jar file) '" + jarFile + "' is empty, nothing to load!");
            this.jarLoadsSingleNamespace.put(jarFile, jarNamespaces.size() == 1);
        });
        // Now all checked.
        this.namespaceCouplesByJarFile = namespaceCouplesByJarFile;
        this.jarIconFile = jarIconFile;
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
}
