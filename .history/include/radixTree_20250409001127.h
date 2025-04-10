#pragma once

#include <string>
#include <memory>
#include <vector>
#include <array>
#include <algorithm>
#include <functional>

namespace radix {

// Financial entity types
enum class EntityType {
    GENERIC = 0,
    COMPANY = 1,
    CURRENCY = 2,
    METRIC = 3
};

// 26 letters + apostrophe = 27 branches
constexpr int ALPHA = 27;

// Forward declarations
class MemoryPool;
class LabelPool;

// Dictionary node structure
class DictNode {
public:
    std::string label;                // Edge label (lowercase)
    int label_len;                    // Label length
    std::array<DictNode*, ALPHA> dwn; // Child branches
    DictNode* up;                     // Parent node
    bool terminal;                    // Word end marker
    int freq;                         // Word frequency (valid only for terminal nodes)
    EntityType entityType;            // Financial entity type

    DictNode(const std::string& s = "");
    ~DictNode() = default;
};

// RadixTree dictionary with path compression
class RadixTree {
public:
    RadixTree();
    ~RadixTree();

    // Prevent copy and assignment
    RadixTree(const RadixTree&) = delete;
    RadixTree& operator=(const RadixTree&) = delete;

    // Core dictionary operations
    bool addWord(const std::string& word);
    bool addWordWithType(const std::string& word, EntityType type, int frequency = 1);
    int nodeCount() const;
    int wordCount() const;
    DictNode* spell(const std::string& word) const;
    int mostCommon() const;
    void autocomplete(const std::string& prefix, std::vector<std::string>& suggestions, int& suggestionCount);
    
    // Entity type specific search
    void typedSearch(const std::string& prefix, EntityType type, 
                    std::vector<std::string>& suggestions, int& suggestionCount);
    
    // Serialization
    bool saveToFile(const std::string& filename) const;
    bool loadFromFile(const std::string& filename);

private:
    DictNode* root;
    std::unique_ptr<MemoryPool> nodePool;
    std::unique_ptr<LabelPool> labelPool;

    // Helper methods
    DictNode* createNode(const std::string& s);
    void buildFullWord(const DictNode* node, std::string& buffer) const;
    int charToIndex(char c) const;
    void collectCompletions(const DictNode* start, const std::string& base, 
                           std::vector<std::pair<std::string, int>>& candidates) const;
    
    // Helper for typed search
    void collectTypeSpecificCompletions(const DictNode* start, const std::string& base, EntityType type,
                                       std::vector<std::pair<std::string, int>>& candidates) const;
};


// Memory pool for efficient node allocation
class MemoryPool {
public:
    MemoryPool(size_t blockSize = 4096);
    ~MemoryPool();
    
    DictNode* allocate();
    void clear();

private:
    struct NodeBlock {
        std::unique_ptr<DictNode[]> nodes;
        size_t used;
        std::unique_ptr<NodeBlock> next;
    };

    std::unique_ptr<NodeBlock> head;
    size_t blockSize;
};

// Memory pool for string label storage
class LabelPool {
public:
    LabelPool(size_t blockSize = 1048576);  // 1MB blocks
    ~LabelPool();
    
    std::string allocate(const std::string& s);
    void clear();

private:
    struct LabelBlock {
        std::unique_ptr<char[]> buffer;
        size_t used;
        size_t capacity;
        std::unique_ptr<LabelBlock> next;
    };

    std::unique_ptr<LabelBlock> head;
    size_t blockSize;
};

} // namespace radix