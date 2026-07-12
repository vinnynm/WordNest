import json
import os
import sys

def main():
    if len(sys.argv) > 1:
        path = sys.argv[1]
    else:
        path = input("Enter path to wordnet_library.json: ").strip().strip('"')

    if not os.path.isfile(path):
        print(f"File not found: {path}")
        input("Press Enter to close...")
        return

    out_path = os.path.splitext(path)[0] + "_coverage.txt"

    lines = []
    lines.append(f"Coverage report for: {path}")
    lines.append("=" * 40)

    try:
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        lines.append(f"ERROR reading/parsing file: {e}")
        text = "\n".join(lines)
        print(text)
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(text + "\n")
        print(f"\nWrote error log to: {out_path}")
        input("Press Enter to close...")
        return

    by_len = {}
    for letter, words in data.items():
        if not isinstance(words, list):
            continue
        for w in words:
            if not isinstance(w, str):
                continue
            by_len.setdefault(len(w), 0)
            by_len[len(w)] += 1

    lines.append(f"{'LEN':>4} {'COUNT':>8}")
    for length in sorted(by_len):
        lines.append(f"{length:>4} {by_len[length]:>8}")

    total = sum(by_len.values())
    lines.append("")
    lines.append(f"Total words: {total}")

    lines.append("")
    lines.append("Flagged thin lengths (< 20 words), lengths 2-15:")
    thin_found = False
    for length in range(2, 16):
        count = by_len.get(length, 0)
        if count < 20:
            thin_found = True
            lines.append(f"  length {length}: only {count} words")
    if not thin_found:
        lines.append("  (none flagged)")

    text = "\n".join(lines)
    print(text)

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(text + "\n")
    print(f"\nWrote coverage report to: {out_path}")

    input("\nFinished. Press Enter to close this window...")


if __name__ == "__main__":
    main()