"""
generate_wordnet_library.py

Builds two WordNest-compatible JSON files from Princeton WordNet:

  1. A word-validity library in the same {"A": [...], "B": [...]} shape used
     by wordlib500.json / large_wordlib*.json (loaded via
     OptimizedWordRepository) — drop this straight into
     app/src/main/res/raw/ and point OptimizedWordRepository at it, or feed
     it to CrosswordWordRepository / CodewordWordRepository the same way
     wordlib500.json is loaded today.

  2. A word -> definition map, meant to back crossword/data/ClueRepository.kt's
     ClueSource interface (see WordNetClueSource sketch at the bottom of this
     file's companion notes) — a JSON-file-backed alternative to the
     curated FallbackClueSource map, without needing the full offline
     SQLite/FTS5 pipeline described in ClueRepository.kt's docstring.

Requires: pip install nltk
On first run this downloads the 'wordnet' and 'omw-1.4' NLTK corpora
(~35MB, cached locally afterwards — no internet needed on subsequent runs).

Usage:
    python generate_wordnet_library.py
    python generate_wordnet_library.py --min-length 3 --max-length 15
    python generate_wordnet_library.py --out-dir ./output --pos n v a r

Outputs (default ./output/):
    wordnet_library.json      -> {"A": ["APPLE", ...], "B": [...], ...}
    wordnet_definitions.json  -> {"APPLE": "fruit with red or green skin...", ...}

Logs everything (console + a log file next to the output) and does NOT
auto-exit when finished — it waits for Enter so you can read the summary.
"""

import argparse
import json
import logging
import os
import re
import sys
from collections import defaultdict
from datetime import datetime

WORD_RE = re.compile(r"^[a-z]+$")


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


def ensure_wordnet_available():
    import nltk
    for corpus in ("wordnet", "omw-1.4"):
        try:
            nltk.data.find(f"corpora/{corpus}")
        except LookupError:
            logging.info(f"Downloading NLTK corpus: {corpus}")
            nltk.download(corpus, quiet=False)


def build_library(
    min_length: int,
    max_length: int,
    pos_tags: list,
    include_multiword: bool,
):
    """
    Returns (library_dict, definitions_dict, stats_dict).

    library_dict:     {"A": ["APPLE", ...], ...}   (letter -> sorted word list)
    definitions_dict: {"APPLE": "short definition", ...}
    """
    from nltk.corpus import wordnet as wn

    library = defaultdict(set)
    definitions: dict[str, str] = {}

    total_synsets = 0
    skipped_length = 0
    skipped_charset = 0
    skipped_multiword = 0

    for pos in pos_tags:
        for synset in wn.all_synsets(pos=pos):
            total_synsets += 1
            definition = synset.definition().strip()

            for lemma in synset.lemmas():
                raw = lemma.name()

                if "_" in raw or "-" in raw:
                    if not include_multiword:
                        skipped_multiword += 1
                        continue
                    raw = raw.replace("_", "").replace("-", "")

                lower = raw.lower()
                if not WORD_RE.match(lower):
                    skipped_charset += 1
                    continue
                if not (min_length <= len(lower) <= max_length):
                    skipped_length += 1
                    continue

                word = lower.upper()
                library[word[0]].add(word)

                # Prefer the shortest available definition for a word that
                # appears across multiple synsets/senses (better crossword clue).
                existing = definitions.get(word)
                if existing is None or len(definition) < len(existing):
                    definitions[word] = definition

    library_sorted = {letter: sorted(words) for letter, words in sorted(library.items())}

    stats = {
        "total_synsets_scanned": total_synsets,
        "unique_words": sum(len(v) for v in library_sorted.values()),
        "skipped_length": skipped_length,
        "skipped_charset": skipped_charset,
        "skipped_multiword": skipped_multiword,
    }
    return library_sorted, definitions, stats


def save_json(data, path: str) -> None:
    logging.info(f"Writing: {path}")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", default="./output", help="Directory to write output JSON files into")
    parser.add_argument("--min-length", type=int, default=2, help="Minimum word length (default: 2)")
    parser.add_argument("--max-length", type=int, default=15, help="Maximum word length (default: 15)")
    parser.add_argument(
        "--pos", nargs="+", default=["n", "v", "a", "r", "s"],
        help="WordNet POS tags to include: n=noun v=verb a=adjective r=adverb s=adj-satellite "
             "(default: all five)",
    )
    parser.add_argument(
        "--include-multiword", action="store_true",
        help="Also include multi-word lemmas (e.g. 'ice_cream' -> ICECREAM) by stripping "
             "separators, instead of skipping them entirely (default: skipped)",
    )
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    log_path = os.path.join(args.out_dir, "generate_wordnet_library_log.txt")
    setup_logging(log_path)

    print("=" * 70)
    print(" WordNest — Generate Dictionary + Clues from Princeton WordNet")
    print("=" * 70)

    logging.info(f"Timestamp: {datetime.now().isoformat()}")
    logging.info(f"Output directory: {os.path.abspath(args.out_dir)}")
    logging.info(f"Length range: {args.min_length}-{args.max_length}")
    logging.info(f"POS tags: {args.pos}")
    logging.info(f"Include multiword lemmas: {args.include_multiword}")

    try:
        ensure_wordnet_available()

        library, definitions, stats = build_library(
            min_length=args.min_length,
            max_length=args.max_length,
            pos_tags=args.pos,
            include_multiword=args.include_multiword,
        )

        library_path = os.path.join(args.out_dir, "wordnet_library.json")
        definitions_path = os.path.join(args.out_dir, "wordnet_definitions.json")

        save_json(library, library_path)
        save_json(definitions, definitions_path)

        logging.info("-" * 60)
        logging.info("SUMMARY")
        logging.info(f"  Synsets scanned:            {stats['total_synsets_scanned']}")
        logging.info(f"  Unique words kept:          {stats['unique_words']}")
        logging.info(f"  Definitions captured:       {len(definitions)}")
        logging.info(f"  Skipped (length range):     {stats['skipped_length']}")
        logging.info(f"  Skipped (non A-Z charset):  {stats['skipped_charset']}")
        logging.info(f"  Skipped (multi-word):       {stats['skipped_multiword']}")
        logging.info(f"  Library written to:         {library_path}")
        logging.info(f"  Definitions written to:     {definitions_path}")
        logging.info("-" * 60)
        logging.info("Done.")
        logging.info(
            "Next step: copy wordnet_library.json into app/src/main/res/raw/ and either "
            "point OptimizedWordRepository at it directly, or load it in "
            "CrosswordWordRepository/CodewordWordRepository the same way wordlib500.json "
            "is loaded today. For clues, load wordnet_definitions.json into a ClueSource "
            "implementation (see crossword/data/ClueRepository.kt) instead of the curated "
            "FallbackClueSource map."
        )

    except Exception as e:
        logging.exception(f"Run failed with an error: {e}")

    print()
    input("Finished. Press Enter to close this window...")


if __name__ == "__main__":
    main()
