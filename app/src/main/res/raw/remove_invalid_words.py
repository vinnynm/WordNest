"""
remove_invalid_words.py

Removes any word from a WordNest dictionary JSON (e.g. large_wordlib.json)
that is NOT a valid, official Scrabble word — i.e. keeps only words that
appear in the official TWL06 (Tournament Word List) reference dictionary.

Since there's no reliable way to hardcode a ~180,000-word official list in
a script, this downloads the reference list once from a public GitHub
mirror of TWL06 and caches it locally (twl06_reference.txt) so future runs
don't need the internet.

    Reference source: cviebrock/wordlists (TWL06.txt, 178,691 words)
    https://github.com/cviebrock/wordlists

If you already have your own reference word list file (one word per line),
pass it with --reference and the script will use that instead of
downloading anything.

Input JSON is expected in the same letter->word-list shape used across
WordNest's raw resources:
    { "A": ["APPLE", "ANT", ...], "B": [...], ... }

Usage:
    python remove_invalid_words.py large_wordlib.json large_wordlib_clean.json
    python remove_invalid_words.py in.json out.json --reference my_twl.txt
    python remove_invalid_words.py            (fully interactive)

Logs everything (console + a log file next to the output) and does NOT
auto-exit when finished — it waits for Enter so you can read the summary.
"""

import argparse
import json
import logging
import os
import sys
import urllib.request
import urllib.error
from datetime import datetime

TWL06_URL = "https://raw.githubusercontent.com/cviebrock/wordlists/master/TWL06.txt"
LOCAL_CACHE_NAME = "twl06_reference.txt"

# ──────────────────────────────────────────────────────────────────────────
# Logging setup
# ──────────────────────────────────────────────────────────────────────────

def setup_logging(log_path: str) -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler(log_path, encoding="utf-8"),
        ],
    )


def prompt_for_path(message: str, must_exist: bool) -> str:
    while True:
        path = input(message).strip().strip('"')
        if not path:
            print("Please enter a path.")
            continue
        if must_exist and not os.path.isfile(path):
            print(f"File not found: {path}")
            continue
        return path


# ──────────────────────────────────────────────────────────────────────────
# Reference word list
# ──────────────────────────────────────────────────────────────────────────

def download_reference_list(cache_path: str) -> set:
    logging.info(f"Downloading official TWL06 reference list from: {TWL06_URL}")
    try:
        req = urllib.request.Request(TWL06_URL, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8", errors="ignore")
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as e:
        logging.error(f"Download failed: {e}")
        raise

    words = {line.strip().upper() for line in raw.splitlines() if line.strip()}
    logging.info(f"Downloaded {len(words)} reference words")

    with open(cache_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sorted(words)))
    logging.info(f"Cached reference list to: {cache_path}")

    return words


def load_reference_list_from_file(path: str) -> set:
    logging.info(f"Loading reference word list from: {path}")
    with open(path, "r", encoding="utf-8") as f:
        words = {line.strip().upper() for line in f if line.strip() and not line.startswith("#")}
    logging.info(f"Loaded {len(words)} reference words")
    return words


def get_reference_words(explicit_path: str | None, script_dir: str) -> set:
    if explicit_path:
        if not os.path.isfile(explicit_path):
            raise FileNotFoundError(f"--reference file not found: {explicit_path}")
        return load_reference_list_from_file(explicit_path)

    cache_path = os.path.join(script_dir, LOCAL_CACHE_NAME)
    if os.path.isfile(cache_path):
        logging.info(f"Found cached reference list: {cache_path}")
        use_cache = input("Use cached reference list instead of re-downloading? [Y/n]: ").strip().lower()
        if use_cache in ("", "y", "yes"):
            return load_reference_list_from_file(cache_path)

    return download_reference_list(cache_path)


# ──────────────────────────────────────────────────────────────────────────
# Core filtering
# ──────────────────────────────────────────────────────────────────────────

