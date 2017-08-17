cmake_minimum_required(VERSION 3.4.1)

add_definitions(-DANDROID )
include_directories(src/main/native/crypto)

find_library( log-lib
              log )

add_library( edsxts
			 SHARED
             src/main/native/xts/edsxts.c )

target_link_libraries( edsxts                       
                       ${log-lib} )

add_library( edsaes
			 SHARED
             src/main/native/aes/edsaes.c 
             src/main/native/aes/aescrypt.c 
             src/main/native/aes/aeskey.c 
             src/main/native/aes/aestab.c )

target_link_libraries( edsaes
                       ${log-lib} )

add_library( edsserpent
			 SHARED
             src/main/native/serpent/edsserpent.c
             src/main/native/serpent/serpent.c )

target_link_libraries( edsserpent
                       ${log-lib} )

add_library( edstwofish
			 SHARED
             src/main/native/twofish/edstwofish.c
             src/main/native/twofish/twofish.c )

add_library( edsripemd160
			 SHARED
             src/main/native/ripemd160/edsripemd160.c )

target_link_libraries( edsripemd160
                       ${log-lib} )

add_library( edswhirlpool
			 SHARED
             src/main/native/whirlpool/edswhirlpool.c
             src/main/native/whirlpool/whirlpool.c )

target_link_libraries( edswhirlpool
                       ${log-lib} )

add_library( fdraio
			 SHARED
             src/main/native/fdraio/fdraio.c )

target_compile_definitions( fdraio
        					PRIVATE _FILE_OFFSET_BITS=64 )

target_link_libraries( fdraio
                       ${log-lib} )

add_library( localxts
			 SHARED
             src/main/native/localxts/localxts.c )

target_compile_definitions( localxts
        					PRIVATE _FILE_OFFSET_BITS=64 )

target_link_libraries( localxts
                       edsxts
                       ${log-lib} )

add_library( edsctr
       SHARED
             src/main/native/ctr/edsctr.c )

target_link_libraries( edsctr
                       ${log-lib} )

add_library( edscbc
       SHARED
             src/main/native/cbc/edscbc.c )

target_link_libraries( edscbc
                       ${log-lib} )

add_library( edscfb
       SHARED
             src/main/native/cfb/edscfb.c )

target_link_libraries( edscfb
                       ${log-lib} )
add_library( edsgost
       SHARED
             src/main/native/gost/edsgost.c
             src/main/native/gost/gost89.c )

include(CMakeLists-full.txt)
include(CMakeLists-fsm.txt)
