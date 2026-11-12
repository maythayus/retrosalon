package fr.retrosalon

import android.os.Build

/**
 * Architecture d'Android sur cette TV : elle décide quels émulateurs peuvent s'y installer.
 * Un processeur 64 bits peut faire tourner un Android 32 bits (Freebox Pop) : seul compte ce que
 * le système accepte.
 */
data class TvArch(
    val android: String,
    val is64: Boolean,
    val family: Family,
    val primaryAbi: String,
    /** Applis ARM 64 bits acceptées : faux sur un x86_64 sans traduction ARM 64 bits. */
    val arm64: Boolean,
) {
    enum class Family { ARM, X86, OTHER }

    companion object {
        fun current(): TvArch =
            from(Build.VERSION.RELEASE.orEmpty(), Build.SUPPORTED_ABIS.toList(), Build.SUPPORTED_64_BIT_ABIS.toList())

        /**
         * [abis] : ABI acceptées par le système, la préférée d'abord ; [abis64] : celles en 64 bits,
         * vide sur un Android 32 bits.
         */
        fun from(android: String, abis: List<String>, abis64: List<String>): TvArch {
            // La première ABI est celle du processeur ; les suivantes peuvent être émulées
            // (ARM sur un x86, par exemple).
            val primary = abis.firstOrNull().orEmpty()
            val family = when {
                primary.startsWith("arm") -> Family.ARM
                primary.startsWith("x86") -> Family.X86
                else -> Family.OTHER
            }
            return TvArch(android, abis64.isNotEmpty(), family, primary, arm64 = "arm64-v8a" in abis64)
        }
    }
}
