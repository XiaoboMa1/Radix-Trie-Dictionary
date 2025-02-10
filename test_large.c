/*
 * test_large.c
 * 从 resources 目录中读取文本文件（每行一个单词），构造 Trie，
 * 然后进行固定前缀和随机前缀查询测试。
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <dirent.h>
#include "t27.h"

#define LINE_SIZE 256
#define SUGGESTION_NUM 5

/* 遍历指定目录，将所有文件中每行的单词插入 Trie，返回总单词数 */
int build_trie_from_resources(dict *d, const char *dir_path) {
    DIR *dp;
    struct dirent *entry;
    char file_path[512];
    int total_words = 0;

    dp = opendir(dir_path);
    if (!dp) {
        perror("无法打开目录");
        exit(EXIT_FAILURE);
    }
    while ((entry = readdir(dp)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 ||
            strcmp(entry->d_name, "..") == 0)
            continue;
        snprintf(file_path, sizeof(file_path), "%s/%s", dir_path, entry->d_name);
        FILE *fp = fopen(file_path, "r");
        if (!fp) {
            fprintf(stderr, "无法打开文件: %s\n", file_path);
            continue;
        }
        char line[LINE_SIZE];
        while (fgets(line, sizeof(line), fp)) {
            line[strcspn(line, "\r\n")] = '\0';
            if (line[0] == '\0') continue;
            dict_addword(d, line);
            total_words++;
        }
        fclose(fp);
    }
    closedir(dp);
    return total_words;
}

/* 生成随机前缀，长度介于 min_len 与 max_len 之间，字母范围 a-z */
void generate_random_prefix(char *prefix, int min_len, int max_len) {
    int len = min_len + rand() % (max_len - min_len + 1);
    for (int i = 0; i < len; i++) {
        prefix[i] = 'a' + rand() % 26;
    }
    prefix[len] = '\0';
}

int main(void) {
    const char *resources_dir = "resources";
    dict *d = dict_init();

    printf("正在构造 Trie，请稍候...\n");
    clock_t start_time = clock();
    int total_words = build_trie_from_resources(d, resources_dir);
    clock_t end_time = clock();
    double build_time = ((double)(end_time - start_time)) / CLOCKS_PER_SEC;
    printf("构造完成：共插入 %d 个单词\n", total_words);
    printf("Trie 节点总数（路径压缩后）：%d\n", dict_nodecount(d));
    printf("字典中单词总数：%d\n", dict_wordcount(d));
    printf("构造 Trie 耗时：%.3f 秒\n", build_time);

    /* 固定前缀测试 */
    char suggestions[SUGGESTION_NUM][256] = {0};
    int suggestion_count = 0;

    dict_autocomplete(d, "se", suggestions, &suggestion_count);
    printf("\n前缀 \"se\" 的自动补全候选（top %d）：\n", suggestion_count);
    for (int i = 0; i < suggestion_count; i++) {
        printf("候选%d：%s\n", i+1, suggestions[i]);
    }

    /* 随机前缀测试：执行 100,000 次随机查询 */
    const int query_times = 10000;
    char rand_prefix[16];
    int found_count = 0;
    start_time = clock();
    for (int i = 0; i < query_times; i++) {
        generate_random_prefix(rand_prefix, 3, 5);
        dict_spell(d, rand_prefix);
        dict_autocomplete(d, rand_prefix, suggestions, &suggestion_count);
        if (suggestion_count > 0)
            found_count++;
    }
    end_time = clock();
    double avg_time = (((double)(end_time - start_time)) / CLOCKS_PER_SEC) / query_times * 1000;
    printf("\n随机查询 %d 次，平均每次查询耗时：%.4f ms，找到补全候选的前缀比例：%.2f%%\n",
           query_times, avg_time, (found_count * 100.0 / query_times));

    dict_free(&d);
    pool_free();
    label_pool_free();
    return 0;
}
