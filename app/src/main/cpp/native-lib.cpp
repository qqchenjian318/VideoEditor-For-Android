#include <jni.h>
#include <string>

extern "C"{
jbyteArray
Java_com_example_cj_videoeditor_jni_AudioJniUtils_audioMix(JNIEnv *env, jclass type, jbyteArray sourceA_,
                                                           jbyteArray sourceB_, jbyteArray dst_, jfloat firstVol,
                                                           jfloat secondVol) {
    jbyte *sourceA = env->GetByteArrayElements(sourceA_, NULL);
    jbyte *sourceB = env->GetByteArrayElements(sourceB_, NULL);
    jbyte *dst = env->GetByteArrayElements(dst_, NULL);
    //归一化混音
    int aL = env->GetArrayLength(sourceA_);
    int bL = env->GetArrayLength(sourceB_);
    int row = aL / 2;
    short sA[row];
    for (int i = 0; i < row; ++i) {
        sA[i] = (short) ((sourceA[i * 2] & 0xff) | (sourceA[i * 2 + 1] & 0xff) << 8);
    }

    short sB[row];
    for (int i = 0; i < row; ++i) {
        sB[i] = (short) ((sourceB[i * 2] & 0xff) | (sourceB[i * 2 + 1] & 0xff) << 8);
    }
    short result[row];
    for (int i = 0; i < row; ++i) {
        int a = (int) (sA[i] * firstVol);
        int b = (int) (sB[i] * secondVol);
        if (a < 0 && b < 0){
            int  i1 = a + b - a * b / (-32768);
            if (i1 > 32768){
                result[i] =  32767;
            } else if (i1 < - 32768){
                result[i] = - 32768;
            } else{
                result[i] = (short) i1;
            }
        } else if (a > 0 && b > 0){
            int i1 = a + b - a  * b  / 32767;
            if (i1 > 32767){
                result[i] = 32767;
            }else if (i1 < - 32768){
                result[i] = -32768;
            }else {
                result[i] = (short) i1;
            }
        } else{
            int i1 = a + b ;
            if (i1 > 32767){
                result[i] = 32767;
            }else if (i1 < - 32768){
                result[i] = -32768;
            }else {
                result[i] = (short) i1;
            }
        }
    }
    for (int i = 0; i <row ; ++i) {
        dst[i * 2 + 1] = (jbyte) ((result[i] & 0xFF00) >> 8);
        dst[i * 2] = (jbyte) (result[i] & 0x00FF);
    }

    jbyteArray result1 = env ->NewByteArray(aL);
    env->SetByteArrayRegion(result1, 0, aL, dst);

    env->ReleaseByteArrayElements(sourceA_, sourceA, 0);
    env->ReleaseByteArrayElements(sourceB_, sourceB, 0);
    env->ReleaseByteArrayElements(dst_, dst, 0);

    return result1;
}

}

