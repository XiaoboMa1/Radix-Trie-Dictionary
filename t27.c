/*
 * t27.c
 * Radix Trie Dictionary with Path Compression – 新版
 * 支持至少 100 万单词，采用大块标签池、节点池，以及非递归遍历方法，
 * 以保证在大数据集下高效且不崩溃。
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <ctype.h>

/* -------------------- 常量定义 -------------------- */
#define ALPHA 27           /* 26个字母 + 撇号 */
#define NODE_BLOCK_SIZE 4096   /* 节点池每块节点数（适当增大） */
#define LABEL_BLOCK_SIZE (1048576) /* 标签池每块 1MB */

/* -------------------- 数据结构 -------------------- */

/* Trie 节点 */
typedef struct dict {
    char *label;             /* 边上的字符串（全部小写），由标签池分配 */
    int label_len;           /* label 长度 */
    struct dict *dwn[ALPHA]; /* 子节点数组 */
    struct dict *up;         /* 父节点指针 */
    bool terminal;           /* 是否为单词结束节点 */
    int freq;                /* 出现次数（仅 terminal 有效） */
} dict;

/* 节点池块 */
typedef struct NodeBlock {
    dict *nodes;           /* 一块内存存放多个节点 */
    int used;
    struct NodeBlock *next;
} NodeBlock;

/* 标签池块 */
typedef struct LabelBlock {
    char *buffer;         /* 一块连续内存 */
    int used;
    int capacity;
    struct LabelBlock *next;
} LabelBlock;

/* -------------------- 全局静态变量 -------------------- */
static NodeBlock *node_pool_head = NULL;
static LabelBlock *label_pool_head = NULL;

/* -------------------- 节点池管理 -------------------- */
static dict* node_alloc(void) {
    if (!node_pool_head || node_pool_head->used >= NODE_BLOCK_SIZE) {
        NodeBlock *new_block = (NodeBlock *)malloc(sizeof(NodeBlock));
        if (!new_block) {
            perror("节点池分配失败");
            exit(EXIT_FAILURE);
        }
        new_block->nodes = (dict *)malloc(sizeof(dict) * NODE_BLOCK_SIZE);
        if (!new_block->nodes) {
            perror("节点数组分配失败");
            exit(EXIT_FAILURE);
        }
        new_block->used = 0;
        new_block->next = node_pool_head;
        node_pool_head = new_block;
    }
    return &node_pool_head->nodes[node_pool_head->used++];
}

void pool_free(void) {
    NodeBlock *cur = node_pool_head;
    while (cur) {
        NodeBlock *next = cur->next;
        free(cur->nodes);
        free(cur);
        cur = next;
    }
    node_pool_head = NULL;
}

/* -------------------- 标签池管理 -------------------- */
static char* label_pool_alloc(const char *s, int len) {
    if (len + 1 > LABEL_BLOCK_SIZE) {
        /* 单个标签太长，单独 malloc */
        char *ptr = malloc(len + 1);
        if (!ptr) {
            perror("大标签分配失败");
            exit(EXIT_FAILURE);
        }
        memcpy(ptr, s, len);
        ptr[len] = '\0';
        return ptr;
    }
    if (!label_pool_head || label_pool_head->used + len + 1 > label_pool_head->capacity) {
        LabelBlock *new_block = (LabelBlock *)malloc(sizeof(LabelBlock));
        if (!new_block) {
            perror("标签池块分配失败");
            exit(EXIT_FAILURE);
        }
        new_block->capacity = LABEL_BLOCK_SIZE;
        new_block->used = 0;
        new_block->buffer = (char *)malloc(new_block->capacity);
        if (!new_block->buffer) {
            perror("标签池缓冲区分配失败");
            exit(EXIT_FAILURE);
        }
        new_block->next = label_pool_head;
        label_pool_head = new_block;
    }
    char *ret = label_pool_head->buffer + label_pool_head->used;
    memcpy(ret, s, len);
    ret[len] = '\0';
    label_pool_head->used += len + 1;
    return ret;
}

void label_pool_free(void) {
    LabelBlock *cur = label_pool_head;
    while (cur) {
        LabelBlock *next = cur->next;
        free(cur->buffer);
        free(cur);
        cur = next;
    }
    label_pool_head = NULL;
}

/* -------------------- 辅助函数 -------------------- */
/* 将字符转换为下标（小写；撇号映射为26） */
int char_to_index(char c) {
    c = tolower(c);
    if (c >= 'a' && c <= 'z') return c - 'a';
    if (c == '\'') return 26;
    return -1;
}

/* 创建新节点：利用标签池分配 label */
static dict* create_node(const char *s, int len) {
    dict *node = node_alloc();
    node->label = label_pool_alloc(s, len);
    node->label_len = len;
    node->terminal = false;
    node->freq = 0;
    node->up = NULL;
    for (int i = 0; i < ALPHA; i++) {
        node->dwn[i] = NULL;
    }
    return node;
}

