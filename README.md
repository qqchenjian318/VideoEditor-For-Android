# VideoEditor-For-Android
一个Android的视频编辑器，包括了视频录制、剪切、增加bgm、美白、加滤镜、加水印等多种功能

基于android硬编码的视频编辑器，不支持4.3以下系统，通过android的api完成视频采集，通过OpenGL，完成视频数据帧的处理，通过android的硬编码器MeidaCodec
对采集到的视频流进行硬编码。
利用OpenGL完成视频的美白、加滤镜、加水印等功能。利用MediaCodec完成音视频的分离和音频的一些混音处理
