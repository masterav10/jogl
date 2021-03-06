#! /bin/sh

export NODE_LABEL=.

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
export HOST_IP=10.1.0.122
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=panda02
#export TARGET_IP=jautab03
#export TARGET_IP=jauphone04
export TARGET_ADB_PORT=5555
# needs executable bit (probably su)
export TARGET_ROOT=/data/projects
export TARGET_ANT_HOME=/usr/share/ant

echo ANDROID_HOME $ANDROID_HOME
echo NDK_ROOT $NDK_ROOT

if [ -z "$NDK_ROOT" ] ; then
    #
    # Generic android-ndk
    #
    if [ -e /usr/local/android-ndk ] ; then
        NDK_ROOT=/usr/local/android-ndk
    elif [ -e /opt-linux-x86/android-ndk ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk
    elif [ -e /opt/android-ndk ] ; then
        NDK_ROOT=/opt/android-ndk
    #
    # Specific android-ndk-r7b
    #
    elif [ -e /usr/local/android-ndk-r7b ] ; then
        NDK_ROOT=/usr/local/android-ndk-r7b
    elif [ -e /opt-linux-x86/android-ndk-r7b ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk-r7b
    elif [ -e /opt/android-ndk-r7b ] ; then
        NDK_ROOT=/opt/android-ndk-r7b
    else 
        echo NDK_ROOT is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $NDK_ROOT ] ; then
    echo NDK_ROOT $NDK_ROOT does not exist
    exit 1
fi
export NDK_ROOT

if [ -z "$ANDROID_HOME" ] ; then
    if [ -e /usr/local/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/usr/local/android-sdk-linux_x86
    elif [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    elif [ -e /opt/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/opt/android-sdk-linux_x86
    else 
        echo ANDROID_HOME is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $ANDROID_HOME ] ; then
    echo ANDROID_HOME $ANDROID_HOME does not exist
    exit 1
fi
export ANDROID_HOME

export ANDROID_VERSION=9
export SOURCE_LEVEL=1.6
export TARGET_LEVEL=1.6
export TARGET_RT_JAR=/opt-share/jre1.6.0_30/lib/rt.jar

export GCC_VERSION=4.4.3
HOST_ARCH=linux-x86
export TARGET_TRIPLE=arm-linux-androideabi

export NDK_TOOLCHAIN_ROOT=$NDK_ROOT/toolchains/${TARGET_TRIPLE}-${GCC_VERSION}/prebuilt/${HOST_ARCH}
export TARGET_PLATFORM_ROOT=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-arm

# Need to add toolchain bins to the PATH. 
export PATH="$NDK_TOOLCHAIN_ROOT/$TARGET_TRIPLE/bin:$ANDROID_HOME/platform-tools:$PATH"

export GLUEGEN_CPPTASKS_FILE=`pwd`/../../gluegen/make/lib/gluegen-cpptasks-android-armv7.xml

#export JUNIT_DISABLED="true"
#export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

# BUILD_ARCHIVE=true \
ant \
    -Drootrel.build=build-android-armv7 \
    $* 2>&1 | tee -a make.jogl.all.android-armv7-cross.log

