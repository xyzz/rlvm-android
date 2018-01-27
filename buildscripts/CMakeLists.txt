cmake_minimum_required(VERSION 3.2.0)

set(RLVM_TAG android)

set(BOOST_VERSION 1.66.0)
set(BOOST_HASH SHA256=5721818253e6a0989583192f96782c4a98eb6204965316df9f5ad75819225ca9)

set(OGG_VERSION 1.3.3)
set(OGG_HASH SHA256=4f3fc6178a533d392064f14776b23c397ed4b9f48f5de297aba73b643f955c08)

set(VORBIS_VERSION 1.3.5)
set(VORBIS_HASH SHA256=54f94a9527ff0a88477be0a71c0bab09a4c3febe0ed878b24824906cd4b0e1d1)

set(SNDFILE_VERSION 1.0.28)
set(SNDFILE_HASH SHA256=1ff33929f042fa333aed1e8923aa628c3ee9e1eb85512686c55092d1e5a9dfa9)

set(SDL2_VERSION 2.0.7)
set(SDL2_HASH SHA256=ee35c74c4313e2eda104b14b1b86f7db84a04eeab9430d56e001cea268bf4d5e)

set(FREETYPE2_VERSION 2.9)
set(FREETYPE2_HASH SHA256=bf380e4d7c4f3b5b1c1a7b2bf3abb967bda5e9ab480d0df656e0e08c5019c5e6)

set(SDL2_TTF_VERSION 2.0.14)
set(SDL2_TTF_HASH SHA256=34db5e20bcf64e7071fe9ae25acaa7d72bdc4f11ab3ce59acc768ab62fe39276)

set(SDL2_MIXER_VERSION 2.0.2)
set(SDL2_MIXER_HASH SHA256=4e615e27efca4f439df9af6aa2c6de84150d17cbfd12174b54868c12f19c83bb)

set(SDL2_IMAGE_VERSION 2.0.2)
set(SDL2_IMAGE_HASH SHA256=72df075aef91fc4585098ea7e0b072d416ec7599aa10473719fbe51e9b8f6ce8)

set(GL4ES_VERSION dd8cc4e7a379a7c479edefc63e5fa40be3eba7ff)
set(GL4ES_HASH SHA256=fc75caec65cffdb8d434e3a3dc9eea3dc4081661c2751354025d528b3bab670e)

# End of configurable options

project(rlvm-android)

include(ExternalProject)

set(wrapper_command ${CMAKE_BINARY_DIR}/command_wrapper.sh)
set(ndk_triplet ${NDK_TRIPLET})
set(download_dir ${CMAKE_SOURCE_DIR}/downloads/)
set(prefix ${CMAKE_INSTALL_PREFIX})
set(host ${ndk_triplet})
set(android_platform android-${ANDROID_API})
set(app_abi ${ABI})

set(COMMON_CMAKE_ARGS
	-DCMAKE_TOOLCHAIN_FILE=${CMAKE_SOURCE_DIR}/toolchain/ndk/build/cmake/android.toolchain.cmake
	-DCMAKE_INSTALL_PREFIX=${prefix}
	-DCMAKE_FIND_ROOT_PATH=${prefix}
	-DANDROID_ABI=${app_abi}
	-DANDROID_PLATFORM=${android_platform}
	-DANDROID_STL=c++_shared
	-DANDROID_CPP_FEATURES=rtti\ exceptions
	-DCMAKE_C_FLAGS=$ENV{CFLAGS}
	-DCMAKE_CXX_FLAGS=$ENV{CXXFLAGS}
	-DCMAKE_SHARED_LINKER_FLAGS=$ENV{LDFLAGS}
)

set(COMMON_NDK_BUILD_FLAGS
	NDK_PROJECT_PATH=.
	APP_BUILD_SCRIPT=<SOURCE_DIR>/Android.mk
	APP_PLATFORM=${android_platform}
	APP_ABI=${app_abi}
	LOCAL_LDFLAGS=$ENV{LDFLAGS}
	-j4
)

set(COMMON_AUTOCONF_FLAGS
	--enable-static
	--disable-shared
	--prefix=${prefix}
)

# ffmpeg doesn't like --host, so sandwich it between adding --host to the flags
set(ffmpeg_configure_flags ${COMMON_AUTOCONF_FLAGS})
if (${BUILD_TYPE} STREQUAL "debug")
	# ffmpeg fails to compile if we have asm enabled and optimizations disabled, so disable both
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm --disable-optimizations)
endif()

if ((${ARCH} STREQUAL "x86_64") OR (${ARCH} STREQUAL "x86"))
	# ffmpeg fails to link with assembly optimizations on x86-64
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm)
endif()

set(COMMON_AUTOCONF_FLAGS
	--host=${host}
	${COMMON_AUTOCONF_FLAGS}
)

if (${ARCH} STREQUAL "arm")
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --enable-thumb)
endif()

# on x86 enabling simd results in text relocations, which are banned on Android
set(libjpeg_turbo_flags "")
if (${ARCH} STREQUAL "x86")
	set(libjpeg_turbo_flags ${libjpeg_turbo_flags} --without-simd)
endif()


## dependencies

