# Usage (from backend-java): source scripts/load-env.sh
# Loads NAME=value lines literally. The file is never run as shell code, so & ? ; and spaces are safe.
# Do not wrap values in quotes with this loader.
while IFS= read -r line || [ -n "$line" ]; do
  line="${line%$'\r'}"
  [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] || continue
  export "${line%%=*}=${line#*=}"
done < "${1:-.env.local}"
