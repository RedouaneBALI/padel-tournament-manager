#!/bin/bash
# Clean H2 database files to free up RAM and reset state

echo "🧹 Cleaning H2 database files..."
rm -f ./target/testdb.* 2>/dev/null && echo "✓ H2 files cleaned" || echo "✗ No H2 files found (already clean)"

echo "🗑️  Clearing Maven cache..."
rm -rf ~/.m2/repository/com/h2database 2>/dev/null && echo "✓ Maven H2 cache cleared"

echo "✅ Cleanup complete. Run tests to regenerate fresh DB."