ExternalProject_Add(ogg
	URL http://downloads.xiph.org/releases/ogg/libogg-${OGG_VERSION}.tar.xz
	URL_HASH ${OGG_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(vorbis
	DEPENDS ogg

	URL http://downloads.xiph.org/releases/vorbis/libvorbis-${VORBIS_VERSION}.tar.xz
	URL_HASH ${VORBIS_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(sndfile # TODO: depends on vorbis etc?
	URL http://www.mega-nerd.com/libsndfile/files/libsndfile-${SNDFILE_VERSION}.tar.gz
	URL_HASH ${SNDFILE_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

string(REPLACE "." "_" BOOST_VERSION_UNDERSCORE ${BOOST_VERSION})
ExternalProject_Add(boost
	URL https://dl.bintray.com/boostorg/release/${BOOST_VERSION}/source/boost_${BOOST_VERSION_UNDERSCORE}.tar.bz2
	URL_HASH ${BOOST_HASH}
	DOWNLOAD_DIR ${download_dir}

	BUILD_IN_SOURCE 1

	PATCH_COMMAND patch -d <SOURCE_DIR> -p2 -t -N < ${CMAKE_SOURCE_DIR}/patches/boost-fix13172.diff

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/bootstrap.sh
	--prefix=${prefix}

	BUILD_COMMAND ""

	INSTALL_COMMAND ${wrapper_command} ./b2
	-j4
	--with-program_options
	--with-serialization
	--with-iostreams
	--with-filesystem
	--with-date_time
	--with-thread
	--with-system
	toolset=clang-android
	architecture=${BOOST_ARCH}
	variant=release
	target-os=android
	threading=multi
	threadapi=pthread
	link=static
	runtime-link=static
	install
)

ExternalProject_Add(sdl2
	URL https://www.libsdl.org/release/SDL2-${SDL2_VERSION}.tar.gz
	URL_HASH ${SDL2_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}

	INSTALL_COMMAND mkdir -p ${prefix}/lib/ ${prefix}/include/
	COMMAND cp libs/${app_abi}/libSDL2.so ${prefix}/lib/
	COMMAND cp -r <SOURCE_DIR>/include/ ${prefix}/include/SDL2
)

ExternalProject_Add(freetype2
	URL https://download.savannah.gnu.org/releases/freetype/freetype-${FREETYPE2_VERSION}.tar.gz
	URL_HASH ${FREETYPE2_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}
	--with-png=no

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(sdl2_ttf
	DEPENDS sdl2 freetype2

	URL https://www.libsdl.org/projects/SDL_ttf/release/SDL2_ttf-${SDL2_TTF_VERSION}.tar.gz
	URL_HASH ${SDL2_TTF_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND rm -rf <SOURCE_DIR>/external # remove bundled freetype

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}
	LOCAL_C_INCLUDES=${prefix}/include/freetype2/\ ${prefix}/include/SDL2/
	FREETYPE_LIBRARY_PATH=
	LOCAL_SHARED_LIBRARIES=

	INSTALL_COMMAND mkdir -p ${prefix}/lib/ ${prefix}/include/SDL2/
	COMMAND cp libs/${app_abi}/libSDL2_ttf.so ${prefix}/lib/
	COMMAND cp <SOURCE_DIR>/SDL_ttf.h ${prefix}/include/SDL2/
)

ExternalProject_Add(sdl2_mixer
	DEPENDS sdl2

	URL https://www.libsdl.org/projects/SDL_mixer/release/SDL2_mixer-${SDL2_MIXER_VERSION}.tar.gz
	URL_HASH ${SDL2_MIXER_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}
	LOCAL_C_INCLUDES=${prefix}/include/SDL2/
	LOCAL_SHARED_LIBRARIES=
	SUPPORT_FLAC=false
	SUPPORT_OGG=false
	SUPPORT_MP3_MPG123=false
	SUPPORT_MOD_MODPLUG=false
	SUPPORT_TIMIDITY=false

	INSTALL_COMMAND mkdir -p ${prefix}/lib/ ${prefix}/include/SDL2/
	COMMAND cp libs/${app_abi}/libSDL2_mixer.so ${prefix}/lib/
	COMMAND cp <SOURCE_DIR>/SDL_mixer.h ${prefix}/include/SDL2/
)

ExternalProject_Add(sdl2_image
	DEPENDS sdl2

	URL https://www.libsdl.org/projects/SDL_image/release/SDL2_image-${SDL2_IMAGE_VERSION}.tar.gz
	URL_HASH ${SDL2_IMAGE_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND touch <SOURCE_DIR>/IMG_WIC.c

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}
	LOCAL_C_INCLUDES=${prefix}/include/SDL2/
	LOCAL_SHARED_LIBRARIES=
	SUPPORT_JPG=false
	SUPPORT_PNG=false
	SUPPORT_WEBP=false

	INSTALL_COMMAND mkdir -p ${prefix}/lib/ ${prefix}/include/SDL2/
	COMMAND cp libs/${app_abi}/libSDL2_image.so ${prefix}/lib/
	COMMAND cp <SOURCE_DIR>/SDL_image.h ${prefix}/include/SDL2/
)

ExternalProject_Add(gl4es
	URL https://github.com/ptitSeb/gl4es/archive/${GL4ES_VERSION}.tar.gz
	URL_HASH ${GL4ES_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/gl4es-build-shared.patch

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}

	INSTALL_COMMAND mkdir -p ${prefix}/lib/
	COMMAND cp libs/${app_abi}/libGL.so ${prefix}/lib/
	COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

## main project

ExternalProject_Add(rlvm
	DEPENDS boost ogg vorbis sndfile sdl2 freetype2 sdl2_ttf sdl2_mixer sdl2_image gl4es

	GIT_REPOSITORY https://github.com/xyzz/rlvm.git
	GIT_TAG ${RLVM_TAG}

	BUILD_IN_SOURCE 1

	PATCH_COMMAND rm -rf <SOURCE_DIR>/vendor/{GLEW,SDL_image,SDL_mixer,SDL_ttf}

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} scons VERBOSE=1 --release --android

	INSTALL_COMMAND cp build/release/libgame.so ${prefix}/lib
)
