cmake_minimum_required(VERSION 3.16)
project(sparkle C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/bin)


include_directories(Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small)
include_directories(Implementations/crypto_aead/schwaemm128128v2/add_armv7m_fast)

add_executable(schwaemmC
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/api.h
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/crypto_aead.h
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/encrypt.c
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/genkat_aead.c
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/schwaemm_cfg.h
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_fast/sparkle_opt.c
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_fast/sparkle_opt.h)

add_executable(sparkleC
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/sparkle_opt.c
        Implementations/crypto_aead/schwaemm128128v2/add_armv7m_small/sparkle_opt.h)