/* -------------------- 字典接口 -------------------- */
dict* dict_init(void) {
    return create_node("", 0);
}

bool dict_addword(dict *root, const char *wd) {
    if (!root || !wd) return false;
    const char *word = wd;
    dict *current = root;
    while (*word) {
        int idx = char_to_index(*word);
        if (idx < 0 || idx >= ALPHA) return false;
        dict *child = current->dwn[idx];
        if (!child) {
            dict *new_node = create_node(word, (int)strlen(word));
            new_node->terminal = true;
            new_node->freq = 1;
            new_node->up = current;
            current->dwn[idx] = new_node;
            return true;
        }
        int i = 0;
        while (i < child->label_len && word[i] && tolower(child->label[i]) == tolower(word[i])) {
            i++;
        }
        if (i == 0) return false; /* 理论上不应发生 */
        if (i < child->label_len) {
            /* 部分匹配，分裂节点 */
            dict *split_node = create_node(child->label, i);
            split_node->up = current;
            current->dwn[idx] = split_node;
            int new_len = child->label_len - i;
            char *new_label = label_pool_alloc(child->label + i, new_len);
            child->label = new_label;
            child->label_len = new_len;
            child->up = split_node;
            int child_idx = char_to_index(child->label[0]);
            split_node->dwn[child_idx] = child;
            if (word[i] == '\0') {
                if (split_node->terminal) {
                    split_node->freq++;
                    return false;
                }
                split_node->terminal = true;
                split_node->freq = 1;
                return true;
            } else {
                dict *new_node = create_node(word + i, (int)strlen(word + i));
                new_node->terminal = true;
                new_node->freq = 1;
                new_node->up = split_node;
                int new_idx = char_to_index(new_node->label[0]);
                split_node->dwn[new_idx] = new_node;
                return true;
            }
        } else {
            word += i;
            current = child;
        }
    }
    if (current->terminal) {
        current->freq++;
        return false;
    }
    current->terminal = true;
    current->freq = 1;
    return true;
}

int dict_nodecount(const dict *p) {
    if (!p) return 0;
    int cnt = 1;
    for (int i = 0; i < ALPHA; i++) {
        if (p->dwn[i])
            cnt += dict_nodecount(p->dwn[i]);
    }
    return cnt;
}

int dict_wordcount(const dict *p) {
    if (!p) return 0;
    int count = 0;
    if (p->terminal) count += p->freq;
    for (int i = 0; i < ALPHA; i++) {
        if (p->dwn[i])
            count += dict_wordcount(p->dwn[i]);
    }
    return count;
}

dict* dict_spell(const dict *root, const char *str) {
    if (!root || !str) return NULL;
    const char *word = str;
    const dict *current = root;
    while (*word) {
        int idx = char_to_index(*word);
        if (idx < 0 || idx >= ALPHA) return NULL;
        dict *child = current->dwn[idx];
        if (!child) return NULL;
        int i = 0;
        while (i < child->label_len && word[i] && tolower(child->label[i]) == tolower(word[i])) {
            i++;
        }
        if (i != child->label_len) return NULL;
        word += i;
        current = child;
    }
    return (current->terminal) ? (dict *)current : NULL;
}

int dict_mostcommon(const dict *p) {
    if (!p) return 0;
    int max_freq = (p->terminal ? p->freq : 0);
    for (int i = 0; i < ALPHA; i++) {
        if (p->dwn[i]) {
            int child_max = dict_mostcommon(p->dwn[i]);
            if (child_max > max_freq)
                max_freq = child_max;
        }
    }
    return max_freq;
}

/* 释放字典（仅清除结构，标签由标签池统一释放） */
void dict_free(dict **p) {
    if (!p || !*p) return;
    for (int i = 0; i < ALPHA; i++) {
        if ((*p)->dwn[i])
            dict_free(&(*p)->dwn[i]);
    }
    *p = NULL;
}

/* -------------------- 非递归构造完整单词 -------------------- */
/* 从 node 出发，沿 up 指针收集各节点 label，倒序拼接到 buffer 中。
   buffer_size 为 buffer 总大小。 */
void build_full_word_iterative(const dict *node, char *buffer, int buffer_size) {
    const dict *stack[256];
    int depth = 0;
    const dict *cur = node;
    while (cur && cur->up) {
        if (depth < 256) {
            stack[depth++] = cur;
        } else {
            break; /* 超过最大单词长度 */
        }
        cur = cur->up;
    }
    buffer[0] = '\0';
    for (int i = depth - 1; i >= 0; i--) {
        strncat(buffer, stack[i]->label, buffer_size - strlen(buffer) - 1);
    }
}

/* -------------------- 非递归 DFS 自动补全 -------------------- */

/* DFS 堆栈项 */
typedef struct {
    const dict *node;
    char cur_word[256];
} DFSStackItem;

/* 自动补全候选结构 */
typedef struct {
    char word[256];
    int freq;
} Completion;

