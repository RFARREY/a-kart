#include <string.h>
#include <jni.h>
#include <stdlib.h>

JNIEXPORT void JNICALL
Java_com_parrot_arsdk_arsal_ARNativeDataHelper_copyData(JNIEnv *env, jclass type, jlong data,
                                                        jint capacity, jint used, jbyteArray retArray) {
    (*env)->SetByteArrayRegion(env, retArray, 0, used, (jbyte *) (intptr_t) data);
}