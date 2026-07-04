"""
filter_two_letter_words.py

Removes two-letter words from the WordNest **large_wordlib** dictionary JSON
UNLESS they appear in the official Scrabble (TWL/SOWPODS-combined) list of
valid two-letter words. Words of any other length are left untouched.

This is specifically meant for `large_wordlib` (loaded via
OptimizedWordRepository.largeLibrary / R.raw.large_wordlib, used by Lexicon
for word-legality checks) — NOT wordlib500.json or largelib.json, which are
left alone. large_wordlib is a big Scrabble-legality dictionary, so garbage
two-letter entries there directly cause bad AI moves / bad word validation
in Lexicon.

Input JSON is expected in the same letter->word-list shape used across
WordNest's raw resources:
    { "A": ["APPLE", "ANT", ...], "B": [...], ... }

Usage:
    python filter_two_letter_words.py [large_wordlib.json] [output.json]

Both arguments are optional — if omitted, the script looks for
`large_wordlib.json` in the current directory, or prompts you for a path.

The script logs everything it does (INFO to console, plus a rotating
log file next to the output) and does NOT auto-exit when finished -
it waits for you to press Enter so you can read the summary/logs
before the window (if any) closes.
"""

import json
import logging
import os
import sys
from datetime import datetime

# ──────────────────────────────────────────────────────────────────────────
# Official Scrabble-valid two-letter words (combined TWL + SOWPODS superset).
# Using the superset is deliberately permissive: it keeps every two-letter
# word that's valid in AT LEAST ONE major Scrabble dictionary, so you don't
# accidentally strip out a word your players expect. Trim this set yourself
# if you want to be TWL-only or SOWPODS-only.
# ──────────────────────────────────────────────────────────────────────────
VALID_TWO_LETTER_WORDS = {
    "AA", "AB", "AD", "AE", "AG", "AH", "AI", "AL", "AM", "AN", "AR", "AS",
    "AT", "AW", "AX", "AY", "BA", "BE", "BI", "BO", "BY", "DE", "DO", "EF",
    "EH", "EL", "EM", "EN", "ER", "ES", "ET", "EW", "EX", "FA", "FE", "GI",
    "GO", "HA", "HE", "HI", "HM", "HO", "ID", "IF", "IN", "IO", "IS", "IT",
    "JO", "KA", "KI", "KO", "KY", "LA", "LI", "LO", "MA", "ME", "MI", "MM",
    "MO", "MU", "MY", "NA", "NE", "NO", "NU", "OB", "OD", "OE", "OF", "OH",
    "OI", "OK", "OM", "ON", "OP", "OR", "OS", "OW", "OX", "OY", "PA", "PE",
    "PH", "PI", "PO", "QI", "RE", "SH", "SI", "SO", "ST", "TA", "TE", "TI",
    "TO", "UG", "UH", "UM", "UN", "UP", "US", "UT", "WE", "WO", "XI", "XU",
    "YA", "YE", "YO", "YU", "ZA", "ZO",
}

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


def load_json(path: str) -> dict:
    logging.info(f"Reading input JSON: {path}")
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        raise ValueError("Expected top-level JSON object of letter -> [words]")
    return data


def filter_dictionary(data: dict) -> tuple[dict, int, int]:
    """
    Returns (filtered_data, kept_two_letter_count, removed_two_letter_count).
    Only touches two-letter entries; everything else passes through unchanged.
    """
    filtered: dict = {}
    kept = 0
    removed = 0

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

            if len(word) == 2:
                if word.upper() in VALID_TWO_LETTER_WORDS:
                    new_words.append(word)
                    kept += 1
                    logging.debug(f"Kept valid two-letter word: {word}")
                else:
                    removed += 1
                    logging.info(f"Removing invalid two-letter word: {word} (key '{key}')")
            else:
                new_words.append(word)

        filtered[key] = new_words

    return filtered, kept, removed


def save_json(data: dict, path: str) -> None:
    logging.info(f"Writing filtered JSON to: {path}")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main() -> None:
    print("=" * 70)
    print(" WordNest — large_wordlib Two-Letter Word Filter")
    print("=" * 70)

    DEFAULT_INPUT_CANDIDATES = ["large_wordlib.json", "large_wordlib.JSON"]

    if len(sys.argv) >= 2:
        input_path = sys.argv[1]
        if not os.path.isfile(input_path):
            print(f"Input file not found: {input_path}")
            input_path = prompt_for_path(
                "Enter path to large_wordlib JSON file: ", must_exist=True
            )
    else:
        auto = next((p for p in DEFAULT_INPUT_CANDIDATES if os.path.isfile(p)), None)
        if auto:
            choice = input(f"Found '{auto}' in current directory — use it? [Y/n]: ").strip().lower()
            input_path = auto if choice in ("", "y", "yes") else prompt_for_path(
                "Enter path to large_wordlib JSON file: ", must_exist=True
            )
        else:
            input_path = prompt_for_path(
                "Enter path to large_wordlib JSON file: ", must_exist=True
            )

    if len(sys.argv) >= 3:
        output_path = sys.argv[2]
    else:
        default_output = os.path.join(
            os.path.dirname(os.path.abspath(input_path)),
            os.path.splitext(os.path.basename(input_path))[0] + "_filtered.json",
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

    logging.info("Starting two-letter word filter run")
    logging.info(f"Timestamp: {datetime.now().isoformat()}")
    logging.info(f"Input file: {input_path}")
    logging.info(f"Output file: {output_path}")
    logging.info(f"Log file: {log_path}")
    logging.info(f"Valid two-letter word list size: {len(VALID_TWO_LETTER_WORDS)}")

    try:
        data = load_json(input_path)
        filtered_data, kept, removed = filter_dictionary(data)
        save_json(filtered_data, output_path)

        total_words_before = sum(len(v) for v in data.values() if isinstance(v, list))
        total_words_after = sum(len(v) for v in filtered_data.values() if isinstance(v, list))

        logging.info("-" * 60)
        logging.info("SUMMARY")
        logging.info(f"  Total words before: {total_words_before}")
        logging.info(f"  Total words after:  {total_words_after}")
        logging.info(f"  Two-letter words kept (valid Scrabble):    {kept}")
        logging.info(f"  Two-letter words removed (invalid):        {removed}")
        logging.info(f"  Words removed overall: {total_words_before - total_words_after}")
        logging.info("-" * 60)
        logging.info("Done. Output written successfully.")

    except Exception as e:
        logging.exception(f"Run failed with an error: {e}")

    # Do NOT auto-exit — keep the console open so logs stay visible.
    print()
    input("Finished. Press Enter to close this window...")


if __name__ == "__main__":
    main()
