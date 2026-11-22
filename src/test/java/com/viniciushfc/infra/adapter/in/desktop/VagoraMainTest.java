package com.viniciushfc.infra.adapter.in.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VagoraMainTest {

    @Test
    void janelaSoContaComoAbertaEnquantoOChromeDaTravaEstiverVivo(@TempDir Path profile) throws Exception {
        assertFalse(VagoraMain.windowOpen(profile));

        Files.createSymbolicLink(profile.resolve("SingletonLock"), Path.of("host-" + ProcessHandle.current().pid()));
        assertTrue(VagoraMain.windowOpen(profile));

        Files.delete(profile.resolve("SingletonLock"));
        Files.createSymbolicLink(profile.resolve("SingletonLock"), Path.of("host-" + Integer.MAX_VALUE));
        assertFalse(VagoraMain.windowOpen(profile), "trava que sobrou de um Chrome que já fechou");
    }
}