def load_json(path: str) -> dict:
    logging.info(f"Reading input JSON: {path}")
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        raise ValueError("Expected top-level JSON object of letter -> [words]")
    return data


def remove_invalid_words(data: dict, valid_words: set) -> tuple:
    """
    Returns (filtered_data, removed_count, kept_count).
    A word is kept only if its uppercase form is in valid_words.
    """
    filtered = {}
    removed = 0
    kept = 0

    for key, words in data.items():
        if not isinstance(words, list):
            logging.warning(f"Key '{key}' does not map to a list — copying as-is")
            filtered[key] = words
            continue

        new_words = []
        for word in words:
            if not isinstance(word, str):
                new_words.append(word)
                continue
            if word.upper() in valid_words:
                new_words.append(word)
                kept += 1
            else:
                removed += 1
                logging.info(f"Removing invalid word: {word} (key '{key}')")
        filtered[key] = new_words

    return filtered, removed, kept


def save_json(data: dict, path: str) -> None:
    logging.info(f"Writing filtered JSON to: {path}")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("input", nargs="?", help="Path to source JSON file")
    parser.add_argument("output", nargs="?", help="Path to write filtered JSON file")
    parser.add_argument("--reference", dest="reference", default=None,
                         help="Path to your own reference word list (skips downloading)")
    args, _ = parser.parse_known_args()

    print("=" * 70)
    print(" WordNest — Remove Invalid (Non-Scrabble) Words")
    print("=" * 70)

    # ── Resolve input path ──────────────────────────────────────────────
    input_path = args.input
    if not input_path or not os.path.isfile(input_path):
        if input_path:
            print(f"Input file not found: {input_path}")
        input_path = prompt_for_path("Enter path to source JSON file: ", must_exist=True)

    # ── Resolve output path ─────────────────────────────────────────────
    output_path = args.output
    if not output_path:
        default_output = os.path.join(
            os.path.dirname(os.path.abspath(input_path)),
            os.path.splitext(os.path.basename(input_path))[0] + "_valid_only.json",
        )
        output_path = input(f"Enter path for new output file [{default_output}]: ").strip().strip('"')
        if not output_path:
            output_path = default_output

    if os.path.abspath(input_path) == os.path.abspath(output_path):
        print("Output file must be different from the input file. Aborting.")
        input("\nPress Enter to exit...")
        return

    log_path = os.path.splitext(output_path)[0] + "_log.txt"
    setup_logging(log_path)

    logging.info("Starting invalid-word removal run")
    logging.info(f"Timestamp: {datetime.now().isoformat()}")
    logging.info(f"Input file: {input_path}")
    logging.info(f"Output file: {output_path}")
    logging.info(f"Log file: {log_path}")

    script_dir = os.path.dirname(os.path.abspath(__file__))

    try:
        valid_words = get_reference_words(args.reference, script_dir)
        if not valid_words:
            raise ValueError("Reference word list is empty — aborting.")

        data = load_json(input_path)
        filtered_data, removed, kept = remove_invalid_words(data, valid_words)
        save_json(filtered_data, output_path)

        total_before = sum(len(v) for v in data.values() if isinstance(v, list))
        total_after = sum(len(v) for v in filtered_data.values() if isinstance(v, list))

        logging.info("-" * 60)
        logging.info("SUMMARY")
        logging.info(f"  Reference list size: {len(valid_words)} words")
        logging.info(f"  Total words before:  {total_before}")
        logging.info(f"  Total words after:   {total_after}")
        logging.info(f"  Words kept (valid):     {kept}")
        logging.info(f"  Words removed (invalid): {removed}")
        logging.info("-" * 60)
        logging.info("Done. Output written successfully.")

    except Exception as e:
        logging.exception(f"Run failed with an error: {e}")

    # Do NOT auto-exit — keep the console open so logs stay visible.
    print()
    input("Finished. Press Enter to close this window...")


if __name__ == "__main__":
    main()
