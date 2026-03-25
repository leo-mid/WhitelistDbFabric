import json

INPUT_FILE = "usercache.json"
OUTPUT_FILE = "player_cache.json"

with open(INPUT_FILE, "r", encoding="utf-8") as f:
    data = json.load(f)

result = {
    entry["name"].lower(): entry["uuid"]
    for entry in data
}

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(result, f, indent=2)

print(f"Converted {len(result)} entries → {OUTPUT_FILE}")
