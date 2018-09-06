
cmake_minimum_required(VERSION 3.4.1)


# TODO 添加第三方库
# TODO add_library(libavcodec-57
# TODO 原先生成的.so文件在编译后会自动添加上前缀lib和后缀.so，
# TODO       在定义库的名字时，不要加前缀lib和后缀 .so，
# TODO       不然会报错:java.lang.UnsatisfiedLinkError: Couldn't load xxx : findLibrary returned null
add_library(ArcSoft_FDEngine
            # TODO STATIC表示静态的.a的库，SHARED表示.so的库
            SHARED
            IMPORTED)

set_target_properties(ArcSoft_FDEngine
                      PROPERTIES IMPORTED_LOCATION
                      # TODO ${CMAKE_SOURCE_DIR}：表示 CMakeLists.txt的当前文件夹路径
                      # TODO ${ANDROID_ABI}：编译时会自动根据 CPU架构去选择相应的库
                      # TODO ABI文件夹上面不要再分层，直接就 jniLibs/${ANDROID_ABI}/
                      # TODO ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_FDEngine.so
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_FDEngine.so)

add_library(mpbase
            SHARED
            IMPORTED)

set_target_properties(mpbase
                      PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libmpbase.so)

add_library(ArcSoft_AEEngine
            SHARED
            IMPORTED)

set_target_properties(ArcSoft_AEEngine
                      PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_AEEngine.so)


add_library(ArcSoft_GEEngine
            SHARED
            IMPORTED)

set_target_properties(ArcSoft_GEEngine
                      PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_GEEngine.so)


add_library(ArcSoft_FREngine
            SHARED
            IMPORTED)

set_target_properties(ArcSoft_FREngine
                      PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_FREngine.so)


add_library(ArcSoft_FTEngine
            SHARED
            IMPORTED)

set_target_properties(ArcSoft_FTEngine
                      PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libArcSoft_FTEngine.so)