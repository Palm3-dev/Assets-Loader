package com.palm3.assets_loader.base;

import java.util.LinkedHashMap;

@Deprecated
public class NamespacesMap {
    private final LinkedHashMap<String, String> newByOld;
    private final LinkedHashMap<String, String> oldByNew;

    public NamespacesMap() {
        newByOld = new LinkedHashMap<>();
        oldByNew = new LinkedHashMap<>();
    }

    public String getOld(String newNamespace) {
        return oldByNew.get(newNamespace);
    }

    public String getNew(String oldNamespace) {
        return newByOld.get(oldNamespace);
    }

    public String getFirstOld() {
        return newByOld.isEmpty() ? null : newByOld.sequencedKeySet().getFirst();
    }

    public String getFirstNew() {
        return oldByNew.isEmpty() ? null : oldByNew.sequencedKeySet().getFirst();
    }    

    public void putNamespaces(String oldNamespace, String newNamespaces) {
        // Inefficient
        /*if (oldByNew.containsValue(aValue)) oldByNew.remove(newByOld.get(aValue));
        if (newByOld.containsValue(bValue)) newByOld.remove(oldByNew.get(bValue));*/

        // Same but more efficient
        if (newByOld.containsKey(oldNamespace))
            oldByNew.remove(getNew(oldNamespace));
        if (oldByNew.containsKey(newNamespaces))
            newByOld.remove(getOld(newNamespaces));

        oldByNew.put(newNamespaces, oldNamespace);
        newByOld.put(oldNamespace, newNamespaces);
    }

    public void clear() {
        oldByNew.clear();
        newByOld.clear();
    }
}
