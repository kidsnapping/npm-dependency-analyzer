
#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

cd "$ROOT_DIR"
mkdir -p build

printf '%s\n' "Compiling project..."
javac -d build src/*.java
printf '%s\n' "Compilation passed."

write_config() {
    cat > "$TEMP_DIR/config.ini"
}

check_error() {
    expected="$1"
    output=$(java -cp build Main "$TEMP_DIR/config.ini" 2>&1 || true)

    if ! printf '%s' "$output" | grep -Fq "$expected"; then
        printf 'FAIL: expected error: %s\n' "$expected"
        printf '%s\n' "$output"
        exit 1
    fi

    printf 'PASS: %s\n' "$expected"
}

printf '\n%s\n' "Stage 1: configuration tests"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = false
version = 4.17.21
max_depth = 3
CONFIG

java -cp build Main "$TEMP_DIR/config.ini" >/dev/null
printf '%s\n' "PASS: valid configuration"

write_config <<'CONFIG'
[settings]
package_name = invalid name
repository = https://registry.npmjs.org
test_mode = false
version = 4.17.21
max_depth = 3
CONFIG
check_error "invalid npm package name"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = not-a-url
test_mode = false
version = 4.17.21
max_depth = 3
CONFIG
check_error "must be an HTTP or HTTPS URL"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = yes
version = 4.17.21
max_depth = 3
CONFIG
check_error "must be true or false"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = false
version = latest
max_depth = 3
CONFIG
check_error "must use the format X.Y.Z"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = false
version = 4.17.21
max_depth = zero
CONFIG
check_error "must be an integer"

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = false
version = 4.17.21
max_depth = 3
unknown_option = value
CONFIG
check_error "unknown parameter 'unknown_option'"

printf '\n%s\n' "Stage 5: Graphviz tests"

if ! command -v dot >/dev/null 2>&1; then
    printf '%s\n' "FAIL: Graphviz is not installed."
    exit 1
fi

printf '%s\n' "PASS: Graphviz is installed."

test_package_graph() {
    package_name="$1"
    package_version="$2"

    printf '\n%s\n' "Testing package: $package_name"

    write_config <<CONFIG
[settings]
package_name = $package_name
repository = https://registry.npmjs.org
test_mode = false
version = $package_version
max_depth = 3
CONFIG

    if ! java -cp build Main "$TEMP_DIR/config.ini" \
            > "$TEMP_DIR/$package_name.log" 2>&1; then
        printf 'FAIL: could not build graph for %s\n' "$package_name"
        cat "$TEMP_DIR/$package_name.log"
        exit 1
    fi

    if [ ! -s "$package_name.dot" ]; then
        printf 'FAIL: %s.dot was not created\n' "$package_name"
        exit 1
    fi

    if [ ! -s "$package_name.png" ]; then
        printf 'FAIL: %s.png was not created\n' "$package_name"
        exit 1
    fi

    if ! grep -Fq "digraph DependencyGraph" "$package_name.dot"; then
        printf 'FAIL: %s.dot has invalid graph content\n' "$package_name"
        exit 1
    fi

    printf 'PASS: %s.dot created\n' "$package_name"
    printf 'PASS: %s.png created\n' "$package_name"
}

test_package_graph express 4.18.2
test_package_graph chalk 4.1.2
test_package_graph lodash 4.17.21

printf '\n%s\n' "All tests passed."
printf '%s\n' "Generated graph images:"
printf '%s\n' "express.png"
printf '%s\n' "chalk.png"
printf '%s\n' "lodash.png"
