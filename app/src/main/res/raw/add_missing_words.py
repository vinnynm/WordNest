"""
add_missing_words.py

Adds words that are MISSING from a WordNest dictionary JSON (e.g.
large_wordlib.json) by comparing it against an official reference word
list — default is SOWPODS / Collins Scrabble Words, the British-English
list used across the UK, Kenya, and most Commonwealth countries.

This is the mirror-image of remove_invalid_words.py:
    - remove_invalid_words.py -> strips words NOT in the reference list
    - add_missing_words.py    -> adds words that ARE in the reference list
                                  but missing from your dictionary

On first run it downloads the reference list and caches it locally
(sowpods_reference.txt) so future runs don't need the internet.

    Default reference source: jesstess/Scrabble (sowpods.txt, full
    Collins Scrabble Words / SOWPODS list)
    https://github.com/jesstess/Scrabble

You can point it at a different reference list (e.g. TWL06 for American
English, or your own curated list) with --reference, which is exactly
the hook you'll want later when you let users pick their dictionary —
just swap which reference file/URL gets used per-user.

New words are added into your JSON's letter buckets by their first
letter (matching the existing wordlib500.json / large_wordlib shape:
{ "A": [...], "B": [...], ... }), sorted alphabetically within each
bucket. If your JSON key casing differs (e.g. lowercase "a" instead of
"A"), the script detects and matches it automatically.

Input/output JSON shape:
    { "A": ["APPLE", "ANT", ...], "B": [...], ... }

Usage:
    python add_missing_words.py large_wordlib.json large_wordlib_gb.json
    python add_missing_words.py in.json out.json --reference my_list.txt
    python add_missing_words.py in.json out.json --min-length 3 --max-length 15
    python add_missing_words.py            (fully interactive)

Logs everything (console + a log file next to the output) and does NOT
auto-exit when finished — it waits for Enter so you can read the summary.
"""

import argparse
import json
import logging
import os
import re
import sys
import urllib.request
import urllib.error
from datetime import datetime

SOWPODS_URL = "https://raw.githubusercontent.com/jesstess/Scrabble/master/scrabble/sowpods.txt"
LOCAL_CACHE_NAME = "sowpods_reference.txt"

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

def download_reference_list(url: str, cache_path: str) -> set:
    logging.info(f"Downloading reference word list from: {url}")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read().decode("utf-8", errors="ignore")
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as e:
        logging.error(f"Download failed: {e}")
        raise

    words = {
        line.strip().upper()
        for line in raw.splitlines()
        if line.strip() and re.fullmatch(r"[A-Za-z]+", line.strip())
    }
    logging.info(f"Downloaded {len(words)} reference words")

    with open(cache_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sorted(words)))
    logging.info(f"Cached reference list to: {cache_path}")

    return words


def load_reference_list_from_file(path: str) -> set:
    logging.info(f"Loading reference word list from: {path}")
    with open(path, "r", encoding="utf-8") as f:
        words = {
            line.strip().upper()
            for line in f
            if line.strip() and not line.startswith("#")
        }
    logging.info(f"Loaded {len(words)} reference words")
    return words


def get_reference_words(explicit_path: str | None, url: str, script_dir: str) -> set:
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

    return download_reference_list(url, cache_path)


# ──────────────────────────────────────────────────────────────────────────
# Core logic
# ──────────────────────────────────────────────────────────────────────────

def load_json(path: str) -> dict:
    logging.info(f"Reading input JSON: {path}")
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        raise ValueError("Expected top-level JSON object of letter -> [words]")
    return data


def detect_key_style(data: dict) -> str:
    """Returns 'upper', 'lower', or 'upper' as a safe default, based on existing keys."""
    for key in data.keys():
        if isinstance(key, str) and len(key) == 1:
            return "lower" if key.islower() else "upper"
    return "upper"


