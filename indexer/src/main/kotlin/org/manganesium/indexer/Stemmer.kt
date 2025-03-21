package org.manganesium.indexer

import java.util.*


internal class NewString {
    var str: String = ""
}

class Porter {
    private fun Clean(str: String): String {
        val last = str.length

        val ch = Character.valueOf(str[0])
        var temp = ""

        for (i in 0 until last) {
            if (Character.isLetterOrDigit(str[i])) temp += str[i]
        }

        return temp
    } //clean

    private fun hasSuffix(word: String, suffix: String, stem: NewString): Boolean {
        var tmp = ""

        if (word.length <= suffix.length) return false
        if (suffix.length > 1) if (word[word.length - 2] != suffix[suffix.length - 2]) return false

        stem.str = ""

        for (i in 0 until word.length - suffix.length) stem.str += word[i]
        tmp = stem.str

        for (i in 0 until suffix.length) tmp += suffix[i]

        return if (tmp.compareTo(word) == 0) true
        else false
    }

    private fun vowel(ch: Char, prev: Char): Boolean {
        return when (ch) {
            'a', 'e', 'i', 'o', 'u' -> true
            'y' -> {
                when (prev) {
                    'a', 'e', 'i', 'o', 'u' -> false

                    else -> true
                }
            }

            else -> false
        }
    }

    private fun measure(stem: String): Int {
        var i = 0
        var count = 0
        val length = stem.length

        while (i < length) {
            while (i < length) {
                if (i > 0) {
                    if (vowel(stem[i], stem[i - 1])) break
                } else {
                    if (vowel(stem[i], 'a')) break
                }
                i++
            }

            i++
            while (i < length) {
                if (i > 0) {
                    if (!vowel(stem[i], stem[i - 1])) break
                } else {
                    if (!vowel(stem[i], '?')) break
                }
                i++
            }
            if (i < length) {
                count++
                i++
            }
        } //while


        return (count)
    }

    private fun containsVowel(word: String): Boolean {
        for (i in 0 until word.length) if (i > 0) {
            if (vowel(word[i], word[i - 1])) return true
        } else {
            if (vowel(word[0], 'a')) return true
        }

        return false
    }

    private fun cvc(str: String): Boolean {
        val length = str.length

        if (length < 3) return false

        if ((!vowel(str[length - 1], str[length - 2]))
            && (str[length - 1] != 'w') && (str[length - 1] != 'x') && (str[length - 1] != 'y')
            && (vowel(str[length - 2], str[length - 3]))
        ) {
            return if (length == 3) {
                if (!vowel(str[0], '?')) true
                else false
            } else {
                if (!vowel(str[length - 3], str[length - 4])) true
                else false
            }
        }

        return false
    }

    private fun step1(str: String): String {
        var str = str
        val stem = NewString()

        if (str[str.length - 1] == 's') {
            if ((hasSuffix(str, "sses", stem)) || (hasSuffix(str, "ies", stem))) {
                var tmp = ""
                for (i in 0 until str.length - 2) tmp += str[i]
                str = tmp
            } else {
                if ((str.length == 1) && (str[str.length - 1] == 's')) {
                    str = ""
                    return str
                }
                if (str[str.length - 2] != 's') {
                    var tmp = ""
                    for (i in 0 until str.length - 1) tmp += str[i]
                    str = tmp
                }
            }
        }

        if (hasSuffix(str, "eed", stem)) {
            if (measure(stem.str) > 0) {
                var tmp = ""
                for (i in 0 until str.length - 1) tmp += str[i]
                str = tmp
            }
        } else {
            if ((hasSuffix(str, "ed", stem)) || (hasSuffix(str, "ing", stem))) {
                if (containsVowel(stem.str)) {
                    var tmp = ""
                    for (i in 0 until stem.str.length) tmp += str[i]
                    str = tmp
                    if (str.length == 1) return str

                    if ((hasSuffix(str, "at", stem)) || (hasSuffix(str, "bl", stem)) || (hasSuffix(str, "iz", stem))) {
                        str += "e"
                    } else {
                        val length = str.length
                        if ((str[length - 1] == str[length - 2])
                            && (str[length - 1] != 'l') && (str[length - 1] != 's') && (str[length - 1] != 'z')
                        ) {
                            tmp = ""
                            for (i in 0 until str.length - 1) tmp += str[i]
                            str = tmp
                        } else if (measure(str) == 1) {
                            if (cvc(str)) str += "e"
                        }
                    }
                }
            }
        }

        if (hasSuffix(str, "y", stem)) if (containsVowel(stem.str)) {
            var tmp = ""
            for (i in 0 until str.length - 1) tmp += str[i]
            str = tmp + "i"
        }
        return str
    }

