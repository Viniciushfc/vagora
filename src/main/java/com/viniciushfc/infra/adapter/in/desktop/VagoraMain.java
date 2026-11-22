package com.viniciushfc.infra.adapter.in.desktop;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@QuarkusMain
public class VagoraMain {

    private static final int FIRST_PORT = Integer.getInteger("quarkus.http.port", 7777);
    private static final String OS = System.getProperty("os.name");

    public static void main(String... args) throws Exception {
        if (!"portable".equals(System.getProperty("quarkus.profile"))) {
            Quarkus.run(args);
            return;
        }
        Path data = Path.of(System.getProperty("vagora.data", "dados")).toAbsolutePath().normalize();
        if (data.toString().contains("/AppTranslocation/")) {
            alert("O macOS abriu o Vagora numa pasta temporária. Mova a pasta do Vagora para Documentos ou Aplicativos e abra de novo.");
            return;
        }
        try {
            Files.createDirectories(data);
        } catch (IOException e) {
            alert("O Vagora não consegue gravar em " + data + ". Mova a pasta do Vagora para Documentos ou para a Área de Trabalho.");
            return;
        }
        System.setProperty("vagora.data", data.toString().replace('\\', '/'));

        int port = FIRST_PORT;
        while (inUse(port)) {
            if (isVagora(port)) {
                openWindow(port, data);
                return;
            }
            port++;
        }
        System.setProperty("quarkus.http.port", String.valueOf(port));
        Quarkus.run(Window.class, args);
    }

    public static class Window implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            int port = ConfigProvider.getConfig().getValue("quarkus.http.port", Integer.class);
            Path data = Path.of(System.getProperty("vagora.data"));
            Optional<Process> window = openWindow(port, data);
            if (window.isEmpty()) {
                Quarkus.waitForExit();
                return 0;
            }
            window.get().waitFor();
            while (windowOpen(data.resolve("janela"))) {
                Thread.sleep(2000);
            }
            return 0;
        }
    }

    static boolean windowOpen(Path profile) throws IOException {
        Path lock = profile.resolve("SingletonLock");
        if (Files.isSymbolicLink(lock)) {
            String owner = Files.readSymbolicLink(lock).toString();
            long pid = Long.parseLong(owner.substring(owner.lastIndexOf('-') + 1));
            return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        }
        try {
            Files.deleteIfExists(profile.resolve("lockfile"));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    static Optional<Process> openWindow(int port, Path data) throws IOException {
        String url = "http://localhost:" + port;
        Optional<String> browser = appBrowser();
        if (browser.isEmpty()) {
            Desktop.getDesktop().browse(URI.create(url));
            return Optional.empty();
        }
        return Optional.of(new ProcessBuilder(browser.get(), "--app=" + url, "--user-data-dir=" + data.resolve("janela"),
                "--no-first-run", "--no-default-browser-check", "--window-size=1440,900")
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start());
    }

    static Optional<String> appBrowser() {
        Stream<String> candidates;
        if (OS.startsWith("Windows")) {
            candidates = Stream.of(System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), System.getenv("LOCALAPPDATA"))
                    .filter(dir -> dir != null)
                    .flatMap(dir -> Stream.of(dir + "\\Google\\Chrome\\Application\\chrome.exe", dir + "\\Microsoft\\Edge\\Application\\msedge.exe"));
        } else if (OS.startsWith("Mac")) {
            candidates = Stream.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
        } else {
            candidates = Stream.of(System.getenv().getOrDefault("PATH", "").split(File.pathSeparator))
                    .flatMap(dir -> Stream.of("google-chrome", "google-chrome-stable", "microsoft-edge", "chromium", "chromium-browser")
                            .map(name -> dir + File.separator + name));
        }
        return candidates.filter(path -> new File(path).canExecute()).findFirst();
    }

    static boolean inUse(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean isVagora(int port) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).timeout(Duration.ofSeconds(2)).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body().contains("<title>Vagora</title>");
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void alert(String message) {
        JOptionPane.showMessageDialog(null, message, "Vagora", JOptionPane.WARNING_MESSAGE);
    }
}
