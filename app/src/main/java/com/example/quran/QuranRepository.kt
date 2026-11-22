package com.example.quran

import android.content.Context
import com.example.model.AyahItem
import com.example.model.SurahInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline Quran module providing the complete 114-Surah metadata catalog,
 * verified built-in Arabic + English + Bengali texts for essential short Surahs,
 * and an asset JSON importer (`assets/quran/quran_dataset.json`) for loading a full
 * verified Tanzil-format dataset completely offline.
 */
class QuranRepository(private val context: Context) {

    private var cachedImportedAyahs: Map<Int, List<AyahItem>>? = null

    fun getAllSurahs(): List<SurahInfo> = surahCatalog

    fun getAyahsForSurah(surahNumber: Int): List<AyahItem> {
        val imported = loadImportedDataset()[surahNumber]
        if (!imported.isNullOrEmpty()) return imported
        return builtInVerifiedAyahs[surahNumber].orEmpty()
    }

    fun hasAyahContent(surahNumber: Int): Boolean =
        getAyahsForSurah(surahNumber).isNotEmpty()

    fun search(query: String): Pair<List<SurahInfo>, List<AyahItem>> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return surahCatalog to emptyList()
        val matchedSurahs = surahCatalog.filter {
            it.englishName.lowercase().contains(q) ||
                it.bengaliName.lowercase().contains(q) ||
                it.arabicName.contains(q) ||
                it.number.toString() == q
        }
        val allAyahs = (builtInVerifiedAyahs.values.flatten() + loadImportedDataset().values.flatten())
            .distinctBy { "${it.surahNumber}:${it.ayahNumber}" }
        val matchedAyahs = allAyahs.filter {
            it.arabicText.contains(q) ||
                it.englishTranslation.lowercase().contains(q) ||
                it.bengaliTranslation.lowercase().contains(q)
        }
        return matchedSurahs to matchedAyahs
    }

    private fun loadImportedDataset(): Map<Int, List<AyahItem>> {
        cachedImportedAyahs?.let { return it }
        val parsed = runCatching {
            context.assets.open("quran/quran_dataset.json").bufferedReader().use { reader ->
                val root = JSONObject(reader.readText())
                val ayahsArray: JSONArray = root.getJSONArray("ayahs")
                val grouped = mutableMapOf<Int, MutableList<AyahItem>>()
                for (i in 0 until ayahsArray.length()) {
                    val obj = ayahsArray.getJSONObject(i)
                    val item = AyahItem(
                        surahNumber = obj.getInt("surah"),
                        ayahNumber = obj.getInt("ayah"),
                        arabicText = obj.getString("arabic"),
                        englishTranslation = obj.optString("english", ""),
                        bengaliTranslation = obj.optString("bengali", "")
                    )
                    grouped.getOrPut(item.surahNumber) { mutableListOf() }.add(item)
                }
                grouped.mapValues { (_, list) -> list.sortedBy { it.ayahNumber } }
            }
        }.getOrElse { emptyMap() }
        cachedImportedAyahs = parsed
        return parsed
    }

    companion object {
        private val builtInVerifiedAyahs: Map<Int, List<AyahItem>> = mapOf(
            1 to listOf(
                AyahItem(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে শুরু করছি।"),
                AyahItem(1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds -", "সমস্ত প্রশংসা জগতসমূহের প্রতিপালক আল্লাহর জন্য।"),
                AyahItem(1, 3, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,", "যিনি পরম করুণাময়, অসীম দয়ালু।"),
                AyahItem(1, 4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense.", "বিচার দিবসের মালিক।"),
                AyahItem(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "আমরা কেবল আপনারই ইবাদত করি এবং কেবল আপনারই সাহায্য প্রার্থনা করি।"),
                AyahItem(1, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -", "আমাদেরকে সরল পথ প্রদর্শন করুন।"),
                AyahItem(1, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", "তাদের পথ, যাদেরকে আপনি অনুগ্রহ করেছেন; তাদের পথ নয়, যারা অভিশপ্ত ও পথভ্রষ্ট হয়েছে।")
            ),
            103 to listOf(
                AyahItem(103, 1, "وَالْعَصْرِ", "By time,", "মহাকালের শপথ,"),
                AyahItem(103, 2, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "Indeed, mankind is in loss,", "নিশ্চয়ই মানুষ ক্ষতির মধ্যে রয়েছে,"),
                AyahItem(103, 3, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ", "Except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience.", "তবে তারা ব্যতীত, যারা ঈমান এনেছে, সৎকর্ম করেছে এবং পরস্পরকে সত্য ও ধৈর্যের উপদেশ দিয়েছে।")
            ),
            108 to listOf(
                AyahItem(108, 1, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "নিশ্চয়ই আমি আপনাকে কাওসার দান করেছি।"),
                AyahItem(108, 2, "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "So pray to your Lord and sacrifice [to Him alone].", "অতএব আপনার প্রতিপালকের উদ্দেশ্যে সালাত আদায় করুন এবং কুরবানি করুন।"),
                AyahItem(108, 3, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "Indeed, your enemy is the one cut off.", "নিশ্চয়ই আপনার বিদ্বেষীই নির্বংশ।")
            ),
            112 to listOf(
                AyahItem(112, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, \"He is Allah, [who is] One,", "বলুন, তিনি আল্লাহ, এক ও অদ্বিতীয়।"),
                AyahItem(112, 2, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge.", "আল্লাহ অমুখাপেক্ষী।"),
                AyahItem(112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,", "তিনি কাউকে জন্ম দেননি এবং কারো থেকে জন্ম নেননি।"),
                AyahItem(112, 4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "Nor is there to Him any equivalent.\"", "এবং তাঁর সমতুল্য কেউ নেই।")
            ),
            113 to listOf(
                AyahItem(113, 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Say, \"I seek refuge in the Lord of daybreak", "বলুন, আমি আশ্রয় প্রার্থনা করছি ভোরের প্রতিপালকের নিকট,"),
                AyahItem(113, 2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created", "তিনি যা সৃষ্টি করেছেন তার অনিষ্ট হতে,"),
                AyahItem(113, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles", "এবং অন্ধকার রাতের অনিষ্ট হতে যখন তা গভীর হয়,"),
                AyahItem(113, 4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "And from the evil of the blowers in knots", "এবং গিরায় ফুঁকদানকারী নারীদের অনিষ্ট হতে,"),
                AyahItem(113, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.\"", "এবং হিংসুকের অনিষ্ট হতে যখন সে হিংসা করে।")
            ),
            114 to listOf(
                AyahItem(114, 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Say, \"I seek refuge in the Lord of mankind,", "বলুন, আমি আশ্রয় প্রার্থনা করছি মানুষের প্রতিপালকের নিকট,"),
                AyahItem(114, 2, "مَلِكِ النَّاسِ", "The Sovereign of mankind.", "মানুষের অধিপতির নিকট,"),
                AyahItem(114, 3, "إِلَٰهِ النَّاسِ", "The God of mankind,", "মানুষের ইলাহের নিকট,"),
                AyahItem(114, 4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "From the evil of the retreating whisperer -", "আত্মগোপনকারী কুমন্ত্রণাদাতার অনিষ্ট হতে,"),
                AyahItem(114, 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Who whispers [evil] into the breasts of mankind -", "যে মানুষের অন্তরে কুমন্ত্রণা দেয়,"),
                AyahItem(114, 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "From among the jinn and mankind.\"", "জিন ও মানুষের মধ্য হতে।")
            )
        )

        val surahCatalog: List<SurahInfo> = listOf(
            SurahInfo(1, "الفاتحة", "Al-Fatihah", "আল-ফাতিহা", "Meccan", 7),
            SurahInfo(2, "البقرة", "Al-Baqarah", "আল-বাকারা", "Medinan", 286),
            SurahInfo(3, "آل عمران", "Ali 'Imran", "আলে ইমরান", "Medinan", 200),
            SurahInfo(4, "النساء", "An-Nisa", "আন-নিসা", "Medinan", 176),
            SurahInfo(5, "المائدة", "Al-Ma'idah", "আল-মায়িদা", "Medinan", 120),
            SurahInfo(6, "الأنعام", "Al-An'am", "আল-আনআম", "Meccan", 165),
            SurahInfo(7, "الأعراف", "Al-A'raf", "আল-আরাফ", "Meccan", 206),
            SurahInfo(8, "الأنفال", "Al-Anfal", "আল-আনফাল", "Medinan", 75),
            SurahInfo(9, "التوبة", "At-Tawbah", "আত-তাওবাহ", "Medinan", 129),
            SurahInfo(10, "يونس", "Yunus", "ইউনুস", "Meccan", 109),
            SurahInfo(11, "هود", "Hud", "হুদ", "Meccan", 123),
            SurahInfo(12, "يوسف", "Yusuf", "ইউসুফ", "Meccan", 111),
            SurahInfo(13, "الرعد", "Ar-Ra'd", "আর-রাদ", "Medinan", 43),
            SurahInfo(14, "ابراهيم", "Ibrahim", "ইব্রাহিম", "Meccan", 52),
            SurahInfo(15, "الحجر", "Al-Hijr", "আল-হিজর", "Meccan", 99),
            SurahInfo(16, "النحل", "An-Nahl", "আন-নাহল", "Meccan", 128),
            SurahInfo(17, "الإسراء", "Al-Isra", "আল-ইসরা", "Meccan", 111),
            SurahInfo(18, "الكهف", "Al-Kahf", "আল-কাহফ", "Meccan", 110),
            SurahInfo(19, "مريم", "Maryam", "মারইয়াম", "Meccan", 98),
            SurahInfo(20, "طه", "Taha", "ত্বা-হা", "Meccan", 135),
            SurahInfo(21, "الأنبياء", "Al-Anbya", "আল-আম্বিয়া", "Meccan", 112),
            SurahInfo(22, "الحج", "Al-Hajj", "আল-হাজ্জ", "Medinan", 78),
            SurahInfo(23, "المؤمنون", "Al-Mu'minun", "আল-মুমিনুন", "Meccan", 118),
            SurahInfo(24, "النور", "An-Nur", "আন-নূর", "Medinan", 64),
            SurahInfo(25, "الفرقان", "Al-Furqan", "আল-ফুরকান", "Meccan", 77),
            SurahInfo(26, "الشعراء", "Ash-Shu'ara", "আশ-শুআরা", "Meccan", 227),
            SurahInfo(27, "النمل", "An-Naml", "আন-নামল", "Meccan", 93),
            SurahInfo(28, "القصص", "Al-Qasas", "আল-কাসাস", "Meccan", 88),
            SurahInfo(29, "العنكبوت", "Al-'Ankabut", "আল-আনকাবুত", "Meccan", 69),
            SurahInfo(30, "الروم", "Ar-Rum", "আর-রূম", "Meccan", 60),
            SurahInfo(31, "لقمان", "Luqman", "লুকমান", "Meccan", 34),
            SurahInfo(32, "السجدة", "As-Sajdah", "আস-সাজদাহ", "Meccan", 30),
            SurahInfo(33, "الأحزاب", "Al-Ahzab", "আল-আহযাব", "Medinan", 73),
            SurahInfo(34, "سبإ", "Saba", "সাবা", "Meccan", 54),
            SurahInfo(35, "فاطر", "Fatir", "ফাতির", "Meccan", 45),
            SurahInfo(36, "يس", "Ya-Sin", "ইয়াসিন", "Meccan", 83),
            SurahInfo(37, "الصافات", "As-Saffat", "আস-সাফফাত", "Meccan", 182),
            SurahInfo(38, "ص", "Sad", "সোয়াদ", "Meccan", 88),
            SurahInfo(39, "الزمر", "Az-Zumar", "আয-যুমার", "Meccan", 75),
            SurahInfo(40, "غافر", "Ghafir", "গাফির", "Meccan", 85),
            SurahInfo(41, "فصلت", "Fussilat", "ফুসসিলাত", "Meccan", 54),
            SurahInfo(42, "الشورى", "Ash-Shuraa", "আশ-শূরা", "Meccan", 53),
            SurahInfo(43, "الزخرف", "Az-Zukhruf", "আয-যুখরুফ", "Meccan", 89),
            SurahInfo(44, "الدخان", "Ad-Dukhan", "আদ-দুখান", "Meccan", 59),
            SurahInfo(45, "الجاثية", "Al-Jathiyah", "আল-জাসিয়াহ", "Meccan", 37),
            SurahInfo(46, "الأحقاف", "Al-Ahqaf", "আল-আহকাফ", "Meccan", 35),
            SurahInfo(47, "محمد", "Muhammad", "মুহাম্মদ", "Medinan", 38),
            SurahInfo(48, "الفتح", "Al-Fath", "আল-ফাতহ", "Medinan", 29),
            SurahInfo(49, "الحجرات", "Al-Hujurat", "আল-হুজুরাত", "Medinan", 18),
            SurahInfo(50, "ق", "Qaf", "ক্বাফ", "Meccan", 45),
            SurahInfo(51, "الذاريات", "Adh-Dhariyat", "আয-যারিয়াত", "Meccan", 60),
            SurahInfo(52, "الطور", "At-Tur", "আত-তূর", "Meccan", 49),
            SurahInfo(53, "النجم", "An-Najm", "আন-নাজম", "Meccan", 62),
            SurahInfo(54, "القمر", "Al-Qamar", "আল-ক্বামার", "Meccan", 55),
            SurahInfo(55, "الرحمن", "Ar-Rahman", "আর-রহমান", "Medinan", 78),
            SurahInfo(56, "الواقعة", "Al-Waqi'ah", "আল-ওয়াকিয়াহ", "Meccan", 96),
            SurahInfo(57, "الحديد", "Al-Hadid", "আল-হাদীদ", "Medinan", 29),
            SurahInfo(58, "المجادلة", "Al-Mujadila", "আল-মুজাদালাহ", "Medinan", 22),
            SurahInfo(59, "الحشر", "Al-Hashr", "আল-হাশর", "Medinan", 24),
            SurahInfo(60, "الممتحنة", "Al-Mumtahanah", "আল-মুমতাহিনাহ", "Medinan", 13),
            SurahInfo(61, "الصف", "As-Saf", "আস-সাফ", "Medinan", 14),
            SurahInfo(62, "الجمعة", "Al-Jumu'ah", "আল-জুমুআহ", "Medinan", 11),
            SurahInfo(63, "المنافقون", "Al-Munafiqun", "আল-মুনাফিকুন", "Medinan", 11),
            SurahInfo(64, "التغابن", "At-Taghabun", "আত-তাগাবুন", "Medinan", 18),
            SurahInfo(65, "الطلاق", "At-Talaq", "আত-তালাক", "Medinan", 12),
            SurahInfo(66, "التحريم", "At-Tahrim", "আত-তাহরীম", "Medinan", 12),
            SurahInfo(67, "الملك", "Al-Mulk", "আল-মুলক", "Meccan", 30),
            SurahInfo(68, "القلم", "Al-Qalam", "আল-ক্বালাম", "Meccan", 52),
            SurahInfo(69, "الحاقة", "Al-Haqqah", "আল-হাক্কাহ", "Meccan", 52),
            SurahInfo(70, "المعارج", "Al-Ma'arij", "আল-মাআরিজ", "Meccan", 44),
            SurahInfo(71, "نوح", "Nuh", "নূহ", "Meccan", 28),
            SurahInfo(72, "الجن", "Al-Jinn", "আল-জিন", "Meccan", 28),
            SurahInfo(73, "المزمل", "Al-Muzzammil", "আল-মুযযাম্মিল", "Meccan", 20),
            SurahInfo(74, "المدثر", "Al-Muddaththir", "আল-মুদ্দাসসির", "Meccan", 56),
            SurahInfo(75, "القيامة", "Al-Qiyamah", "আল-কিয়ামাহ", "Meccan", 40),
            SurahInfo(76, "الانسان", "Al-Insan", "আল-ইনসান", "Medinan", 31),
            SurahInfo(77, "المرسلات", "Al-Mursalat", "আল-মুরসালাত", "Meccan", 50),
            SurahInfo(78, "النبإ", "An-Naba", "আন-নাবা", "Meccan", 40),
            SurahInfo(79, "النازعات", "An-Nazi'at", "আন-নাযিয়াত", "Meccan", 46),
            SurahInfo(80, "عبس", "'Abasa", "আবাসা", "Meccan", 42),
            SurahInfo(81, "التكوير", "At-Takwir", "আত-তাকভীর", "Meccan", 29),
            SurahInfo(82, "الإنفطار", "Al-Infitar", "আল-ইনফিতার", "Meccan", 19),
            SurahInfo(83, "المطففين", "Al-Mutaffifin", "আল-মুতাফফিফীন", "Meccan", 36),
            SurahInfo(84, "الإنشقاق", "Al-Inshiqaq", "আল-ইনশিক্বাক", "Meccan", 25),
            SurahInfo(85, "البروج", "Al-Buruj", "আল-বুরুজ", "Meccan", 22),
            SurahInfo(86, "الطارق", "At-Tariq", "আত-তারিক", "Meccan", 17),
            SurahInfo(87, "الأعلى", "Al-A'la", "আল-আলা", "Meccan", 19),
            SurahInfo(88, "الغاشية", "Al-Ghashiyah", "আল-গাশিয়াহ", "Meccan", 26),
            SurahInfo(89, "الفجر", "Al-Fajr", "আল-ফাজর", "Meccan", 30),
            SurahInfo(90, "البلد", "Al-Balad", "আল-বালাদ", "Meccan", 20),
            SurahInfo(91, "الشمس", "Ash-Shams", "আশ-শামস", "Meccan", 15),
            SurahInfo(92, "الليل", "Al-Layl", "আল-লাইল", "Meccan", 21),
            SurahInfo(93, "الضحى", "Ad-Duhaa", "আদ-দুহা", "Meccan", 11),
            SurahInfo(94, "الشرح", "Ash-Sharh", "আশ-শারহ", "Meccan", 8),
            SurahInfo(95, "التين", "At-Tin", "আত-তীন", "Meccan", 8),
            SurahInfo(96, "العلق", "Al-'Alaq", "আল-আলাক", "Meccan", 19),
            SurahInfo(97, "القدر", "Al-Qadr", "আল-ক্বদর", "Meccan", 5),
            SurahInfo(98, "البينة", "Al-Bayyinah", "আল-বাইয়্যিনাহ", "Medinan", 8),
            SurahInfo(99, "الزلزلة", "Az-Zalzalah", "আয-যিলযাল", "Medinan", 8),
            SurahInfo(100, "العاديات", "Al-'Adiyat", "আল-আদিয়াত", "Meccan", 11),
            SurahInfo(101, "القارعة", "Al-Qari'ah", "আল-ক্বারিয়াহ", "Meccan", 11),
            SurahInfo(102, "التكاثر", "At-Takathur", "আত-তাকাসুর", "Meccan", 8),
            SurahInfo(103, "العصر", "Al-'Asr", "আল-আসর", "Meccan", 3),
            SurahInfo(104, "الهمزة", "Al-Humazah", "আল-হুমাযাহ", "Meccan", 9),
            SurahInfo(105, "الفيل", "Al-Fil", "আল-ফীল", "Meccan", 5),
            SurahInfo(106, "قريش", "Quraysh", "কুরাইশ", "Meccan", 4),
            SurahInfo(107, "الماعون", "Al-Ma'un", "আল-মাউন", "Meccan", 7),
            SurahInfo(108, "الكوثر", "Al-Kawthar", "আল-কাওসার", "Meccan", 3),
            SurahInfo(109, "الكافرون", "Al-Kafirun", "আল-কাফিরুন", "Meccan", 6),
            SurahInfo(110, "النصر", "An-Nasr", "আন-নাসর", "Medinan", 3),
            SurahInfo(111, "المسد", "Al-Masad", "আল-মাসাদ", "Meccan", 5),
            SurahInfo(112, "الإخلاص", "Al-Ikhlas", "আল-ইখলাস", "Meccan", 4),
            SurahInfo(113, "الفلق", "Al-Falaq", "আল-ফালাক", "Meccan", 5),
            SurahInfo(114, "الناس", "An-Nas", "আন-নাস", "Meccan", 6)
        )
    }
}