def add_missing_words(
    data: dict,
    reference_words: set,
    min_length: int,
    max_length: int,
) -> tuple:
    """
    Returns (updated_data, added_count, added_detail, skipped_length_count).
    """
    key_style = detect_key_style(data)

    existing_words = set()
    for words in data.values():
        if isinstance(words, list):
            for w in words:
                if isinstance(w, str):
                    existing_words.add(w.upper())

    updated = {k: (list(v) if isinstance(v, list) else v) for k, v in data.items()}
    added_detail = []
    skipped_length = 0

    for word in sorted(reference_words):
        if word in existing_words:
            continue
        if not (min_length <= len(word) <= max_length):
            skipped_length += 1
            continue

        first_letter = word[0]
        key = first_letter.lower() if key_style == "lower" else first_letter.upper()

        if key not in updated:
            updated[key] = []
            logging.info(f"Creating new bucket for letter '{key}' (wasn't present before)")

        # store using the same case convention as the rest of that bucket,
        # defaulting to uppercase (matches wordlib500.json / large_wordlib style)
        stored_word = word.upper()
        updated[key].append(stored_word)
        added_detail.append((key, stored_word))
        logging.info(f"Adding missing word: {stored_word} (key '{key}')")

    # re-sort each bucket alphabetically for tidiness
    for key in updated:
        if isinstance(updated[key], list):
            updated[key] = sorted(updated[key])

    return updated, len(added_detail), added_detail, skipped_length


def save_json(data: dict, path: str) -> None:
    logging.info(f"Writing updated JSON to: {path}")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("input", nargs="?", help="Path to source JSON file")
    parser.add_argument("output", nargs="?", help="Path to write updated JSON file")
    parser.add_argument("--reference", dest="reference", default=None,
                         help="Path to your own reference word list (skips downloading)")
    parser.add_argument("--reference-url", dest="reference_url", default=SOWPODS_URL,
                         help="URL to download the reference list from (default: SOWPODS/Collins)")
    parser.add_argument("--min-length", dest="min_length", type=int, default=2,
                         help="Minimum word length to add (default: 2)")
    parser.add_argument("--max-length", dest="max_length", type=int, default=15,
                         help="Maximum word length to add (default: 15)")
    args, _ = parser.parse_known_args()

    print("=" * 70)
    print(" WordNest — Add Missing Words (default: British/SOWPODS)")
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
            os.path.splitext(os.path.basename(input_path))[0] + "_gb_augmented.json",
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

    logging.info("Starting missing-word addition run")
    logging.info(f"Timestamp: {datetime.now().isoformat()}")
    logging.info(f"Input file: {input_path}")
    logging.info(f"Output file: {output_path}")
    logging.info(f"Log file: {log_path}")
    logging.info(f"Reference source: {'custom file — ' + args.reference if args.reference else args.reference_url}")
    logging.info(f"Word length range accepted: {args.min_length}-{args.max_length}")

    script_dir = os.path.dirname(os.path.abspath(__file__))

    try:
        reference_words = get_reference_words(args.reference, args.reference_url, script_dir)
        if not reference_words:
            raise ValueError("Reference word list is empty — aborting.")

        data = load_json(input_path)
        updated_data, added_count, added_detail, skipped_length = add_missing_words(
            data, reference_words, args.min_length, args.max_length
        )
        save_json(updated_data, output_path)

        total_before = sum(len(v) for v in data.values() if isinstance(v, list))
        total_after = sum(len(v) for v in updated_data.values() if isinstance(v, list))

        logging.info("-" * 60)
        logging.info("SUMMARY")
        logging.info(f"  Reference list size:        {len(reference_words)} words")
        logging.info(f"  Total words before:         {total_before}")
        logging.info(f"  Total words after:          {total_after}")
        logging.info(f"  Words added:                {added_count}")
        logging.info(f"  Reference words skipped (outside length {args.min_length}-{args.max_length}): {skipped_length}")
        logging.info("-" * 60)
        logging.info("Done. Output written successfully.")

    except Exception as e:
        logging.exception(f"Run failed with an error: {e}")

    # Do NOT auto-exit — keep the console open so logs stay visible.
    print()
    input("Finished. Press Enter to close this window...")


if __name__ == "__main__":
    main()
