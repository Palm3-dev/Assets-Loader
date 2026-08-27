package com.palm3.packs_loader.common;

import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.palm3.packs_loader.PacksLoaderMain.GAME_DIR;

/**
 * Base class used to create other more specialized copier classes.
 */
@ParametersAreNonnullByDefault
public abstract class JarCopierBase {
    protected final PrettyLogging pl;
    protected final FilesCopier copier;
    protected final Path modJarPath;
    protected final List<NamespaceCouple> namespaceCouples;
    protected final Path packRootPath;

    protected JarCopierBase(Path modJarFile, List<NamespaceCouple> namespaceCouples, Path packRootPath) {
        this.modJarPath = modJarFile;
        this.namespaceCouples = namespaceCouples;
        this.packRootPath = packRootPath;
        pl = new PrettyLogging(LoggerFactory.getLogger(getClass()), PacksLoaderMain.DEF_PL_PARAMS);
        copier = new FilesCopier(pl);
        pl.conditionalW(namespaceCouples.isEmpty(), "The given namespace couples list is empty, nothing will be copied!");
    }

    /**
     * Builder class for {@link JarCopierBase} and its extending subclasses.
     * @param <C> The class that is extending {@link JarCopierBase}.
     * @param <B> The builder class that is extending {@link JarCopierBuilderBase}.
     */
    public static abstract class JarCopierBuilderBase<C extends JarCopierBase, B extends JarCopierBuilderBase<C, B>> {
        protected Path modJarPath = null;
        protected Path packRoot = null;
        protected List<NamespaceCouple> namespaceCouples = new ArrayList<>();

        /**
         * Constructs an instance of this builder class.
         */
        public JarCopierBuilderBase() {}

        /**
         * Sets the mod jar file from which the files will be copied.
         * @param modJarFile The name of the jar file.
         *                   Should be inside the mods dir (or in the incompatible mods dir, see {@link IncompatibleModsRemover#INCOMPATIBLE_JARS_DIR}) for that.
         * @return this builder instance.
         * @throws IllegalArgumentException If the mod jar isn't found in the {@code mods} directory and isn't found in the {@link IncompatibleModsRemover#INCOMPATIBLE_JARS_DIR}.
         */
        public JarCopierBuilderBase<C, B> modJarFile(String modJarFile) {
            try {
                this.modJarPath = IncompatibleModsRemover.getModJarPath(modJarFile);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Mod jar file '" + modJarFile + "' hasn't been found neither in the 'mods' directory nor in the incompatible mods directory!");
            }
            return this;
        }

        /**
         * Sets the pack root.
         * @param packRoot The path of the pack root.
         *                 The root is the folder that would normally contain the icon, the assets folder and the data folder.
         * @return this builder instance.
         * @throws IllegalArgumentException As defined in {@link FilesCopier#absoluteEndsInGameDirOrThrow(Path)}.
         */
        public JarCopierBuilderBase<C, B> packRoot(Path packRoot) {
            Path absolutePackRoot = packRoot.isAbsolute() ? packRoot : GAME_DIR.resolve(packRoot);
            FilesCopier.absoluteEndsInGameDirOrThrow(absolutePackRoot);
            this.packRoot = absolutePackRoot;
            return this;
        }

        /**
         * Sets the namespace couples to copy to the given list.
         * <br><b>IMPORTANT:</b> if you called {@link #addCouple(NamespaceCouple)} <b>before</b> this method, all the couples you added with
         * that method until now will be erased, since this method reassigns the builder internal list to the given one.
         * @param namespaceCouples The list of {@link NamespaceCouple}.
         * @return this builder instance.
         */
        public JarCopierBuilderBase<C, B> withNamespaceCouples(List<NamespaceCouple> namespaceCouples) {
            this.namespaceCouples = namespaceCouples;
            return this;
        }

        /**
         * Adds the given {@link NamespaceCouple} to the current list.
         * <br><b>NOTE:</b> If you used {@link #withNamespaceCouples(List)} the couple will be added to that list, otherwise to an empty one.
         * @param namespaceCouple The namespace couple.
         * @return this builder instance.
         */
        public JarCopierBuilderBase<C, B> addCouple(NamespaceCouple namespaceCouple) {
            namespaceCouples.add(namespaceCouple);
            return this;
        }

        /**
         * Builds the {@link C} class instance, with {@link C} being a subclass of {@link JarCopierBase}.
         * @return A new {@link C} instance.
         * @throws IllegalArgumentException If one or more of the needed params is not set.
         */
        public abstract @NotNull C build();
    }

    /**
     * Does an action for every namespace couple in this class instance.
     * @param action The action to execute.
     */
    protected void doForEachCouple(Consumer<NamespaceCouple> action) {
        for (NamespaceCouple namespaceCouple : namespaceCouples) {
            action.accept(namespaceCouple);
        }
    }

    /**
     * Copies the files from this instance jar file from this instance specified namespace couples.
     */
    public abstract void copy(boolean forceCopyFile, FilesCopier.LogCopyOption... logCopyOption);

    /**
     * Used to get one log copy option from a vararg.
     * @param logCopyOption The vararg value.
     * @return the first option of the vararg, if the vararg is empty return {@link com.palm3.packs_loader.common.FilesCopier.LogCopyOption#LOG_NONEXISTENT}.
     */
    protected static FilesCopier.LogCopyOption getLogCopyOption(FilesCopier.LogCopyOption... logCopyOption) {
        List<FilesCopier.LogCopyOption> logCopyOptions = Arrays.stream(logCopyOption).toList();
        return logCopyOptions.isEmpty() ? FilesCopier.LogCopyOption.LOG_NONEXISTENT : logCopyOptions.getFirst();
    }

    /**
     * @return The pack repository source to load the currently copied
     * pack in-game in {@link net.neoforged.neoforge.event.AddPackFindersEvent#addRepositorySource(RepositorySource)}.
     */
    public abstract RepositorySource getRepositorySource(Component packTitle, Component packDescription, boolean required, boolean hidden);
}
