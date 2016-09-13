#include <jni.h>
#include <stdio.h>

static jfieldID rawDataFld;

void init(JNIEnv* env, jobject self) {
    jclass segmentClass = env->FindClass("com/medallia/data/Segment");
    rawDataFld = env->GetFieldID(segmentClass, "rawData", "[[J");
}

void process(JNIEnv* env, jobject self, jobject segment) {
    jobjectArray rawDataObj = (jobjectArray) env->GetObjectField(segment, rawDataFld);
    jint cols = env->GetArrayLength(rawDataObj);

    jlong** rawData = new jlong*[cols];
    jint nRows = 0;
    jboolean anyCopies = JNI_FALSE;
    for (jint i = 0; i < cols; i++) {
    	jlongArray column = (jlongArray) env->GetObjectArrayElement(rawDataObj, i);
    	nRows = env->GetArrayLength(column);
    	jboolean isCopy;
    	rawData[i] = env->GetLongArrayElements(column, &isCopy);
    	anyCopies |= isCopy;
    }


    // TODO: process query data
    if (anyCopies) {
		for (jint i = 0; i < cols; i++) {
			jlongArray column = (jlongArray) env->GetObjectArrayElement(rawDataObj, i);
			env->ReleaseLongArrayElements(column, rawData[i], JNI_ABORT);
		}
    }
    delete rawData;

}
jobject getResult(JNIEnv* env, jobject self) {
	return NULL;
}
