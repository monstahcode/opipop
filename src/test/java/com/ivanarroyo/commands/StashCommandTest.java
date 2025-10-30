package com.ivanarroyo.commands;

import com.ivanarroyo.core.Index;
import com.ivanarroyo.core.ObjectStore;
import com.ivanarroyo.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StashCommandTest {

    @TempDir
    File tempDir;

    private File workingDir;
    private ObjectStore store;
    private StashCommand stashCommand;

    @BeforeEach
    void setUp() throws Exception {
        // Directorio temporal para el test
        workingDir = Files.createTempDirectory("testRepo").toFile();

        // IMPORTANTE: fijar user.dir al workingDir
        System.setProperty("user.dir", workingDir.getAbsolutePath());

        // Crear repositorio .opipop
        File repoDir = new File(workingDir, ".opipop");
        repoDir.mkdirs();

        store = new ObjectStore(repoDir.getAbsolutePath());
        store.getObjectsDir().mkdirs();
        store.getRefsDir().mkdirs();
        new File(store.getRefsDir(), "heads").mkdirs();
        Files.writeString(store.getHeadFile().toPath(), "ref: refs/heads/main");

        // Crear un archivo inicial "file1.txt"
        File file1 = new File(workingDir, "file1.txt");
        Files.writeString(file1.toPath(), "original");

        // Añadir al índice con hash de su contenido original
        Index index = new Index(store.getIndexFile());
        String originalHash = HashUtils.sha1("original".getBytes());
        index.add("file1.txt", originalHash);
        index.save();

        // Guardar el objeto original en ObjectStore
        File objectFile = store.getObjectFile(originalHash);
        objectFile.getParentFile().mkdirs();
        Files.write(objectFile.toPath(), "original".getBytes());

        // Crear comando de stash
        stashCommand = new StashCommand(store);
    }

    @Test
    void testStashAndPop() throws Exception {
        File file1 = new File(workingDir, "file1.txt");

        // Modificar el archivo
        Files.writeString(file1.toPath(), "modificado");

        // Guardar stash
        stashCommand.execute(new String[]{});

        // Archivo vuelve a estado original
        assertEquals("original", Files.readString(file1.toPath()));

        // Stash existe
        assertTrue(store.getStashFile().exists());

        // Modificar de nuevo para pop
        Files.writeString(file1.toPath(), "cambio2");

        // Hacer pop
        stashCommand.execute(new String[]{"pop"});

        // Archivo restaurado al estado guardado en stash
        assertEquals("modificado", Files.readString(file1.toPath()));

        // Stash vacío después de pop
        stashCommand.execute(new String[]{"pop"}); // no debe fallar
    }


    @Test
    void testListStashAndClear() throws Exception {
        // Crear archivo y añadir al índice
        File file2 = new File(workingDir, "file2.txt");
        Files.writeString(file2.toPath(), "contenido");
        Index index = new Index(store.getIndexFile());
        String hash = HashUtils.sha1("contenido".getBytes());
        index.add("file2.txt", hash);
        index.save();

        // Modificar archivo
        Files.writeString(file2.toPath(), "modificado");

        // Guardar stash
        stashCommand.execute(new String[]{});

        // List debe mostrar algo
        stashCommand.execute(new String[]{"list"});

        // Clear
        stashCommand.execute(new String[]{"clear"});
        assertFalse(store.getStashFile().exists());
    }

    @Test
    void testNoChangesToStash() throws Exception {
        // Crear archivo y añadir al índice
        File file3 = new File(workingDir, "file3.txt");
        Files.writeString(file3.toPath(), "contenido");
        Index index = new Index(store.getIndexFile());
        String hash = HashUtils.sha1("contenido".getBytes());
        index.add("file3.txt", hash);
        index.save();

        // Ejecutar stash sin cambios
        stashCommand.execute(new String[]{});
        // Mensaje esperado: "No changes to stash"
        assertFalse(store.getStashFile().exists());
    }
}
