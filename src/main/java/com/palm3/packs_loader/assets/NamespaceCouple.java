package com.palm3.packs_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.logging.PrettyLogging;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public record NamespaceCouple(String oldNamespace, String newOrSameNamespace) {

    public static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), PacksLoaderMain.DEF_PL_PARAMS);

    /**
     * Used to create a couple of related namespaces.
     * <br><b>IMPORTANT:</b> remember to not use uppercase letters, accepted chars are: [a-z0-9_.-]
     *
     * @param oldNamespace The old namespace occurring in the resourcepack files.
     * @param newOrSameNamespace The new namespace that will replace the old one (if not the same); also the name of the directory
     *                           inside the {@code assets} folder containing all the files (blockstates, models...).
     *                           Can be the same as the old one (thus the old one won't be changed).
     */
    public NamespaceCouple {
        if (oldNamespace.chars().anyMatch(Character::isUpperCase)) {
            PL.logW("OLD namespace '" + oldNamespace + "' contains uppercase chars that have been made lowercase.");
            oldNamespace = oldNamespace.toLowerCase();
        }

        if (newOrSameNamespace.chars().anyMatch(Character::isUpperCase)) {
            PL.logW("NEW namespace '" + newOrSameNamespace + "' contains uppercase chars that have been made lowercase.");
            newOrSameNamespace = newOrSameNamespace.toLowerCase();
        }

        if (!oldNamespace.matches("^[a-z0-9._-]+$")) {
            throw new IllegalCharsetNameException("OLD namespace '" + oldNamespace + "' contains non [a-z0-9._-] chars!");
        }
        if (!newOrSameNamespace.matches("^[a-z0-9._-]+$")) {
            throw new IllegalCharsetNameException("NEW namespace '" + newOrSameNamespace + "' contains non [a-z0-9._-] chars!");
        }
    }

    /**
     * Creates a list of multiple {@link NamespaceCouple}. Uppercase will be replaced with lowercase.
     * <p>
     * <b>NOTE:</b> Pay attention while using this method.
     * The general order you put the namespaces in the lists is not important; what is important is that you
     * respect the same element number you have chosen in both lists, otherwise you'll get errors.
     * <pre>
     *     {@code
     *      //====== Examples of lists ======
     *
     *      // Accepted, order (A, B...) is not important.
     *      List.of("oldNamespace_B", "oldNamespace_A"),
     *      List.of("newNamespace_B", "newNamespace_A")
     *
     *      // Wrong, will throw an IllegalArgumentException.
     *      List.of("oldNamespace_A", "oldNamespace_B"),
     *      List.of("newNamespace_A", "newNamespace_B", "newNamespace_C")
     *
     *      // Caps is used as example, don't use it, will get replaced with lowercase anyway.
     *     }
     * </pre>
     * </p>
     * @param oldNamespaces A list of the old namespaces.
     * @param newNamespaces A list of the new namespaces. Remember to match the namespaces order of the previous list, or you'll get namespaces
     *                      that don't match expectations.
     *                      <pre>
     *                      {@code
     *                      // Example of unmatched namespaces:
     *                      List.of("oldNamespace_A", "oldNamespace_B"),
     *                      List.of("newNamespace_B", "newNamespace_A")
     *                      // It works, but it's not what you expect.
     *                      }
     *                      </pre>
     * @return The {@link List} of namespaces couples.
     * @throws IllegalArgumentException If the lists are different in dimension.
     */
    public static List<NamespaceCouple> createMultipleCouplesList(List<String> oldNamespaces, List<String> newNamespaces) {
        // Check lists or throw
        if (oldNamespaces.size() != newNamespaces.size()) {
            String biggerList = oldNamespaces.size() > newNamespaces.size() ? "old_namespaces" : "new_namespaces";
            throw new IllegalArgumentException("The given lists need to be the same size! Bigger list: " + biggerList);
        }

        int newIndex = 0;
        List<NamespaceCouple> namespacesCouples = new ArrayList<>();
        for (String oldNamespace : oldNamespaces) {
            namespacesCouples.add(new NamespaceCouple(oldNamespace.toLowerCase(), newNamespaces.get(newIndex).toLowerCase()));
            newIndex++;
        }
        return namespacesCouples;
    }

    /**
     * Creates a list of one {@link NamespaceCouple}. Uppercase will be replaced with lowercase.
     * @param oldNamespace The old namespace.
     * @param newNamespace The new namespace.
     * @return The {@link List} of namespace couple.
     */
    public static List<NamespaceCouple> createSingleCoupleList(String oldNamespace, String newNamespace) {
        return List.of(new NamespaceCouple(oldNamespace.toLowerCase(), newNamespace.toLowerCase()));
    }

    /**
     * Creates a list of one {@link NamespaceCouple} that contains the same old and new namespaces. Uppercase will be replaced with lowercase.
     * @param namespace The namespace, old and new one.
     * @return The {@link List} of namespace couple.
     */
    public static List<NamespaceCouple> createSingleCoupleList(String namespace) {
        return List.of(new NamespaceCouple(namespace.toLowerCase(), namespace.toLowerCase()));
    }

    @Override
    public @NotNull String toString() {
        return "OLD: " + oldNamespace + " | NEW: " + newOrSameNamespace;
    }

    /**
     * Used to confront the new namespace values.
     * @param namespaceCouple The namespace couple to confront this one to.
     * @return {@code true} if the new namespaces match.
     */
    public boolean sameNewOf(NamespaceCouple namespaceCouple) {
        return namespaceCouple.newOrSameNamespace.equals(newOrSameNamespace);
    }

    /**
     * Used to confront the old namespace values.
     * @param namespaceCouple The namespace couple to confront this one to.
     * @return {@code true} if the old namespaces match.
     */
    public boolean sameOldOf(NamespaceCouple namespaceCouple) {
        return namespaceCouple.oldNamespace.equals(oldNamespace);
    }
}