#include <jni.h>
#include "../cpp/radixTree.h"
#include <string>
#include <memory>
#include <vector>
#include <unordered_map>

// 全局变量存储RadixTree实例
std::unordered_map<jlong, std::shared_ptr<radix::RadixTree>> g_trees;
jlong g_next_handle = 1;

extern "C" {

// 初始化方法
JNIEXPORT jlong JNICALL Java_common_utils_radix_RadixTreeJNI_initialize(
    JNIEnv* env, jobject thiz, jstring dictionary_path) {
    
    const char* path = env->GetStringUTFChars(dictionary_path, NULL);
    
    // 创建RadixTree实例
    auto tree = std::make_shared<radix::RadixTree>();
    
    // 从文件加载词典
    bool result = tree->loadFromFile(path);
    
    // 释放字符串
    env->ReleaseStringUTFChars(dictionary_path, path);
    
    if (!result) {
        return 0; // 加载失败
    }
    
    // 存储并返回句柄
    jlong handle = g_next_handle++;
    g_trees[handle] = tree;
    return handle;
}

// 查找实体方法
JNIEXPORT jobjectArray JNICALL Java_common_utils_radix_RadixTreeJNI_findEntities(
    JNIEnv* env, jobject thiz, jlong handle, jstring text) {
    
    // 查找树实例
    auto it = g_trees.find(handle);
    if (it == g_trees.end()) {
        return NULL;
    }
    
    auto& tree = it->second;
    const char* text_cstr = env->GetStringUTFChars(text, NULL);
    std::string text_str(text_cstr);
    
    // 查找实体
    std::vector<radix::EntityMatch> matches = tree->findEntities(text_str);
    
    // 创建Java对象数组
    jclass matchClass = env->FindClass("common/utils/radix/EntityMatch");
    
    // 获取构造函数
    jmethodID constructor = env->GetMethodID(matchClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;IID)V");
    
    // 创建结果数组
    jobjectArray result = env->NewObjectArray(matches.size(), matchClass, NULL);
    
    // 填充数组
    for (size_t i = 0; i < matches.size(); i++) {
        const auto& match = matches[i];
        
        // 创建Java字符串
        jstring entityStr = env->NewStringUTF(match.entity.c_str());
        
        // 获取实体类型字符串
        std::string entityType;
        switch (match.entityType) {
            case radix::EntityType::GENERIC: entityType = "GENERIC"; break;
            case radix::EntityType::FINANCIAL_INSTITUTION: entityType = "FINANCIAL_INSTITUTION"; break;
            case radix::EntityType::CORPORATE_ENTITY: entityType = "CORPORATE_ENTITY"; break;
            case radix::EntityType::GOVERNMENT_BODY: entityType = "GOVERNMENT_BODY"; break;
            case radix::EntityType::INDIVIDUAL: entityType = "INDIVIDUAL"; break;
            case radix::EntityType::PEP: entityType = "PEP"; break;
            case radix::EntityType::CURRENCY: entityType = "CURRENCY"; break;
            case radix::EntityType::VESSEL: entityType = "VESSEL"; break;
            case radix::EntityType::AIRCRAFT: entityType = "AIRCRAFT"; break;
            case radix::EntityType::SANCTIONED_ENTITY: entityType = "SANCTIONED_ENTITY"; break;
            case radix::EntityType::HIGH_RISK_JURISDICTION: entityType = "HIGH_RISK_JURISDICTION"; break;
            case radix::EntityType::REGULATORY_TERM: entityType = "REGULATORY_TERM"; break;
            case radix::EntityType::RISK_INDICATOR: entityType = "RISK_INDICATOR"; break;
            default: entityType = "UNKNOWN"; break;
        }
        
        jstring entityTypeStr = env->NewStringUTF(entityType.c_str());
        
        // 创建Java对象
        jobject matchObj = env->NewObject(
            matchClass, constructor,
            entityStr, entityTypeStr,
            match.startPos, match.endPos,
            match.confidence
        );
        
        // 添加到数组
        env->SetObjectArrayElement(result, i, matchObj);
        
        // 释放局部引用
        env->DeleteLocalRef(entityStr);
        env->DeleteLocalRef(entityTypeStr);
        env->DeleteLocalRef(matchObj);
    }
    
    // 释放资源
    env->ReleaseStringUTFChars(text, text_cstr);
    
    return result;
}

// 添加实体方法
JNIEXPORT jboolean JNICALL Java_common_utils_radix_RadixTreeJNI_addEntity(
    JNIEnv* env, jobject thiz, jlong handle, jstring entity, jint entityType) {
    
    // 查找树实例
    auto it = g_trees.find(handle);
    if (it == g_trees.end()) {
        return JNI_FALSE;
    }
    
    auto& tree = it->second;
    const char* entity_cstr = env->GetStringUTFChars(entity, NULL);
    std::string entity_str(entity_cstr);
    
    // 将jint转换为EntityType枚举
    radix::EntityType type = static_cast<radix::EntityType>(entityType);
    
    // 添加实体
    bool result = tree->addWordWithType(entity_str, type);
    
    // 释放资源
    env->ReleaseStringUTFChars(entity, entity_cstr);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

// 释放资源方法
JNIEXPORT void JNICALL Java_common_utils_radix_RadixTreeJNI_dispose(
    JNIEnv* env, jobject thiz, jlong handle) {
    
    g_trees.erase(handle);
}

} // extern "C"