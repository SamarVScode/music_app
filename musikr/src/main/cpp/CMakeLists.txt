# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("tagJNI") # becomes "libtagJNI.so"

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static taglib" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "Disable testing" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "Disable examples" FORCE)
set(BUILD_BINDINGS OFF CACHE BOOL "Disable bindings" FORCE)
set(WITH_ZLIB OFF CACHE BOOL "Disable zlib" FORCE)
set(WITH_APE OFF CACHE BOOL "Disable ape" FORCE)
set(WITH_ASF OFF CACHE BOOL "Disable asf" FORCE)
set(WITH_MOD OFF CACHE BOOL "Disable mod" FORCE)
set(WITH_SHORTEN OFF CACHE BOOL "Disable shorten" FORCE)
set(WITH_TRUEAUDIO OFF CACHE BOOL "Disable trueaudio" FORCE)
set(VISIBILITY_HIDDEN ON CACHE BOOL "Hidden visibility" FORCE)

add_subdirectory(taglib)
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        taglib_jni.cpp
        JInputStream.cpp
        JTagMap.cpp
        JMetadataBuilder.cpp
        JClassRef.cpp
        JObjectRef.cpp
        JStringRef.cpp
        JByteArrayRef.cpp
)
target_link_options(${CMAKE_PROJECT_NAME}
        # @Tolriq found that these flags can reduce the size of the linked
        # taglib + jni shim shared library. Kudos to them.
        # https://github.com/taglib/taglib/issues/1212#issuecomment-2326456903
        # Additionally, enable 16kb page size. I believe taglib can support this fine,
        # as a cursory glance indicates that it doesn't hardcode any page sizes.
        PRIVATE "-Wl,--exclude-libs,ALL,-z,max-page-size=16384")

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/toolkit"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/ape"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/asf"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/flac"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/it"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mp4"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpc"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v1"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v2"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/mpeg/id3v2/frames"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/ogg"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/riff"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/s3m"
        "${CMAKE_CURRENT_SOURCE_DIR}/taglib/taglib/xm"
        "${CMAKE_CURRENT_BINARY_DIR}/taglib"
)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        PRIVATE android
        PRIVATE log
        PRIVATE tag)
