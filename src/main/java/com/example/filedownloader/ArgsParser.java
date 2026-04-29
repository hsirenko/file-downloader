package com.example.filedownloader;

import java.net.URI;
import java.nio.file.Path;

final class ArgsParser {

    DownloaderConfig parse(final String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException(usage());
        }

        if (hasFlag(args, "--help")) {
            throw new IllegalArgumentException(usage());
        }

        final String url = args[0];
        final URI uri = new UriParser().parseRequiredHttpUrl(new String[]{url});

        final Path output = Path.of(getOptionValue(args, "--output", "storage/downloaded.bin"));
        final int threads = parsePositiveInt(
                getOptionValue(args, "--threads", "4"),
                "--threads"
        );
        final int chunkSize = parsePositiveInt(
                getOptionValue(args, "--chunk-size", "4096"),
                "--chunk-size"
        );

        return new DownloaderConfig(uri, output, threads, chunkSize);
    }

    private static int parsePositiveInt(final String rawValue, final String optionName) {
        final int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(optionName + " must be a positive integer", e);
        }
        if (value <= 0) {
            throw new IllegalArgumentException(optionName + " must be > 0");
        }
        return value;
    }

    private static boolean hasFlag(final String[] args, final String flag) {
        for (final String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String getOptionValue(final String[] args, final String option, final String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (option.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static String usage() {
        return """
                Usage: ./gradlew run --args="<url> [--output <path>] [--threads <n>] [--chunk-size <bytes>]"
                
                Example:
                  ./gradlew run --args="http://localhost:8080/storage/example-file.txt --output storage/out.bin --threads 4 --chunk-size 4096"
                """;
    }
}