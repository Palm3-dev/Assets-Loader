package com.palm3.assets_loader.assets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to create a couple of namespaces that need to be changed.
 *
 * @param oldNamespace The old namespace occurring in the resourcepack files.
 * @param newNamespace The new namespace that will replace the old one; also the name of the directory
 *                     inside the {@code assets} folder containing all the files (blockstates, models...).
 */
public record NamespaceCouple(String oldNamespace, String newNamespace) {
    /**
     * Creates a list of multiple {@link NamespaceCouple}.
     * <p>
     * <b>NOTE:</b> Pay attention while using this method.
     * The general order you put the namespaces in the lists is not important; what is important is that you
     * respect the same element number you have chosen in both lists, otherwise you'll get errors.
     * <br>Examples of lists:
     * <pre>
     *     {@code
     *      // Accepted, order (A, B...) is not important.
     *      List.of("oldNamespace_B", "oldNamespace_A"),
     *      List.of("newNamespace_B", "newNamespace_A")
     *
     *      // Wrong, will throw an IllegalArgumentException.
     *      List.of("oldNamespace_A", "oldNamespace_B"),
     *      List.of("newNamespace_A", "newNamespace_B", "newNamespace_C")
     *     }
     * </pre>
     * </p>
     * @param oldNamespaces A list of the old namespaces.
     * @param newNamespaces A list of the new namespaces. Remember to match the namespaces order of the previous list, or you'll get namespaces
     *                      that don't match expectations.
     * @return The {@link List} of namespaces couples.
     * @throws IllegalArgumentException If the lists are different in dimension.
     */
    @ParametersAreNonnullByDefault
    public static List<NamespaceCouple> createMultipleNamespacesList(List<String> oldNamespaces, List<String> newNamespaces) throws IllegalArgumentException {
        // Check lists or throw
        if (oldNamespaces.size() != newNamespaces.size()) {
            String biggerList = oldNamespaces.size() > newNamespaces.size() ? "old_namespaces" : "new_namespaces";
            throw new IllegalArgumentException("The given lists need to be the same size! Bigger list: " + biggerList);
        }

        int newIndex = 0;
        List<NamespaceCouple> namespacesCouples = new ArrayList<>();
        for (String oldNamespace : oldNamespaces) {
            namespacesCouples.add(new NamespaceCouple(oldNamespace, newNamespaces.get(newIndex)));
            newIndex++;
        }
        return namespacesCouples;
    }

    /**
     * Creates a list of one {@link NamespaceCouple}.
     * @param oldNamespace The old namespace.
     * @param newNamespace The new namespace.
     * @return The {@link List} of namespaces couples.
     */
    @ParametersAreNonnullByDefault
    public static List<NamespaceCouple> createSingleNamespacesList(String oldNamespace, String newNamespace) throws IllegalArgumentException {
        List<NamespaceCouple> namespacesCouples = new ArrayList<>();
        namespacesCouples.add(new NamespaceCouple(oldNamespace, newNamespace));
        return namespacesCouples;
    }
}