/* 采用动态栈进行 DFS 遍历：非递归遍历当前子树，收集终端节点作为候选 */
static void dfs_collect_iterative(const dict *start, const char *base, Completion best_arr[5], int *best_count) {
    /* 动态分配 DFS 栈，初始容量 1024 */
    int stack_capacity = 1024;
    int stack_size = 0;
    DFSStackItem *stack = (DFSStackItem *)malloc(sizeof(DFSStackItem) * stack_capacity);
    if (!stack) {
        perror("DFS 栈分配失败");
        exit(EXIT_FAILURE);
    }
    /* 初始化：将起始节点与 base 作为第一个栈项 */
    stack[stack_size].node = start;
    strncpy(stack[stack_size].cur_word, base, sizeof(stack[stack_size].cur_word)-1);
    stack[stack_size].cur_word[sizeof(stack[stack_size].cur_word)-1] = '\0';
    stack_size++;
    *best_count = 0;
    while (stack_size > 0) {
        DFSStackItem item = stack[--stack_size];
        /* 如果该节点为终端节点，则考虑加入候选 */
        if (item.node->terminal) {
            Completion cand;
            strncpy(cand.word, item.cur_word, sizeof(cand.word)-1);
            cand.word[sizeof(cand.word)-1] = '\0';
            cand.freq = item.node->freq;
            if (*best_count < 5) {
                best_arr[(*best_count)++] = cand;
            } else {
                int min_index = 0;
                for (int i = 1; i < 5; i++) {
                    if (best_arr[i].freq < best_arr[min_index].freq ||
                        (best_arr[i].freq == best_arr[min_index].freq &&
                         strcmp(best_arr[i].word, best_arr[min_index].word) > 0))
                        min_index = i;
                }
                if (cand.freq > best_arr[min_index].freq ||
                    (cand.freq == best_arr[min_index].freq &&
                     strcmp(cand.word, best_arr[min_index].word) < 0))
                {
                    best_arr[min_index] = cand;
                }
            }
        }
        /* 遍历子节点 */
        for (int i = 0; i < ALPHA; i++) {
            if (item.node->dwn[i]) {
                const dict *child = item.node->dwn[i];
                char new_word[256];
                snprintf(new_word, sizeof(new_word), "%s%s", item.cur_word, child->label);
                /* 推入栈 */
                if (stack_size >= stack_capacity) {
                    stack_capacity *= 2;
                    DFSStackItem *new_stack = realloc(stack, sizeof(DFSStackItem) * stack_capacity);
                    if (!new_stack) {
                        perror("扩展 DFS 栈失败");
                        free(stack);
                        exit(EXIT_FAILURE);
                    }
                    stack = new_stack;
                }
                stack[stack_size].node = child;
                strncpy(stack[stack_size].cur_word, new_word, sizeof(stack[stack_size].cur_word)-1);
                stack[stack_size].cur_word[sizeof(stack[stack_size].cur_word)-1] = '\0';
                stack_size++;
            }
        }
    }
    free(stack);
}

/* 自动补全：先根据前缀 wd 定位对应节点，然后采用非递归 DFS 收集候选，返回前 5 个候选 */
void dict_autocomplete(const dict *root, const char *wd, char suggestions[][256], int *suggestion_count) {
    if (!root || !wd || !suggestions || !suggestion_count)
        return;
    *suggestion_count = 0;
    const dict *current = root;
    const char *p = wd;
    while (*p) {
        int idx = char_to_index(*p);
        if (idx < 0 || current->dwn[idx] == NULL)
            return;  /* 前缀不存在 */
        current = current->dwn[idx];
        int i = 0;
        while (i < current->label_len && *p && tolower(current->label[i]) == tolower(*p))
            i++, p++;
        if (i < current->label_len && *p != '\0')
            return;  /* 前缀匹配中断 */
    }
    /* 用非递归方式 DFS 遍历 current 的子树，收集候选 */
    char base[256] = {0};
    build_full_word_iterative(current, base, sizeof(base));
    Completion best_arr[5];
    int best_count = 0;
    dfs_collect_iterative(current, base, best_arr, &best_count);
    /* 对候选排序：频率降序；频率相同字典序升序 */
    for (int i = 0; i < best_count; i++) {
        for (int j = i + 1; j < best_count; j++) {
            if ((best_arr[j].freq > best_arr[i].freq) ||
               (best_arr[j].freq == best_arr[i].freq && strcmp(best_arr[j].word, best_arr[i].word) < 0))
            {
                Completion tmp = best_arr[i];
                best_arr[i] = best_arr[j];
                best_arr[j] = tmp;
            }
        }
    }
    int num = best_count < 5 ? best_count : 5;
    for (int i = 0; i < num; i++) {
        strncpy(suggestions[i], best_arr[i].word, 255);
        suggestions[i][255] = '\0';
    }
    *suggestion_count = num;
}