    private fun step2(str: String): String {
        var str = str
        val suffixes = arrayOf(
            arrayOf("ational", "ate"),
            arrayOf("tional", "tion"),
            arrayOf("enci", "ence"),
            arrayOf("anci", "ance"),
            arrayOf("izer", "ize"),
            arrayOf("iser", "ize"),
            arrayOf("abli", "able"),
            arrayOf("alli", "al"),
            arrayOf("entli", "ent"),
            arrayOf("eli", "e"),
            arrayOf("ousli", "ous"),
            arrayOf("ization", "ize"),
            arrayOf("isation", "ize"),
            arrayOf("ation", "ate"),
            arrayOf("ator", "ate"),
            arrayOf("alism", "al"),
            arrayOf("iveness", "ive"),
            arrayOf("fulness", "ful"),
            arrayOf("ousness", "ous"),
            arrayOf("aliti", "al"),
            arrayOf("iviti", "ive"),
            arrayOf("biliti", "ble")
        )
        val stem = NewString()


        for (index in suffixes.indices) {
            if (hasSuffix(str, suffixes[index][0], stem)) {
                if (measure(stem.str) > 0) {
                    str = stem.str + suffixes[index][1]
                    return str
                }
            }
        }

        return str
    }

    private fun step3(str: String): String {
        var str = str
        val suffixes = arrayOf(
            arrayOf("icate", "ic"),
            arrayOf("ative", ""),
            arrayOf("alize", "al"),
            arrayOf("alise", "al"),
            arrayOf("iciti", "ic"),
            arrayOf("ical", "ic"),
            arrayOf("ful", ""),
            arrayOf("ness", "")
        )
        val stem = NewString()

        for (index in suffixes.indices) {
            if (hasSuffix(str, suffixes[index][0], stem)) if (measure(stem.str) > 0) {
                str = stem.str + suffixes[index][1]
                return str
            }
        }
        return str
    }

    private fun step4(str: String): String {
        var str = str
        val suffixes = arrayOf(
            "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment", "ent", "sion", "tion",
            "ou", "ism", "ate", "iti", "ous", "ive", "ize", "ise"
        )

        val stem = NewString()

        for (index in suffixes.indices) {
            if (hasSuffix(str, suffixes[index], stem)) {
                if (measure(stem.str) > 1) {
                    str = stem.str
                    return str
                }
            }
        }
        return str
    }

    private fun step5(str: String): String {
        var str = str
        if (str[str.length - 1] == 'e') {
            if (measure(str) > 1) { /* measure(str)==measure(stem) if ends in vowel */
                var tmp = ""
                for (i in 0 until str.length - 1) tmp += str[i]
                str = tmp
            } else if (measure(str) == 1) {
                var stem = ""
                for (i in 0 until str.length - 1) stem += str[i]

                if (!cvc(stem)) str = stem
            }
        }

        if (str.length == 1) return str
        if ((str[str.length - 1] == 'l') && (str[str.length - 2] == 'l') && (measure(str) > 1)) if (measure(str) > 1) { /* measure(str)==measure(stem) if ends in vowel */
            var tmp = ""
            for (i in 0 until str.length - 1) tmp += str[i]
            str = tmp
        }
        return str
    }

    private fun stripPrefixes(str: String): String {
        val prefixes = arrayOf("kilo", "micro", "milli", "intra", "ultra", "mega", "nano", "pico", "pseudo")

        val last = prefixes.size
        for (i in 0 until last) {
            if (str.startsWith(prefixes[i])) {
                var temp = ""
                for (j in 0 until str.length - prefixes[i].length) temp += str[j + prefixes[i].length]
                return temp
            }
        }

        return str
    }


    private fun stripSuffixes(str: String): String {
        var str = str
        str = step1(str)
        if (str.length >= 1) str = step2(str)
        if (str.length >= 1) str = step3(str)
        if (str.length >= 1) str = step4(str)
        if (str.length >= 1) str = step5(str)

        return str
    }


    fun stripAffixes(str: String): String {
        var str = str
        str = str.lowercase(Locale.getDefault())
        str = Clean(str)

        if ((str !== "") && (str.length > 2)) {
            str = stripPrefixes(str)

            if (str !== "") str = stripSuffixes(str)
        }

        return str
    } //stripAffixes
} //class
