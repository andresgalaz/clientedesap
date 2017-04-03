package prg.glz.cli.sync;

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Traducido y Modificado por: Andrés Galaz
 * Compustrom AR-CL
 */

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import prg.glz.FrameworkException;
import prg.glz.cli.frm.PnParams;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class WatchDir implements Runnable {
    private static Logger             logger = Logger.getLogger( WatchDir.class );

    private final WatchService        watcher;
    private final Map<WatchKey, Path> keys;
    private final Sincroniza          migraFrm;
    private boolean                   trace  = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public WatchDir(Path dir, Sincroniza migraFrm) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.migraFrm = migraFrm;

        // Recursivo recursive
        // registerAll( dir );
        // No Recursivo
        logger.info( "Buscando " + dir );
        register( dir );
        logger.info( "Terminada la busqueda" );

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register( watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY );
        if (trace) {
            Path prev = keys.get( key );
            if (prev == null) {
                logger.info( "Registro directorio: " + dir );
            } else {
                if (!dir.equals( prev ))
                    logger.info( "Registro directorio, actualiza: " + prev + " -> " + dir );
            }
        }
        keys.put( key, dir );
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree( start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                // if (dir.equals(
                // WatchDir.this.migraFrm.getHsql().getDatabaseDir() ))
                // return FileVisitResult.SKIP_SUBTREE;
                register( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        // Para evitar la doble llamada del evento
        // long lastModif = 0;

        // Corre mientra no este activado booleano 'shutdown'
        for (;;) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (ClosedWatchServiceException x) {
                return;
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get( key );
            if (dir == null) {
                logger.warn( "WatchKey no reconocido!!" );
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast( event );
                Path name = ev.context();
                Path child = dir.resolve( name );
                File fileChild = child.toFile();

                if (kind == ENTRY_MODIFY) {
                    // Se controla que el evento no este duplicado
                    // if (fileChild.lastModified() - lastModif > 100) {
                    // Se llama al servidor WEB para subir el archivo
                    try {
                        this.migraFrm.syncFile( fileChild );
                    } catch (FrameworkException | SQLException e) {
                        logger.error( "No se pudo subir archivo:" + fileChild, e );
                        JOptionPane.showMessageDialog( PnParams.frmPrincipal, e.getMessage() );
                    }
                    // }
                } else if (kind == ENTRY_DELETE) {
                    // No se hace nada, aglunos editores, en ves de modificar,
                    // primero hace un DELETE y después un CREATE
                    System.out.println( "BORRA:" + fileChild.getName() );
                    try {
                        this.migraFrm.deleteFile( fileChild );
                    } catch (FrameworkException | SQLException e) {
                        logger.error( "No se pudo subir archivo:" + fileChild, e );
                        JOptionPane.showMessageDialog( PnParams.frmPrincipal, e.getMessage() );
                    }                    
                } else if (kind == ENTRY_CREATE) {
                    if (fileChild.isFile()) {
                        // Se llama al servidor WEB para subir el archivo
                        try {
                            this.migraFrm.syncFile( fileChild );
                        } catch (FrameworkException | SQLException e) {
                            logger.error( "No se pudo subir archivo:" + fileChild, e );
                            JOptionPane.showMessageDialog( PnParams.frmPrincipal, e.getMessage() );
                        }
                    } else {
                        // Si aparacen directorios, se agregan a los directorios
                        // a seguir
                        try {
                            if (Files.isDirectory( child, NOFOLLOW_LINKS )) {
                                registerAll( child );
                            }
                        } catch (IOException x) {
                            // Se ignora, se mantiene leyendo eventos
                        }
                    }
                }
                // Evita que se invoque 2 veces el mismo evento
                // if (fileChild.length() > 0)
                // lastModif = fileChild.lastModified();
            }

            // Reinicia key y lo saca del conjunto de directorios
            boolean valid = key.reset();
            if (!valid) {
                keys.remove( key );

                // Si todos los directorios son inacsesibles
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * Esta clase es Runnable, esto permite ser utilizado dentro de un hilo (Thread). Esto se hace necesari porque el
     * método proccesEvents tiene un ciclo sin fin
     * </p>
     */
    @Override
    public void run() {
        this.processEvents();
    }

    public Sincroniza getMigraFrm() {
        return migraFrm;
    }

    public void detener() {
        try {
            this.watcher.close();
        } catch (IOException e) {
        }
    }
}
