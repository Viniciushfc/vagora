package com.viniciushfc.infra.adapter.out.claude;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public final class ClaudeCli {

    public record Result(int exitCode, String output) {
    }

    private static final File WORKDIR = createWorkdir();
    private static final List<String> NAMES = System.getProperty("os.name").startsWith("Windows")
            ? List.of("claude.exe", "claude.cmd") : List.of("claude");

    private ClaudeCli() {
    }

    public static String executable() {
        String path = System.getenv().getOrDefault("PATH", "");
        return Stream.concat(Arrays.stream(path.split(File.pathSeparator)),
                        Stream.of(System.getProperty("user.home") + "/.local/bin"))
                .filter(dir -> !dir.isBlank())
                .flatMap(dir -> NAMES.stream().map(name -> Path.of(dir, name)))
                .filter(Files::isExecutable)
                .findFirst()
                .map(Path::toString)
                .orElse(null);
    }

    public static Process start(List<String> command) throws IOException {
        return new ProcessBuilder(command)
                .directory(WORKDIR)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
    }

    public static Result run(List<String> command, String stdin, Duration timeout)
            throws IOException, InterruptedException, TimeoutException {
        Process process = start(command);
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try (var in = process.getOutputStream()) {
            if (stdin != null) {
                in.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        }
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new TimeoutException();
        }
        return new Result(process.exitValue(), output.join());
    }

    private static File createWorkdir() {
        try {
            return Files.createTempDirectory("vagora-claude").toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta temporária do Claude", e);
        }
    }
}
