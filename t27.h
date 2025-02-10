#pragma once

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <stdbool.h>
#include <string.h>
#include <ctype.h>

// 26个字母，加上撇号，共27个分支
#define ALPHA 27

/* 基于路径压缩的字典节点结构 */
typedef struct dict {
    char* label;             // 边上存储的字符串（均为小写），由标签池统一分配
    int label_len;           // label 的长度
    struct dict* dwn[ALPHA]; // 各子分支指针
    struct dict* up;         // 指向父节点
    bool terminal;           // 是否为单词结尾
    int freq;                // 单词出现次数（仅 terminal 节点有效）
} dict;

/* 接口函数声明 */
dict* dict_init(void);
bool dict_addword(dict* p, const char* wd);
int dict_nodecount(const dict* p);
int dict_wordcount(const dict* p);
dict* dict_spell(const dict* p, const char* str);
void dict_free(dict** p);
int dict_mostcommon(const dict* p);
/* 返回两个终端节点完整单词去掉最长公共前缀后剩余字符数之和 */
unsigned dict_cmp(dict* p1, dict* p2);
/* 自动补全：给定前缀 wd，返回全词中词频最高的 5 个建议（完整单词），存入 suggestions 数组，
   suggestion_count 返回候选数 */
void dict_autocomplete(const dict* p, const char* wd, char suggestions[][256], int* suggestion_count);

/* 辅助函数声明 */
int char_to_index(char c);

/* 内存池释放函数（节点池） */
void pool_free(void);
/* 标签池释放函数 */
void label_pool_free(void);
