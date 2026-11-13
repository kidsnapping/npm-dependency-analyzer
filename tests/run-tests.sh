#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

cd "$ROOT_DIR"
mkdir -p build
javac -d build src/*.java

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

write_config <<'CONFIG'
[settings]
package_name = lodash
repository = https://registry.npmjs.org
test_mode = false
version = 4.17.21
max_depth = 3
CONFIG
java -cp build Main "$TEMP_DIR/config.ini" >/dev/null
printf 'PASS: valid configuration\n'

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

printf 'All stage 1 tests passed.\n'
