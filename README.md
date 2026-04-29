# File Downloader (Java)

Parallel HTTP file downloader using byte-range requests.

## What It Does

- Fetches metadata with `HEAD` (`Content-Length`, `Accept-Ranges`)
- Splits the file into non-overlapping byte ranges
- Downloads ranges in parallel with `Range: bytes=start-end`
- Validates `206 Partial Content` and `Content-Range`
- Assembles all chunks into a single output file

## Prerequisites

- Java 21
- Docker

## Quick Start (from fresh clone)

```bash
./gradlew clean test
```

Start local test server (assignment setup):

```bash
docker run --rm -p 8080:80 -v "/path/to/your/local/directory:/usr/local/apache2/htdocs/" httpd:latest
```

Example for this repository (run from repository root, or use an absolute path):

```bash
docker run --rm -p 8080:80 -v "$(pwd):/usr/local/apache2/htdocs/" httpd:latest
```

The repository includes a sample source file at `storage/example-file.txt`.
After starting Docker with the repository mounted as Apache web root, run downloader:

```bash
./gradlew run --args="http://localhost:8080/storage/example-file.txt"
```

## CLI

```text
./gradlew run --args="<url> [--output <path>] [--threads <n>] [--chunk-size <bytes>]"
```

Defaults:

- `--output storage/downloaded.bin`
- `--threads 4`
- `--chunk-size 4096`

Example with explicit options:

```bash
./gradlew run --args="http://localhost:8080/storage/example-file.txt --output storage/out.bin --threads 8 --chunk-size 8192"
```

## Expected Output

On success, the app prints:

- URL and HTTP metadata
- Output file path
- Expected byte size and written byte size

Default output file:

- `storage/downloaded.bin`

## Verification

Compare downloaded output with the source file:

```bash
wc -c storage/example-file.txt storage/downloaded.bin
shasum -a 256 storage/example-file.txt storage/downloaded.bin
cmp storage/example-file.txt storage/downloaded.bin && echo "IDENTICAL"
```

Matching checksum and successful `cmp` confirm correctness.

## Notes

- Parallel mode requires `Accept-Ranges: bytes` and a valid positive `Content-Length`.
- Generated binary outputs are ignored by git (`.gitignore`).