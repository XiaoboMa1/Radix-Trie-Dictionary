#include "RadixTree.h"
#include <algorithm>
#include <cstring>
#include <queue>
#include <stack>
#include <cassert>
#include <fstream>
#include <sstream>

namespace radix {

DictNode::DictNode(const std::string& s) : 
    label(s), 
    label_len(static_cast<int>(s.length())), 
    dwn{}, 
    up(nullptr),
    terminal(false), 
    freq(0),
    entityType(EntityType::GENERIC) {}

// Add new method to add words with entity type
bool RadixTree::addWordWithType(const std::string& word, EntityType type, int frequency) {
    if (word.empty()) return false;
    
    bool added = addWord(word);
    
    // Update entity type and frequency if word exists
    DictNode* node = spell(word);
    if (node && node->terminal) {
        node->entityType = type;
        node->freq = frequency;
    }
    
    return added;
}

// Add method for entity type-specific completions
void RadixTree::collectTypeSpecificCompletions(
    const DictNode* start, 
    const std::string& base, 
    EntityType type,
    std::vector<std::pair<std::string, int>>& candidates) const {
    
    if (!start) return;
    
    // Add this word if it's terminal and of the specified type
    if (start->terminal && 
        (type == EntityType::GENERIC || start->entityType == type)) {
        candidates.emplace_back(base, start->freq);
    }
    
    // Recurse to all children
    for (int i = 0; i < ALPHA; i++) {
        if (start->dwn[i]) {
            std::string next = base;
            next.append(start->dwn[i]->label);
            collectTypeSpecificCompletions(start->dwn[i], next, type, candidates);
        }
    }
}

// Add typed search method
void RadixTree::typedSearch(
    const std::string& prefix, 
    EntityType type,
    std::vector<std::string>& suggestions, 
    int& suggestionCount) {
    
    suggestions.clear();
    suggestionCount = 0;
    
    // Find prefix node using existing logic
    DictNode* current = root;
    size_t prefixIter = 0;
    
    // Navigate to prefix node
    while (prefixIter < prefix.length() && current) {
        int idx = charToIndex(prefix[prefixIter]);
        if (idx < 0 || !current->dwn[idx]) break;
        
        DictNode* child = current->dwn[idx];
        const std::string& label = child->label;
        
        size_t i = 0;
        while (i < label.length() && 
               prefixIter < prefix.length() && 
               label[i] == prefix[prefixIter]) {
            i++;
            prefixIter++;
        }
        
        if (i < label.length()) {
            // Prefix ends in middle of an edge
            return;
        }
        
        current = child;
    }
    
    if (prefixIter < prefix.length()) {
        // Prefix not found
        return;
    }
    
    // Collect completions of specific type
    std::string base;
    buildFullWord(current, base);
    
    std::vector<std::pair<std::string, int>> candidates;
    collectTypeSpecificCompletions(current, base, type, candidates);
    
    // Sort by frequency (descending) and alphabetically
    std::sort(candidates.begin(), candidates.end(), 
              [](const auto& a, const auto& b) {
                  return a.second > b.second || 
                         (a.second == b.second && a.first < b.first);
              });
    
    // Get top suggestions
    int count = std::min(5, static_cast<int>(candidates.size()));
    for (int i = 0; i < count; i++) {
        suggestions.push_back(candidates[i].first);
    }
    
    suggestionCount = count;
}

// Add serialization methods
#include <fstream>

bool RadixTree::saveToFile(const std::string& filename) const {
    std::ofstream file(filename, std::ios::binary);
    if (!file) return false;
    
    // Format: [word count][word length][word][entity type][frequency]...
    std::vector<std::tuple<std::string, EntityType, int>> words;
    std::function<void(const DictNode*, std::string)> collectWords;
    
    collectWords = [&](const DictNode* node, std::string current) {
        if (!node) return;
        
        if (node->terminal) {
            words.emplace_back(current, node->entityType, node->freq);
        }
        
        for (int i = 0; i < ALPHA; i++) {
            if (node->dwn[i]) {
                collectWords(node->dwn[i], current + node->dwn[i]->label);
            }
        }
    };
    
    collectWords(root, "");
    
    // Write number of words
    int32_t wordCount = static_cast<int32_t>(words.size());
    file.write(reinterpret_cast<const char*>(&wordCount), sizeof(wordCount));
    
    // Write each word
    for (const auto& [word, type, freq] : words) {
        int32_t len = static_cast<int32_t>(word.length());
        file.write(reinterpret_cast<const char*>(&len), sizeof(len));
        file.write(word.c_str(), len);
        
        int8_t entityType = static_cast<int8_t>(type);
        file.write(reinterpret_cast<const char*>(&entityType), sizeof(entityType));
        
        file.write(reinterpret_cast<const char*>(&freq), sizeof(freq));
    }
    
    return true;
}

bool RadixTree::loadFromFile(const std::string& filename) {
    std::ifstream file(filename, std::ios::binary);
    if (!file) return false;
    
    // Clear existing tree
    nodePool->clear();
    labelPool->clear();
    root = createNode("");
    
    // Read number of words
    int32_t wordCount;
    file.read(reinterpret_cast<char*>(&wordCount), sizeof(wordCount));
    
    // Read each word
    for (int32_t i = 0; i < wordCount; i++) {
        int32_t len;
        file.read(reinterpret_cast<char*>(&len), sizeof(len));
        
        std::string word(len, ' ');
        file.read(&word[0], len);
        
        int8_t entityType;
        file.read(reinterpret_cast<char*>(&entityType), sizeof(entityType));
        
        int freq;
        file.read(reinterpret_cast<char*>(&freq), sizeof(freq));
        
        addWordWithType(word, static_cast<EntityType>(entityType), freq);
    }
    
    return true;
}
// Memory pool implementation
MemoryPool::MemoryPool(size_t blockSize) : blockSize(blockSize) {
    head = std::make_unique<NodeBlock>();
    head->nodes = std::make_unique<DictNode[]>(blockSize);
    head->used = 0;
    head->next = nullptr;
}

MemoryPool::~MemoryPool() {
    clear();
}

DictNode* MemoryPool::allocate() {
    if (!head || head->used >= blockSize) {
        auto newBlock = std::make_unique<NodeBlock>();
        newBlock->nodes = std::make_unique<DictNode[]>(blockSize);
        newBlock->used = 0;
        newBlock->next = std::move(head);
        head = std::move(newBlock);
    }
    return &head->nodes[head->used++];
}

void MemoryPool::clear() {
    head.reset();
}

// Label pool implementation
LabelPool::LabelPool(size_t blockSize) : blockSize(blockSize) {
    head = std::make_unique<LabelBlock>();
    head->buffer = std::make_unique<char[]>(blockSize);
    head->used = 0;
    head->capacity = blockSize;
    head->next = nullptr;
}

LabelPool::~LabelPool() {
    clear();
}

std::string LabelPool::allocate(const std::string& s) {
    if (s.length() + 1 > blockSize) {
        // String too long for pool, return a copy
        return s;
    }

    if (!head || head->used + s.length() + 1 > head->capacity) {
        auto newBlock = std::make_unique<LabelBlock>();
        newBlock->buffer = std::make_unique<char[]>(blockSize);
        newBlock->used = 0;
        newBlock->capacity = blockSize;
        newBlock->next = std::move(head);
        head = std::move(newBlock);
    }
    
    char* dest = head->buffer.get() + head->used;
    std::memcpy(dest, s.c_str(), s.length() + 1);
    head->used += s.length() + 1;
    
    return std::string(dest);
}

void LabelPool::clear() {
    head.reset();
}

// RadixTree implementation
RadixTree::RadixTree() : 
    nodePool(std::make_unique<MemoryPool>()), 
    labelPool(std::make_unique<LabelPool>()) {
    
    root = createNode("");
}

RadixTree::~RadixTree() {
    nodePool->clear();
    labelPool->clear();
    root = nullptr;
}

DictNode* RadixTree::createNode(const std::string& s) {
    DictNode* node = nodePool->allocate();
    new (node) DictNode(labelPool->allocate(s));
    return node;
}

int RadixTree::charToIndex(char c) const {
    c = std::tolower(c);
    if (c >= 'a' && c <= 'z') return c - 'a';
    if (c == '\'') return 26;
    return -1;
}

bool RadixTree::addWord(const std::string& word) {
    if (!root || word.empty()) return false;
    
    std::string::const_iterator wordIter = word.begin();
    DictNode* current = root;
    
    while (wordIter != word.end()) {
        int idx = charToIndex(*wordIter);
        if (idx < 0 || idx >= ALPHA) return false;
        
        DictNode* child = current->dwn[idx];
        if (!child) {
            // Create new node with remaining suffix
            std::string suffix(wordIter, word.end());
            DictNode* newNode = createNode(suffix);
            newNode->terminal = true;
            newNode->freq = 1;
            newNode->up = current;
            current->dwn[idx] = newNode;
            return true;
        }
        
        // Match as much of the child's label as possible
        size_t i = 0;
        while (i < child->label.length() && 
               wordIter + i != word.end() && 
               std::tolower(child->label[i]) == std::tolower(*(wordIter + i))) {
            i++;
        }
        
        if (i == 0) return false; // Shouldn't happen
        
        if (i < child->label.length()) {
            // Partial match - split the node
            std::string prefix = child->label.substr(0, i);
            DictNode* splitNode = createNode(prefix);
            splitNode->up = current;
            current->dwn[idx] = splitNode;
            
            // Update child node with remaining label
            std::string childSuffix = child->label.substr(i);
            child->label = labelPool->allocate(childSuffix);
            child->label_len = childSuffix.length();
            child->up = splitNode;
            
            int childIdx = charToIndex(child->label[0]);
            splitNode->dwn[childIdx] = child;
            
            if (wordIter + i == word.end()) {
                // Word ends at split point
                if (splitNode->terminal) {
                    splitNode->freq++;
                    return false;
                }
                splitNode->terminal = true;
                splitNode->freq = 1;
                return true;
            } else {
                // Create new node for remaining word suffix
                std::string newSuffix(wordIter + i, word.end());
                DictNode* newNode = createNode(newSuffix);
                newNode->terminal = true;
                newNode->freq = 1;
                newNode->up = splitNode;
                
                int newIdx = charToIndex(newSuffix[0]);
                splitNode->dwn[newIdx] = newNode;
                return true;
            }
        } else {
            // Full match with label, move to next segment
            wordIter += i;
            current = child;
        }
    }
    
    // Word ends at current node
    if (current->terminal) {
        current->freq++;
        return false; // Word already exists
    }
    
    current->terminal = true;
    current->freq = 1;
    return true;
}

int RadixTree::nodeCount() const {
    if (!root) return 0;
    
    int count = 0;
    std::stack<DictNode*> nodeStack;
    nodeStack.push(root);
    
    while (!nodeStack.empty()) {
        DictNode* node = nodeStack.top();
        nodeStack.pop();
        count++;
        
        for (const auto& child : node->dwn) {
            if (child) nodeStack.push(child);
        }
    }
    
    return count;
}

int RadixTree::wordCount() const {
    if (!root) return 0;
    
    int count = 0;
    std::stack<DictNode*> nodeStack;
    nodeStack.push(root);
    
    while (!nodeStack.empty()) {
        DictNode* node = nodeStack.top();
        nodeStack.pop();
        
        if (node->terminal) count += node->freq;
        
        for (const auto& child : node->dwn) {
            if (child) nodeStack.push(child);
        }
    }
    
    return count;
}

DictNode* RadixTree::spell(const std::string& word) const {
    if (!root || word.empty()) return nullptr;
    
    std::string::const_iterator wordIter = word.begin();
    const DictNode* current = root;
    
    while (wordIter != word.end()) {
        int idx = charToIndex(*wordIter);
        if (idx < 0 || idx >= ALPHA) return nullptr;
        
        DictNode* child = current->dwn[idx];
        if (!child) return nullptr;
        
        // Match child's label against remaining word
        size_t i = 0;
        while (i < child->label.length() && 
               wordIter + i != word.end() && 
               std::tolower(child->label[i]) == std::tolower(*(wordIter + i))) {
            i++;
        }
        
        if (i != child->label.length()) return nullptr; // Partial match
        
        wordIter += i;
        current = child;
    }
    
    return (current->terminal) ? const_cast<DictNode*>(current) : nullptr;
}

int RadixTree::mostCommon() const {
    if (!root) return 0;
    
    int maxFreq = 0;
    std::stack<const DictNode*> nodeStack;
    nodeStack.push(root);
    
    while (!nodeStack.empty()) {
        const DictNode* node = nodeStack.top();
        nodeStack.pop();
        
        if (node->terminal && node->freq > maxFreq) {
            maxFreq = node->freq;
        }
        
        for (const auto& child : node->dwn) {
            if (child) nodeStack.push(child);
        }
    }
    
    return maxFreq;
}

void RadixTree::buildFullWord(const DictNode* node, std::string& buffer) const {
    if (!node) return;
    
    std::vector<const DictNode*> path;
    const DictNode* current = node;
    
    // Build path from node to root
    while (current && current->up) {
        path.push_back(current);
        current = current->up;
    }
    
    // Construct word by following path from root to node
    buffer.clear();
    for (auto it = path.rbegin(); it != path.rend(); ++it) {
        buffer += (*it)->label;
    }
}

void RadixTree::collectCompletions(const DictNode* start, const std::string& base,
                                  std::vector<std::pair<std::string, int>>& candidates) const {
    if (!start) return;
    
    // Use non-recursive DFS to collect completions
    struct StackItem {
        const DictNode* node;
        std::string currentWord;
        
        StackItem(const DictNode* n, const std::string& word) : node(n), currentWord(word) {}
    };
    
    std::stack<StackItem> nodeStack;
    nodeStack.push(StackItem(start, base));
    
    while (!nodeStack.empty()) {
        StackItem item = nodeStack.top();
        nodeStack.pop();
        
        // Add terminal nodes to candidates
        if (item.node->terminal) {
            candidates.push_back(std::make_pair(item.currentWord, item.node->freq));
        }
        
        // Add all children to stack
        for (int i = 0; i < ALPHA; i++) {
            if (item.node->dwn[i]) {
                const DictNode* child = item.node->dwn[i];
                std::string newWord = item.currentWord + child->label;
                nodeStack.push(StackItem(child, newWord));
            }
        }
    }
}

void RadixTree::autocomplete(const std::string& prefix, std::vector<std::string>& suggestions, int& suggestionCount) {
    if (!root || prefix.empty()) {
        suggestionCount = 0;
        return;
    }
    
    suggestions.clear();
    suggestionCount = 0;
    
    // Find the node corresponding to prefix
    const DictNode* current = root;
    std::string::const_iterator prefixIter = prefix.begin();
    
    while (prefixIter != prefix.end()) {
        int idx = charToIndex(*prefixIter);
        if (idx < 0 || current->dwn[idx] == nullptr) {
            suggestionCount = 0;
            return; // Prefix doesn't exist
        }
        
        current = current->dwn[idx];
        size_t i = 0;
        while (i < current->label.length() && 
               prefixIter + i != prefix.end() && 
               std::tolower(current->label[i]) == std::tolower(*(prefixIter + i))) {
            i++;
        }
        
        if (i < current->label.length() && prefixIter + i != prefix.end()) {
            suggestionCount = 0;
            return; // Prefix match interrupted
        }
        
        prefixIter += i;
    }
    
    // Found the prefix node, now collect completions
    std::string base;
    buildFullWord(current, base);
    
    std::vector<std::pair<std::string, int>> candidates;
    collectCompletions(current, base, candidates);
    
    // Sort by frequency (descending) and then alphabetically
    std::sort(candidates.begin(), candidates.end(), 
              [](const auto& a, const auto& b) {
                  return a.second > b.second || 
                         (a.second == b.second && a.first < b.first);
              });
    
    // Get top 5 suggestions
    int count = std::min(5, static_cast<int>(candidates.size()));
    for (int i = 0; i < count; i++) {
        suggestions.push_back(candidates[i].first);
    }
    
    suggestionCount = count;
}
std::vector<radix::EntityMatch> RadixTree::findEntities(const std::string& text) {
    std::vector<EntityMatch> results;
    
    // 简单的单词分割
    std::istringstream iss(text);
    std::string word;
    int pos = 0;
    
    while (iss >> word) {
        int wordStart = text.find(word, pos);
        pos = wordStart + word.length();
        
        // 去除标点符号进行检查
        std::string cleanWord = word;
        cleanWord.erase(std::remove_if(cleanWord.begin(), cleanWord.end(), 
                           [](char c) { return std::ispunct(c) && c != '\''; }), 
                     cleanWord.end());
        
        if (!cleanWord.empty()) {
            DictNode* node = spell(cleanWord);
            if (node && node->terminal) {
                results.emplace_back(cleanWord, node->entityType, 
                                   wordStart, wordStart + word.length(), 1.0);
            }
        }
    }
    
    return results;
}

} // namespace radix