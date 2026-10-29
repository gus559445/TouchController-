cmake_minimum_required(VERSION 3.10)
project(TouchController-Proxy-Windows)

set(CMAKE_CXX_STANDARD 20)

add_subdirectory(${CMAKE_SOURCE_DIR}/../proxy-common proxy-common.out)

include(CheckCXXCompilerFlag)
check_cxx_compiler_flag(-Wno-ignored-attributes HAVE_NO_IGNORE_ATTRIBUTE)

find_package(JNI REQUIRED)
find_library(WS2_32_LIBRARY ws2_32)
find_library(WINRT_LIBRARY windowsapp)

add_library(proxy_windows SHARED "src/lib.cpp" "src/lib.hpp" "src/input.cpp" "src/input.hpp" )
target_include_directories(proxy_windows PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(proxy_windows PRIVATE ws2_32 proxy_protocol windowsapp)
target_link_options(proxy_windows PRIVATE -Wl,-Bstatic -lc++ -lunwind -Wl,-Bdynamic)
# Target Windows 10.
target_compile_definitions(proxy_windows PRIVATE -DWIN32_LEAN_AND_MEAN -DWINVER=0x0A00 -D_WIN32_WINNT=0x0A00)

add_library(proxy_windows_legacy SHARED "src/lib-legacy.cpp" "src/lib-legacy.hpp" "src/input.cpp" "src/input.hpp")
target_include_directories(proxy_windows_legacy PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(proxy_windows_legacy PRIVATE ws2_32 proxy_protocol)
target_link_options(proxy_windows_legacy PRIVATE -Wl,-Bstatic -lc++ -lunwind -Wl,-Bdynamic)
# Target Windows 7.
target_compile_definitions(proxy_windows_legacy PRIVATE -DWIN32_LEAN_AND_MEAN -DWINVER=0x0601 -D_WIN32_WINNT=0x0601)

if (HAVE_NO_IGNORE_ATTRIBUTE)
    target_compile_options(proxy_windows PRIVATE -Wno-ignored-attributes)
    target_compile_options(proxy_windows_legacy PRIVATE -Wno-ignored-attributes)
endif (HAVE_NO_IGNORE_ATTRIBUTE)
