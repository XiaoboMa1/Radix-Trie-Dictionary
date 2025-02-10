# Radix Trie Dictionary with Path Compression

## Overview
This project implements a highly efficient Radix Trie with path compression, designed to store and query large dictionaries. The data structure utilizes advanced memory management strategies—including a custom node pool and a large label pool—to reduce memory fragmentation and support over one million words with minimal overhead.

## Data Structures & Algorithms
- **Radix Trie with Path Compression**: Each edge in the trie stores a string rather than a single character, significantly reducing the total number of nodes.
- **Memory Management**: 
  - *Node Pool*: Nodes are allocated in large contiguous blocks to minimize malloc calls and improve cache efficiency.
  - *Label Pool*: All edge labels are allocated from large (1MB) memory blocks to avoid many small allocations, thereby reducing memory fragmentation.
- **Auto-Completion**: An iterative, non-recursive DFS algorithm is used to traverse the sub-tree of a given prefix and collect candidate completions based on frequency. Candidates are maintained in a fixed-size array sorted by frequency (and lexicographical order when frequencies match).

## Testing & Metrics
- **Test Data**: The sample data used for testing is located in the `resources` folder. Users can add additional text files (one word per line) to further expand the dataset.
- **Performance Metrics**:
  - Construction: For ~1.15 million words, the trie is built in approximately 0.102 seconds.
  - Query Speed: Average query time is around 0.0009 ms per query.
  - Auto-Completion: Auto-completion queries now support dynamic input from the command line. Users can input any prefix to retrieve the top 5 most frequent suggestions.
- **Methodology**: The project has been stress-tested with 100,000 random prefix queries. Users can replicate and extend the tests by placing new data files in the `resources` folder.

## How to Use
1. Place your word list files in the `resources` directory.
2. Compile the project using the provided Makefile:
