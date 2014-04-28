# Usage

Both scripts contain usage/help information that can be obtained via
the `--help` flag.

## Verify integrity

This script verifies that the properties of the files on disk (location, size,
and SHA-1 hash) are equivalent to those returned by Marlin.

### Example usage

    # Specify the endpoint and root directory
    marlin/support$ ./verify-integrity.rb -e http://localhost:3000 -r /tmp/marlin

## Verify consistency

This script verifies that a collection of Marlin instances all contain
the same data (entries, file sizes, and hashes).

### Example usage

    # Use an endpoint
    marlin/support$ ./verify-consistency.rb http://localhost:3000
    # Or a file with containing a newline-delimited list of endpoints
    marlin/support$ ./verify-consistency.rb -f list_of_endpoints.txt

