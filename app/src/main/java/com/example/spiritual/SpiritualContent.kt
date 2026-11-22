package com.example.spiritual

import java.time.Duration
import java.time.ZonedDateTime

data class DailyMilestones(
    val sehriEnd: ZonedDateTime,
    val ishraq: ZonedDateTime,
    val zawal: ZonedDateTime,
    val iftar: ZonedDateTime,
    val tahajjudStart: ZonedDateTime
)

data class MasnoonDua(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val titleAr: String,
    val arabic: String,
    val transliteration: String,
    val meaningEn: String,
    val meaningBn: String,
    val reference: String
)

data class DailyVerse(
    val arabic: String,
    val meaningEn: String,
    val meaningBn: String,
    val reference: String
)

data class DhikrPreset(
    val id: String,
    val arabic: String,
    val titleEn: String,
    val titleBn: String,
    val defaultTarget: Int
)

object SpiritualContent {

    val dhikrPresets: List<DhikrPreset> = listOf(
        DhikrPreset("subhanallah", "سُبْحَانَ اللَّهِ", "SubhanAllah", "সুবহানাল্লাহ", 33),
        DhikrPreset("alhamdulillah", "الْحَمْدُ لِلَّهِ", "Alhamdulillah", "আলহামদুলিল্লাহ", 33),
        DhikrPreset("allahuakbar", "اللَّهُ أَكْبَرُ", "Allahu Akbar", "আল্লাহু আকবার", 34),
        DhikrPreset("istighfar", "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah", "আস্তাগফিরুল্লাহ", 100),
        DhikrPreset("tahlil", "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "লা ইলাহা ইল্লাল্লাহ", 100),
        DhikrPreset("tasbih_fatimi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "SubhanAllahi wa bihamdihi", "সুবহানাল্লাহি ওয়া বিহামদিহি", 100)
    )

    val masnoonDuas: List<MasnoonDua> = listOf(
        MasnoonDua(
            id = "after_azan",
            titleEn = "Dua After Azan",
            titleBn = "আযানের পরের দোয়া",
            titleAr = "الدعاء بعد الأذان",
            arabic = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            transliteration = "Allahumma rabba hadhihid-da'watit-tammah, was-salatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah, wab'ath-hu maqaman mahmudanil-ladhi wa'adtah.",
            meaningEn = "O Allah, Lord of this perfect call and established prayer, grant Muhammad the intercession and favor, and raise him to the honored station You have promised him.",
            meaningBn = "হে আল্লাহ! এই পরিপূর্ণ আহ্বান এবং প্রতিষ্ঠিত নামাজের প্রতিপালক! হযরত মুহাম্মদ (সা.)-কে ওসিলা ও সর্বোচ্চ মর্যাদা দান করুন এবং তাঁকে সেই প্রশংসিত স্থানে অধিষ্ঠিত করুন, যার প্রতিশ্রুতি আপনি তাঁকে দিয়েছেন।",
            reference = "Sahih al-Bukhari 614"
        ),
        MasnoonDua(
            id = "entering_mosque",
            titleEn = "Entering the Mosque",
            titleBn = "মসজিদে প্রবেশের দোয়া",
            titleAr = "دعاء دخول المسجد",
            arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Allahummaf-tah li abwaba rahmatik.",
            meaningEn = "O Allah, open for me the gates of Your mercy.",
            meaningBn = "হে আল্লাহ! আমার জন্য আপনার রহমতের দরজাসমূহ খুলে দিন।",
            reference = "Sahih Muslim 713"
        ),
        MasnoonDua(
            id = "leaving_mosque",
            titleEn = "Leaving the Mosque",
            titleBn = "মসজিদ থেকে বের হওয়ার দোয়া",
            titleAr = "دعاء الخروج من المسجد",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Allahumma inni as'aluka min fadlik.",
            meaningEn = "O Allah, indeed I ask You of Your bounty.",
            meaningBn = "হে আল্লাহ! নিশ্চয়ই আমি আপনার অনুগ্রহ প্রার্থনা করছি।",
            reference = "Sahih Muslim 713"
        ),
        MasnoonDua(
            id = "iftar_dua",
            titleEn = "Upon Breaking the Fast (Iftar)",
            titleBn = "ইফতারের দোয়া",
            titleAr = "دعاء الإفطار",
            arabic = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            transliteration = "Dhahabaz-zama'u wabtallatil-'uruqu wa thabatal-ajru in sha'Allah.",
            meaningEn = "The thirst has gone, the veins are quenched, and the reward is assured, if Allah wills.",
            meaningBn = "পিপাসা দূর হয়েছে, শিরা-উপশিরা সিক্ত হয়েছে এবং ইনশাআল্লাহ সওয়াব নির্ধারিত হয়েছে।",
            reference = "Sunan Abi Dawud 2357"
        ),
        MasnoonDua(
            id = "morning_evening_tawakkul",
            titleEn = "Sufficiency & Peace of Heart",
            titleBn = "প্রশান্তি ও আল্লাহর ওপর ভরসার দোয়া",
            titleAr = "حسبي الله",
            arabic = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            transliteration = "Hasbiyallahu la ilaha illa Huwa, 'alayhi tawakkaltu wa Huwa Rabbul-'Arshil-'Azim.",
            meaningEn = "Allah is sufficient for me; there is no deity except Him. On Him I rely, and He is the Lord of the Mighty Throne.",
            meaningBn = "আমার জন্য আল্লাহই যথেষ্ট, তিনি ব্যতীত কোনো উপাস্য নেই। তাঁরই ওপর আমি ভরসা করি এবং তিনি মহান আরশের অধিপতি।",
            reference = "Quran 9:129"
        ),
        MasnoonDua(
            id = "parents_dua",
            titleEn = "Dua for Parents",
            titleBn = "পিতা-মাতার জন্য দোয়া",
            titleAr = "الدعاء للوالدين",
            arabic = "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            transliteration = "Rabbir-hamhuma kama rabbayani saghira.",
            meaningEn = "My Lord, have mercy upon them both as they brought me up when I was small.",
            meaningBn = "হে আমার প্রতিপালক! তাঁদের উভয়ের প্রতি দয়া করুন, যেভাবে তাঁরা শৈশবে আমাকে লালন-পালন করেছেন।",
            reference = "Quran 17:24"
        ),
        MasnoonDua(
            id = "knowledge_dua",
            titleEn = "Seeking Beneficial Knowledge",
            titleBn = "কল্যাণকর জ্ঞান বৃদ্ধির দোয়া",
            titleAr = "دعاء طلب العلم",
            arabic = "رَّبِّ زِدْنِي عِلْمًا",
            transliteration = "Rabbi zidni 'ilma.",
            meaningEn = "My Lord, increase me in knowledge.",
            meaningBn = "হে আমার প্রতিপালক! আমার জ্ঞান বৃদ্ধি করে দিন।",
            reference = "Quran 20:114"
        )
    )

    val dailyVerses: List<DailyVerse> = listOf(
        DailyVerse(
            arabic = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            meaningEn = "Unquestionably, by the remembrance of Allah hearts are assured.",
            meaningBn = "জেনে রেখো, আল্লাহর স্মরণেই অন্তরসমূহ প্রশান্ত হয়।",
            reference = "Surah Ar-Ra'd • 13:28"
        ),
        DailyVerse(
            arabic = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            meaningEn = "For indeed, with hardship comes ease. Indeed, with hardship comes ease.",
            meaningBn = "নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে। নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে।",
            reference = "Surah Ash-Sharh • 94:5-6"
        ),
        DailyVerse(
            arabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            meaningEn = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            meaningBn = "অতএব তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করব। এবং আমার কৃতজ্ঞতা প্রকাশ করো।",
            reference = "Surah Al-Baqarah • 2:152"
        ),
        DailyVerse(
            arabic = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            meaningEn = "Allah does not charge a soul except [with that within] its capacity.",
            meaningBn = "আল্লাহ কোনো প্রাণের ওপর তার সাধ্যের অতিরিক্ত দায়িত্ব চাপিয়ে দেন না।",
            reference = "Surah Al-Baqarah • 2:286"
        ),
        DailyVerse(
            arabic = "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ",
            meaningEn = "And seek help through patience and prayer.",
            meaningBn = "এবং ধৈর্য ও সালাতের মাধ্যমে সাহায্য প্রার্থনা করো।",
            reference = "Surah Al-Baqarah • 2:45"
        )
    )

    fun verseForDayOfYear(dayOfYear: Int): DailyVerse {
        return dailyVerses[(dayOfYear.coerceAtLeast(0)) % dailyVerses.size]
    }

    fun computeDailyMilestones(
        fajr: ZonedDateTime,
        sunrise: ZonedDateTime,
        dhuhr: ZonedDateTime,
        maghrib: ZonedDateTime,
        nextFajr: ZonedDateTime
    ): DailyMilestones {
        val nightSeconds = Duration.between(maghrib, nextFajr).seconds.coerceAtLeast(0L)
        return DailyMilestones(
            sehriEnd = fajr,
            ishraq = sunrise.plusMinutes(15),
            zawal = dhuhr.minusMinutes(10),
            iftar = maghrib,
            tahajjudStart = maghrib.plusSeconds((nightSeconds * 2L) / 3L)
        )
    }
}
