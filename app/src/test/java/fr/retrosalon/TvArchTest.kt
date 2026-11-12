package fr.retrosalon

import fr.retrosalon.TvArch.Family
import org.junit.Assert.assertEquals
import org.junit.Test

class TvArchTest {
    @Test
    fun freeboxPopRunsA32BitAndroidOnArm() {
        // Processeur 64 bits, mais Android limité au 32 bits.
        assertEquals(
            TvArch("10", is64 = false, Family.ARM, "armeabi-v7a", arm64 = false),
            TvArch.from("10", listOf("armeabi-v7a", "armeabi"), emptyList()),
        )
    }

    @Test
    fun arm64TvWithOrWithout32BitSupport() {
        assertEquals(
            TvArch("12", is64 = true, Family.ARM, "arm64-v8a", arm64 = true),
            TvArch.from("12", listOf("arm64-v8a", "armeabi-v7a", "armeabi"), listOf("arm64-v8a")),
        )
        assertEquals(
            TvArch("14", is64 = true, Family.ARM, "arm64-v8a", arm64 = true),
            TvArch.from("14", listOf("arm64-v8a"), listOf("arm64-v8a")),
        )
    }

    @Test
    fun x86IsRecognisedEvenWhenArmIsEmulated() {
        assertEquals(
            TvArch("15", is64 = true, Family.X86, "x86_64", arm64 = true),
            TvArch.from("15", listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a"), listOf("x86_64", "arm64-v8a")),
        )
        assertEquals(
            TvArch("9", is64 = false, Family.X86, "x86", arm64 = false),
            TvArch.from("9", listOf("x86", "armeabi-v7a"), emptyList()),
        )
    }

    @Test
    fun x86_64WithoutArm64TranslationCantInstallArmOnlyEmulators() {
        // Android-x86 ou ChromeOS : traduction ARM 32 bits seulement.
        assertEquals(
            TvArch("11", is64 = true, Family.X86, "x86_64", arm64 = false),
            TvArch.from("11", listOf("x86_64", "x86", "armeabi-v7a", "armeabi"), listOf("x86_64")),
        )
    }

    @Test
    fun unknownProcessorKeepsItsName() {
        assertEquals(
            TvArch("15", is64 = true, Family.OTHER, "riscv64", arm64 = false),
            TvArch.from("15", listOf("riscv64"), listOf("riscv64")),
        )
        assertEquals(TvArch("", is64 = false, Family.OTHER, "", arm64 = false), TvArch.from("", emptyList(), emptyList()))
    }
}
