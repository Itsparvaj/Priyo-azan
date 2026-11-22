# Mosque Azan Audio & Full Quran JSON Guide

## 1. Bundled Mosque Azan Recordings (`app/src/main/res/raw/`)

Priyo Azan ships offline with real human Muezzin Azan recordings in MP3 format under Public Domain / CC0 licensing:
- `azan_mosque_classic.mp3` (2:34) — CC0 1.0 Public Domain (Wikimedia Commons)
- `azan_mosque_levant.mp3` (2:38) — Public Domain (Wikimedia Commons)
- `azan_gentle_chime.wav` (0:03) — Short notification chime

Users can also import any personal `.mp3`, `.ogg`, or `.m4a` Azan recording directly from their Android device inside **Settings -> Automatic Azan & Audio -> Import Custom MP3** (zero storage permissions required via Android Storage Access Framework).

## 2. Full Offline Quran Dataset (`app/src/main/assets/quran/quran_dataset.json`)

Priyo Azan bundles the complete 114-Surah metadata index and 6 verified Surahs (Al-Fatihah, Al-Asr, Al-Kawthar, Al-Ikhlas, Al-Falaq, An-Nas). To unlock all 6,236 Ayahs offline, place a verified Tanzil-derived JSON file at `app/src/main/assets/quran/quran_dataset.json`:

```json
{
  "ayahs": [
    {
      "surah": 2,
      "ayah": 1,
      "arabic": "الم",
      "english": "Alif, Lam, Meem.",
      "bengali": "আলিফ-লাম-মীম।"
    }
  ]
}
```
