package com.example.data

import com.example.model.Artist
import com.example.model.Playlist
import com.example.model.Song

object SampleData {
    val songs = listOf(
        // NEW RELEASES & 24H TRENDING SPOTLIGHT HITS
        Song(
            id = "new_rel_1",
            videoId = "rlR4PJn8b8I",
            title = "Tauba Tauba",
            artist = "Karan Aujla, Badshah",
            album = "Bad Newz",
            artworkUrl = "https://c.saavncdn.com/138/Tauba-Tauba-From-Bad-Newz-Hindi-2024-20240702133444-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/992/5d44da8bc1d78fb72d18b701d758fd1f_320.mp4",
            durationMs = 207000L,
            source = "Online",
            genre = "Bollywood",
            isNewRelease = true,
            lyrics = "[00:10.00]Husn tera tauba tauba\n[00:15.00]Nakhre tere tauba tauba\n[00:20.00]Pehle hi dil kho baitha\n[00:25.00]Hun ki main karan tauba tauba"
        ),
        Song(
            id = "new_rel_2",
            videoId = "XO8wew38VM8",
            title = "Millionaire",
            artist = "Yo Yo Honey Singh",
            album = "Glory",
            artworkUrl = "https://c.saavncdn.com/712/GLORY-Hindi-2024-20240826131018-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/173/af61d1cf779677cd767692c0a7f3301f_320.mp4",
            durationMs = 198000L,
            source = "Online",
            genre = "Punjabi",
            isNewRelease = true,
            lyrics = "[00:12.00]Bande hum millionaire\n[00:18.00]Gaadiyan sab top gear\n[00:24.00]Honey Singh on the beat\n[00:30.00]Kare duniya pe repeat"
        ),
        Song(
            id = "new_rel_3",
            videoId = "k3g_WjLCsXM",
            title = "O Sajni Re",
            artist = "Arijit Singh, Sachin-Jigar",
            album = "Laapataa Ladies",
            artworkUrl = "https://c.saavncdn.com/835/O-Sajni-Re-From-Laapataa-Ladies-Hindi-2024-20240212181027-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/179/dbbf3ed3c678c23ceb4d4e149ff61213_320.mp4",
            durationMs = 170000L,
            source = "Online",
            genre = "Bollywood",
            isNewRelease = true,
            lyrics = "[00:14.00]O sajni re kaise kate din raat\n[00:22.00]Tore bina jiya laage na\n[00:30.00]Dard yeh dil ka baatein na\n[00:38.00]O sajni re tore sang ho prabhat"
        ),
        Song(
            id = "new_rel_4",
            videoId = "tOM-nWPcR4U",
            title = "Illuminati",
            artist = "Sushin Shyam, Dabzee",
            album = "Aavesham",
            artworkUrl = "https://c.saavncdn.com/791/Aavesham-Malayalam-2024-20240411130607-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/202/ba6006006a2f40e6b20b5ced32cc2885_320.mp4",
            durationMs = 193000L,
            source = "Online",
            genre = "South",
            isNewRelease = true,
            lyrics = "[00:12.00]Ranga ranga mass aane\n[00:18.00]Illuminati beat drop aane\n[00:24.00]Aavesham peak time aane"
        ),
        Song(
            id = "new_rel_5",
            videoId = "hOHKltAiKXQ",
            title = "Big Dawgs",
            artist = "Hanumankind, Kalmi",
            album = "Big Dawgs",
            artworkUrl = "https://c.saavncdn.com/479/Big-Dawgs-English-2024-20240710001019-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/172/d274e7cd67653c97c0d391f754afcfdf_320.mp4",
            durationMs = 234000L,
            source = "Online",
            genre = "English",
            isNewRelease = true,
            lyrics = "[00:10.00]Wait a minute get it how you live it\n[00:15.00]Ten toes in when we standing in business\n[00:20.00]Big dawgs barking in the yard\n[00:25.00]Never let down the guard"
        ),
        Song(
            id = "new_rel_6",
            videoId = "m4mOeaWfQh4",
            title = "Chuttamalle",
            artist = "Shilpa Rao, Anirudh Ravichander",
            album = "Devara Part 1",
            artworkUrl = "https://c.saavncdn.com/177/Chuttamalle-From-Devara-Part-1-Telugu-2024-20240805174005-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/313/e49e604945889f330e5b3536dd0ff524_320.mp4",
            durationMs = 221000L,
            source = "Online",
            genre = "Tamil",
            isNewRelease = true,
            lyrics = "[00:15.00]Chuttamalle chuttukove thullintha gaali\n[00:23.00]Kallaloki choosukove allanthaga\n[00:30.00]Anirudh magic in the air"
        ),
        Song(
            id = "new_rel_7",
            videoId = "7Q0s-R51o5M",
            title = "Systumm 2",
            artist = "Masoom Sharma, DG Immortals",
            album = "Systumm 2",
            artworkUrl = "https://c.saavncdn.com/923/Systumm-Hindi-2023-20230522053412-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/923/e3dfe6105da87481cbe056823c761465_96.mp4",
            durationMs = 215000L,
            source = "Online",
            genre = "Haryanvi",
            isNewRelease = true,
            lyrics = "[00:12.00]Systumm paad denge chhore haryana aale\n[00:20.00]Gaddi kali chamke rohtak ke mod pe\n[00:28.00]Masoom sharma ka danka baje"
        ),

        // 1. Arijit Singh
        Song(
            id = "ind_1",
            videoId = "fHI8X480mxo",
            title = "Kesariya",
            artist = "Pritam, Arijit Singh, Amitabh Bhattacharya",
            album = "Brahmastra",
            artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_96.mp4",
            durationMs = 268000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:16.00]Mujhko itna bataye koyi\n[00:23.00]Kaise tujhse dil na lagaye koyi\n[00:30.00]Rabba ne tujhko banane mein\n[00:37.00]Kardi hai husn ki khaali tijoriyan\n[00:45.00]Kesariya tera ishq hai piya\n[01:01.00]Rang jaaun jo main haath lagaaun"
        ),
        // 2. Renuka Panwar & Pranjal Dahiya
        Song(
            id = "ind_2",
            videoId = "CMy6A_hR3qA",
            title = "52 Gaj Ka Daman",
            artist = "Renuka Panwar, Pranjal Dahiya, Mj",
            album = "52 Gaj Ka Daman",
            artworkUrl = "https://c.saavncdn.com/407/52-Gaj-Ka-Daman-Haryanvi-2020-20210625170012-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/407/d8b1de34b18920d7715033dbf8c93f47_96.mp4",
            durationMs = 163000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Aapna roop sajaaun main\n[00:18.00]Ghunghat khol ke aaoon main\n[00:24.00]52 gaj ka daman pehar matak chalungi\n[00:32.00]52 gaj ka daman pehar matak chalungi"
        ),
        // 3. Sidhu Moose Wala
        Song(
            id = "ind_3",
            videoId = "n_FCrCQ6-bM",
            title = "295",
            artist = "Sidhu Moose Wala",
            album = "Moosetape",
            artworkUrl = "https://c.saavncdn.com/609/Moosetape-Punjabi-2021-20260626155141-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/609/852628435c98083dfe217c1cfa731bb5_96.mp4",
            durationMs = 270000L,
            source = "Online",
            genre = "Punjabi",
            lyrics = "[00:15.00]Dass kihton kihton sach lukavega\n[00:20.00]Aithe 295 lag jau\n[00:25.00]Je sach boleyan taan jail paunge\n[00:30.00]Je jhooth boleyan taan star banaunge"
        ),
        // 4. Diljit Dosanjh
        Song(
            id = "ind_4",
            videoId = "mH_LFkWxpI0",
            title = "Lover",
            artist = "Diljit Dosanjh",
            album = "MoonChild Era",
            artworkUrl = "https://c.saavncdn.com/209/MoonChild-Era-Punjabi-2021-20240715073449-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/209/88cd9a1cc0af8768d67272876bb09851_96.mp4",
            durationMs = 190000L,
            source = "Online",
            genre = "Punjabi",
            lyrics = "[00:10.00]Jihna tera karda main koi kar nayi sakda\n[00:15.00]Tainu chhad ke kise hor te mar nayi sakda\n[00:20.00]Tera lover haan main lover\n[00:25.00]Tera lover haan main lover"
        ),
        // 5. Karan Aujla
        Song(
            id = "ind_5",
            videoId = "cWMxCE2HTag",
            title = "Winning Speech",
            artist = "Karan Aujla, MXRCI, Seshnolan",
            album = "Winning Speech",
            artworkUrl = "https://c.saavncdn.com/089/Winning-Speech-Punjabi-2024-20260626013220-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/089/64beffa430e4c948223ec6bfcc3a13f0_96.mp4",
            durationMs = 227000L,
            source = "Online",
            genre = "Punjabi",
            isNewRelease = true,
            lyrics = "[00:08.00]Listen to me softly\n[00:12.00]Kade kade gusse vich boldi ae\n[00:16.00]Par dil vich bheda koyi rakhdi ni\n[00:21.00]Jad takdi ae akhan sek lendi"
        ),
        // 6. Masoom Sharma
        Song(
            id = "ind_6",
            videoId = "7Q0s-R51o5M",
            title = "KALESHI CHORI",
            artist = "Masoom Sharma, DG IMMORTALS, Raga, Harjas Harjaayi",
            album = "Systumm",
            artworkUrl = "https://c.saavncdn.com/923/Systumm-Hindi-2023-20230522053412-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/923/e3dfe6105da87481cbe056823c761465_96.mp4",
            durationMs = 211000L,
            source = "Online",
            genre = "Haryanvi",
            isNewRelease = true,
            lyrics = "[00:14.00]Yaar badmasha aale ghoome kaali car mein\n[00:22.00]Chhori kaleshi laad ladave hathiyar mein\n[00:30.00]Masoom sharma ka gaana baje thaaneyan mein"
        ),
        // 7. Jubin Nautiyal
        Song(
            id = "ind_7",
            videoId = "gvyUuxdRdR4",
            title = "Raataan Lambiyan",
            artist = "Tanishk Bagchi, Jubin Nautiyal, Asees Kaur",
            album = "Shershaah",
            artworkUrl = "https://c.saavncdn.com/238/Shershaah-Original-Motion-Picture-Soundtrack--Hindi-2021-20210815181610-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/238/35726d4394604604e961bf5b846870d0_96.mp4",
            durationMs = 230000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:15.00]Teri meri gallan ho gayi mashhoor\n[00:22.00]Kar na kabhi tu mujhe nazron se door\n[00:30.00]Kithe chaliyan tu kithe chaliyan"
        ),
        // 8. Gulzaar Chhaniwala
        Song(
            id = "ind_8",
            videoId = "Q7T8y1Mtwb8",
            title = "Middle Class",
            artist = "Gulzaar Chhaniwala",
            album = "Middle Class",
            artworkUrl = "https://c.saavncdn.com/194/Middle-Class-Haryanvi-2019-20221109134007-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/194/94b14ba5413c8f34e6e6c0b4643bb5e8_96.mp4",
            durationMs = 198000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:18.00]Middle class aale chhore laadley\n[00:26.00]Baapu ne kade hath jod ke padhaye\n[00:34.00]Aaj mehnat ka meetha phal khavaange"
        ),
        // English Trending: The Weeknd
        Song(
            id = "eng_1",
            videoId = "34Na4j8AVgA",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://www.youtube.com/watch?v=34Na4j8AVgA",
            durationMs = 230000L,
            source = "Online",
            genre = "English",
            lyrics = "[00:10.00]I'm tryna put you in the worst mood, ah\n[00:15.00]P1 cleaner than your church shoes, ah\n[00:20.00]Milli point two just to hurt you, ah\n[00:25.00]All red Lamb' just to tease you, ah\n[00:30.00]Look what you've done\n[00:33.00]I'm a starboy"
        ),
        // South Tamil Trending: Anirudh Ravichander
        Song(
            id = "south_1",
            videoId = "KUN5Uf9mObQ",
            title = "Arabic Kuthu - Halamithi Habibo",
            artist = "Anirudh Ravichander, Jonita Gandhi",
            album = "Beast",
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://www.youtube.com/watch?v=KUN5Uf9mObQ",
            durationMs = 280000L,
            source = "Online",
            genre = "Tamil",
            lyrics = "[00:12.00]Malama Pitha Pithadhe\n[00:16.00]Halamithi Habibo\n[00:20.00]Arabic Kuthu style\n[00:24.00]Vaathi coming ottu kuthu"
        ),
        // Bhojpuri Trending: Pawan Singh
        Song(
            id = "bhoj_1",
            videoId = "Gr8G_ldltDE",
            title = "Lollypop Lagelu",
            artist = "Pawan Singh",
            album = "Lollypop Lagelu",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            audioUrl = "https://www.youtube.com/watch?v=Gr8G_ldltDE",
            durationMs = 245000L,
            source = "Online",
            genre = "Bhojpuri",
            lyrics = "[00:15.00]Kamariya kare lapa lap\n[00:22.00]Lollipop lagelu\n[00:30.00]Zila top lagelu"
        ),
        // 9. Sumit Goswami
        Song(
            id = "ind_9",
            videoId = "V0m09sB6GjY",
            title = "Feelings",
            artist = "Sumit Goswami",
            album = "Feelings",
            artworkUrl = "https://c.saavncdn.com/543/Feelings-Unknown-2020-20260508161540-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/543/d93cc04f29fac3d2c6786799410444c6_96.mp4",
            durationMs = 224000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Feeling aave tere sang chhori\n[00:20.00]Akhan band karke dekhoon tasveer thari\n[00:28.00]Sumit goswami ka dil tere aage haarya"
        ),
        // 10. Diler Kharkiya
        Song(
            id = "ind_10",
            videoId = "u3K5K70dF7o",
            title = "Moto",
            artist = "Diler Kharkiya",
            album = "Moto",
            artworkUrl = "https://c.saavncdn.com/392/Moto-Haryanvi-2020-20200416170730-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/392/e2dc3269228b1465226e42cad98d0206_96.mp4",
            durationMs = 181000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Moto meri moto re\n[00:22.00]Kare nakhre hazaar moto\n[00:30.00]Diler kharkiya gaave gaane"
        ),
        // 11. Shreya Ghoshal
        Song(
            id = "ind_11",
            videoId = "zRtPUIumXkk",
            title = "Deewani Mastani",
            artist = "Sanjay Leela Bhansali, Shreya Ghoshal, Ganesh Chandanshive, Mujtaba Aziz Naza, Shadab Faridi, Altamash Faridi, Farhan Sabri, Siddharth-Garima",
            album = "Bajirao Mastani",
            artworkUrl = "https://c.saavncdn.com/808/Bajirao-Mastani-Hindi-2025-20260120201154-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/808/08d91b5009b5e221ea5838d2acb4b690_96.mp4",
            durationMs = 339000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:20.00]Nazar jo teri laagi main deewani ho gayi\n[00:30.00]Deewani haan deewani ho gayi\n[00:40.00]Mashhoor mere ishq ki kahani ho gayi"
        ),
        // 12. Neha Kakkar
        Song(
            id = "ind_12",
            videoId = "V7LwfY5U5WI",
            title = "Mile Ho Tum (Reprise)",
            artist = "Neha Kakkar, Tony Kakkar",
            album = "Fever (Original Motion Picture Soundtrack)",
            artworkUrl = "https://c.saavncdn.com/095/Fever-Hindi-2016-500x500.jpg",
            audioUrl = "https://aac.saavncdn.com/095/c25069539316625a4fc7e894ba5fbccb_96.mp4",
            durationMs = 305000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:18.00]Mile ho tum humko bade naseebon se\n[00:25.00]Churaya hai maine kismat ki lakeeron se\n[00:35.00]Teri mohabbat se saansein mili hain"
        ),
        // 13. Danda Noliwala
        Song(
            id = "ind_13",
            videoId = "E7ergOnpO1Q",
            title = "Not Guilty",
            artist = "Danda Noliwala, Dhanda Nyoliwala",
            album = "Not Guilty",
            artworkUrl = "https://c.saavncdn.com/533/Not-Guilty-Hindi-2026-20260106053129-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=E7ergOnpO1Q",
            durationMs = 175000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:10.00]Danda noliwala aagya re bhaai\n[00:20.00]Yaari sab te upar se"
        ),
        // 14. Sumit Parta
        Song(
            id = "ind_14",
            videoId = "Uq4dyDHgqAI",
            title = "Thada Bhartar",
            artist = "Sumit Parta, Raju Panjabi, Raju Punjabi, Sushila Takhar",
            album = "Thada Bhartar",
            artworkUrl = "https://c.saavncdn.com/751/Thada-Bhartar-Haryanvi-2022-20221109120205-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Uq4dyDHgqAI",
            durationMs = 190000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:15.00]Sumit parta ka gaana baje\n[00:25.00]Desi jaat ke thaath nirale"
        ),
        // 15. Sinta Bhai
        Song(
            id = "ind_15",
            videoId = "Kvpx2RkJbGo",
            title = "Bholenath",
            artist = "Sinta Bhai, Sumit Goswami",
            album = "Bholenath",
            artworkUrl = "https://c.saavncdn.com/325/Bholenath-Haryanvi-2018-20220902000931-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Kvpx2RkJbGo",
            durationMs = 210000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Sinta bhai ka rola chale gaam mein\n[00:24.00]Hathiyar te darte koni"
        ),
        // 16. Fazil Puriya
        Song(
            id = "ind_16",
            videoId = "Bea019pOw5w",
            title = "Chull",
            artist = "Fazil Puriya, Fazilpuria",
            album = "Chull",
            artworkUrl = "https://c.saavncdn.com/628/Chull-Punjabi-2014-20190626120718-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Bea019pOw5w",
            durationMs = 165000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Kar gayi chull ladki kar gayi chull\n[00:24.00]Fazil puriya in the house"
        ),
        // 17. MC Square
        Song(
            id = "ind_17",
            videoId = "PQlKfJqWtsk",
            title = "Bad Munda",
            artist = "MC Square, Jass Manak, Emiway Bantai",
            album = "Bad Munda",
            artworkUrl = "https://c.saavncdn.com/581/Bad-Munda-Hindi-2021-20220803062305-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=PQlKfJqWtsk",
            durationMs = 177000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:10.00]MC Square rhyme drop kare\n[00:22.00]Desi hip hop baje poore desh me"
        ),
        // 18. Pranjal Dahiya
        Song(
            id = "ind_18",
            videoId = "PwH8HHUADuo",
            title = "Bhagatt Aadmi",
            artist = "Pranjal Dahiya, Masoom Sharma, Anjali 99, Aman Jaji",
            album = "Bhagatt Aadmi",
            artworkUrl = "https://c.saavncdn.com/417/Bhagat-Aadmi-Haryanvi-2022-20220603182743-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=PwH8HHUADuo",
            durationMs = 195000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Meri chali gypsy gaddi\n[00:26.00]Pranjal dahiya nachdi aave"
        ),
        // 19. Sapna Choudhary
        Song(
            id = "ind_19",
            videoId = "GftlBD-Apu8",
            title = "Teri Aakhya Ka Yo Kajal",
            artist = "Sapna Choudhary, Dc Madana",
            album = "Teri Aakhya Ka Yo Kajal",
            artworkUrl = "https://c.saavncdn.com/515/Teri-Aakhya-Ka-Yo-Kajal-Haryanvi-2022-20230120165509-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=GftlBD-Apu8",
            durationMs = 262000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:15.00]Teri aakhya ka yo kajal\n[00:28.00]Mane kare se gori ghayal"
        ),
        // 20. Amit Saini Rohtakiya
        Song(
            id = "ind_20",
            videoId = "x86kT6r-EEs",
            title = "Kafan",
            artist = "Amit Saini Rohtakiya, Iqbal, Vansh DX",
            album = "Kafan",
            artworkUrl = "https://c.saavncdn.com/939/Kafan-Punjabi-2026-20260604183304-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=x86kT6r-EEs",
            durationMs = 172000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:15.00]Amit saini rohtakiya ka rola\n[00:28.00]Bhole baba ka bhakt bada"
        ),
        // 21. Vicky Kajla
        Song(
            id = "ind_21",
            videoId = "n2TBNpQAAjo",
            title = "Desi Desi Na Bolya Kar (feat. MD,KD)",
            artist = "Vicky Kajla, Raju Panjabi, Raju Punjabi",
            album = "Desi Desi Na Bolya Kar",
            artworkUrl = "https://c.saavncdn.com/682/Desi-Desi-Na-Bolya-Kar-Haryanvi-2022-20250702075506-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=n2TBNpQAAjo",
            durationMs = 215000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Desi desi na bolya kar chori re\n[00:24.00]Is desi ki fan yeh duniya re"
        ),
        // 22. Khasa Ala Chahar
        Song(
            id = "ind_22",
            videoId = "AMQIiEea12A",
            title = "Loot Liya",
            artist = "Khasa Ala Chahar, Khasa Aala Chahar",
            album = "Loot Liya",
            artworkUrl = "https://c.saavncdn.com/619/Loot-Liya-Haryanvi-2021-20250808093801-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=AMQIiEea12A",
            durationMs = 199000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Khasa ala chahar gaave gaane\n[00:28.00]Tere roop ne loot liya mane"
        ),
        // 23. Anjali Raghav
        Song(
            id = "ind_23",
            videoId = "Qf6YsD2ROIU",
            title = "Sandook",
            artist = "Anjali Raghav, Vijay Varma, Masoom Sharma",
            album = "Sandook",
            artworkUrl = "https://c.saavncdn.com/431/Sandook-Haryanvi-2022-20220727223417-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Qf6YsD2ROIU",
            durationMs = 191000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:10.00]Anjali raghav nachi se gori\n[00:22.00]Suit pehre se laal gulabi"
        ),
        // 24. Raju Panjabi
        Song(
            id = "ind_24",
            videoId = "wDm7PtFXUQo",
            title = "Achha Sila Diya",
            artist = "Raju Panjabi, Raju Punjabi, Jaani, B Praak, Nikhil-Vinay",
            album = "Achha Sila Diya",
            artworkUrl = "https://c.saavncdn.com/646/Achha-Sila-Diya-Hindi-2023-20230112201105-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=wDm7PtFXUQo",
            durationMs = 317000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:16.00]Raju panjabi ki aawaz amar se\n[00:30.00]Desi gaane sada bajenge"
        ),
        // 25. KD Desirock
        Song(
            id = "ind_25",
            videoId = "oxjcKVjojKk",
            title = "Weapon",
            artist = "KD Desirock, KD DESIROCK",
            album = "Weapon",
            artworkUrl = "https://c.saavncdn.com/532/Weapon-Hindi-2024-20260327110125-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=oxjcKVjojKk",
            durationMs = 248000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:11.00]KD desirock rap sunaye\n[00:23.00]Sher kade ghaas nahi khata"
        ),
        // 26. Rawme Hooda
        Song(
            id = "ind_26",
            videoId = "RYOc39XLzh4",
            title = "Kasoote 2",
            artist = "Rawme Hooda, Gulzaar Chhaniwala",
            album = "Kasoote 2",
            artworkUrl = "https://c.saavncdn.com/214/Kasoote-2-Haryanvi-2022-20221109114007-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=RYOc39XLzh4",
            durationMs = 246000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Rawme hooda ka naam chale se\n[00:25.00]Yaari jaan te pyari se"
        ),
        // 27. Vijay Varma
        Song(
            id = "ind_27",
            videoId = "Mlj0hdOG4QQ",
            title = "Bahu Kale Ki",
            artist = "Vijay Varma, Gajender Phogat, Anu Kadyan",
            album = "Bahu Kale Ki",
            artworkUrl = "https://c.saavncdn.com/394/Bahu-Kale-Ki-Haryanvi-2022-20260316104714-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Mlj0hdOG4QQ",
            durationMs = 228000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Bahu kale ki aayi gaam me\n[00:28.00]Vijay varma ka thumka laage"
        ),
        // 28. Miki Malang
        Song(
            id = "ind_28",
            videoId = "7n3HUERLmQA",
            title = "MALANG BHOLE",
            artist = "Miki Malang, Ps Polist",
            album = "MALANG BHOLE",
            artworkUrl = "https://c.saavncdn.com/762/MALANG-BHOLE-Haryanvi-2023-20230706043955-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=7n3HUERLmQA",
            durationMs = 191000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:15.00]Miki malang bhole ka mastana\n[00:30.00]Damru baje dam dam bhole"
        ),
        // 29. Arijit Singh
        Song(
            id = "ind_29",
            videoId = "Umqb9KENgmk",
            title = "Tum Hi Ho",
            artist = "Mithoon, Arijit Singh",
            album = "Aashiqui 2",
            artworkUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Umqb9KENgmk",
            durationMs = 262000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:20.00]Hum tere bin ab reh nahi sakte\n[00:28.00]Tere bina kya wajood mera\n[00:36.00]Kyunki tum hi ho ab tum hi ho\n[00:44.00]Zindagi ab tum hi ho"
        ),
        // 30. Shreya Ghoshal
        Song(
            id = "ind_30",
            videoId = "inEu2qQuGZ8",
            title = "Sunn Raha Hai (Female)",
            artist = "Shreya Ghoshal",
            album = "Aashiqui 2",
            artworkUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=inEu2qQuGZ8",
            durationMs = 314000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:25.00]Apne karam ki kar adaayein\n[00:35.00]Sunn raha hai na tu\n[00:45.00]Ro rahi hoon main"
        ),
        // 31. Sidhu Moose Wala
        Song(
            id = "ind_31",
            videoId = "6xoB4ZiKKn0",
            title = "The Last Ride",
            artist = "Sidhu Moose Wala",
            album = "The Last Ride",
            artworkUrl = "https://c.saavncdn.com/582/The-Last-Ride-Punjabi-2022-20260626154442-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=6xoB4ZiKKn0",
            durationMs = 262000L,
            source = "Online",
            genre = "Punjabi",
            lyrics = "[00:15.00]Ho chobbar de chehre utte noor dasda\n[00:25.00]Ni ehda uthuga jawani ch janaza mithiye"
        ),
        // 32. Diljit Dosanjh
        Song(
            id = "ind_32",
            videoId = "dCmp56tSSmA",
            title = "Born to Shine",
            artist = "Diljit Dosanjh",
            album = "G.O.A.T.",
            artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=dCmp56tSSmA",
            durationMs = 214000L,
            source = "Online",
            genre = "Punjabi",
            lyrics = "[00:12.00]Paisa poosa baare billo soche duniya\n[00:22.00]Jatt paida hoya bas chhaun vaaste"
        ),
        // 33. Karan Aujla
        Song(
            id = "ind_33",
            videoId = "LK7-_dgAVQE",
            title = "Tauba Tauba",
            artist = "Karan Aujla",
            album = "Bad Newz",
            artworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=LK7-_dgAVQE",
            durationMs = 207000L,
            source = "Online",
            genre = "Punjabi",
            lyrics = "[00:15.00]Husn tera tauba tauba\n[00:25.00]Karan aujla da gaana baje gaadiyan ch"
        ),
        // 34. Renuka Panwar
        Song(
            id = "ind_34",
            videoId = "s9AICwTKgOg",
            title = "Chatak Matak",
            artist = "Renuka Panwar",
            album = "Chatak Matak",
            artworkUrl = "https://c.saavncdn.com/001/Chatak-Matak-Haryanvi-2020-20201221105140-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=s9AICwTKgOg",
            durationMs = 137000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Chatak matak ke chale gori\n[00:26.00]Renuka panwar ka daman hile"
        ),
        // 35. Masoom Sharma
        Song(
            id = "ind_35",
            videoId = "MVF-GCOY2kI",
            title = "2 Numbari",
            artist = "Manisha Sharma",
            album = "2 Numbari",
            artworkUrl = "https://c.saavncdn.com/314/2-Numbari-Hindi-2022-20220310024753-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=MVF-GCOY2kI",
            durationMs = 222000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]2 numbari dhandhe koni karda\n[00:24.00]Yaaran aali yaari me jaan haazir"
        ),
        // 36. Gulzaar Chhaniwala
        Song(
            id = "ind_36",
            videoId = "X48UDSJIDl8",
            title = "GodFather Lofi",
            artist = "Gulzaar Chhaniwala",
            album = "GodFather Lofi Mix",
            artworkUrl = "https://c.saavncdn.com/987/GodFather-Lofi-Mix-Haryanvi-2023-20230828093504-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=X48UDSJIDl8",
            durationMs = 206000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:18.00]Godfather banya phire shehar me\n[00:30.00]Gulzaar chhaniwala naam e kaafi se"
        ),
        // 37. Jubin Nautiyal
        Song(
            id = "ind_37",
            videoId = "sCbbMZ-q4-I",
            title = "Lut Gaye (From \"Lut Gaye\")",
            artist = "Manoj Muntashir, Tanishk Bagchi, Jubin Nautiyal",
            album = "World Music Day - Best Of Bollywood Hits",
            artworkUrl = "https://c.saavncdn.com/179/World-Music-Day-Best-Of-Bollywood-Hits-Hindi-2026-20260622111029-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=sCbbMZ-q4-I",
            durationMs = 228000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:15.00]Lut gaye hum toh pehli mulaqaat mein\n[00:28.00]Aankh uthi mohabbat ne angrai li"
        ),
        // 38. Neha Kakkar
        Song(
            id = "ind_38",
            videoId = "_uUdJalMaF8",
            title = "O Saki Saki",
            artist = "Vishal &amp; Shekhar, Neha Kakkar, Tulsi Kumar, B Praak",
            album = "Batla House",
            artworkUrl = "https://c.saavncdn.com/906/Batla-House-Hindi-2019-20241205142148-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=_uUdJalMaF8",
            durationMs = 191000L,
            source = "Online",
            genre = "Bollywood",
            lyrics = "[00:10.00]O saki saki re saki saki\n[00:20.00]Aa paas aa reh na jaaye koi khwaahish baaqi"
        ),
        // 39. Sapna Choudhary
        Song(
            id = "ind_39",
            videoId = "fBsQmLjKf5U",
            title = "Solid Body",
            artist = "Sapna Choudhary, Aryan Chauhan, Ashu Twinkle",
            album = "Solid Body",
            artworkUrl = "https://c.saavncdn.com/676/Solid-Body-Haryanvi-2025-20251016221654-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=fBsQmLjKf5U",
            durationMs = 127000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Solid body re teri solid body\n[00:26.00]Sapna choudhary ka dance baje"
        ),
        // 40. MC Square
        Song(
            id = "ind_40",
            videoId = "Tnfs0MZsBBE",
            title = "Ram Ram",
            artist = "MC Square",
            album = "Mtv Hustle 2.0",
            artworkUrl = "https://c.saavncdn.com/578/Mtv-Hustle-2-0-Hindi-2023-20230403132332-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=Tnfs0MZsBBE",
            durationMs = 209000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:10.00]Ram ram saareyan ne\n[00:20.00]MC square hustle kare desi swag se"
        ),
        // 41. Pranjal Dahiya
        Song(
            id = "ind_41",
            videoId = "cZSrWoBMSrg",
            title = "Balam Thanedar",
            artist = "Pranjal Dahiya, Mahi Panchal",
            album = "Balam Thanedar",
            artworkUrl = "https://c.saavncdn.com/744/Balam-Thanedar-Haryanvi-2024-20240112050543-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=cZSrWoBMSrg",
            durationMs = 137000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:14.00]Mera balam thanedar chalave gypsy\n[00:28.00]Pranjal dahiya nachdi aave gaam mein"
        ),
        // 42. Sumit Goswami
        Song(
            id = "ind_42",
            videoId = "m0Db-rzp20k",
            title = "Bawli Tared",
            artist = "Sumit Goswami",
            album = "Bawli Tared",
            artworkUrl = "https://c.saavncdn.com/754/Bawli-Tared-Haryanvi-2021-20210522093949-500x500.jpg",
            audioUrl = "https://www.youtube.com/watch?v=m0Db-rzp20k",
            durationMs = 228000L,
            source = "Online",
            genre = "Haryanvi",
            lyrics = "[00:12.00]Bawli tared hogya tere pyaar mein\n[00:24.00]Sumit goswami ka gaana baje yaaran mein"
        )
    )

    // Complete list of all 28 requested Indian Artists
    val artists = listOf(
        Artist(
            id = "art_1",
            name = "Arijit Singh",
            imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323062147_500x500.jpg",
            genre = "Bollywood / Soul",
            followerCount = "38.5M",
            isFollowed = true
        ),
        Artist(
            id = "art_2",
            name = "Shreya Ghoshal",
            imageUrl = "https://i.postimg.cc/Bv7ZC0dX/download.jpg",
            genre = "Bollywood / Melodies",
            followerCount = "24.2M",
            isFollowed = true
        ),
        Artist(
            id = "art_3",
            name = "Jubin Nautiyal",
            imageUrl = "https://i.postimg.cc/Hn2hQphZ/download.jpg",
            genre = "Romantic / Acoustic",
            followerCount = "18.9M",
            isFollowed = false
        ),
        Artist(
            id = "art_4",
            name = "Neha Kakkar",
            imageUrl = "https://c.saavncdn.com/artists/Neha_Kakkar_006_20200822042626_500x500.jpg",
            genre = "Bollywood / Dance",
            followerCount = "29.1M",
            isFollowed = false
        ),
        Artist(
            id = "art_5",
            name = "Sidhu Moose Wala",
            imageUrl = "https://i.postimg.cc/PxM47SQz/images.jpg",
            genre = "Punjabi / Desi Hip-Hop",
            followerCount = "22.4M",
            isFollowed = true
        ),
        Artist(
            id = "art_6",
            name = "Diljit Dosanjh",
            imageUrl = "https://i.postimg.cc/KYPrK0Vf/download.jpg",
            genre = "Punjabi / Pop",
            followerCount = "19.8M",
            isFollowed = true
        ),
        Artist(
            id = "art_7",
            name = "Karan Aujla",
            imageUrl = "https://i.postimg.cc/wBpDdhvV/download.jpg",
            genre = "Punjabi / Rap",
            followerCount = "14.5M",
            isFollowed = false
        ),
        Artist(
            id = "art_8",
            name = "Danda Noliwala",
            imageUrl = "https://i.postimg.cc/59CK5WGQ/download.jpg",
            genre = "Haryanvi / Folk",
            followerCount = "3.2M",
            isFollowed = false
        ),
        Artist(
            id = "art_9",
            name = "Masoom Sharma",
            imageUrl = "https://i.postimg.cc/CxL4Q3vF/download.jpg",
            genre = "Haryanvi / Ragni",
            followerCount = "6.8M",
            isFollowed = true
        ),
        Artist(
            id = "art_10",
            name = "Gulzaar Chhaniwala",
            imageUrl = "https://i.postimg.cc/cJtfq0fS/images.jpg",
            genre = "Haryanvi / Beats",
            followerCount = "8.1M",
            isFollowed = true
        ),
        Artist(
            id = "art_11",
            name = "Sumit Parta",
            imageUrl = "https://i.postimg.cc/QCYMxWr2/download.jpg",
            genre = "Haryanvi / Folk",
            followerCount = "1.9M",
            isFollowed = false
        ),
        Artist(
            id = "art_12",
            name = "Sinta Bhai",
            imageUrl = "https://i.postimg.cc/43vNH0gr/download.jpg",
            genre = "Haryanvi / Rap",
            followerCount = "1.2M",
            isFollowed = false
        ),
        Artist(
            id = "art_13",
            name = "Fazil Puriya",
            imageUrl = "https://i.postimg.cc/3rq7NhG4/download.webp",
            genre = "Haryanvi / Hip-Hop",
            followerCount = "4.3M",
            isFollowed = false
        ),
        Artist(
            id = "art_14",
            name = "MC Square",
            imageUrl = "https://i.postimg.cc/fbwWQvGj/download.webp",
            genre = "Desi Hip-Hop",
            followerCount = "3.7M",
            isFollowed = false
        ),
        Artist(
            id = "art_15",
            name = "Pranjal Dahiya",
            imageUrl = "https://i.postimg.cc/05LwRLcp/images.webp",
            genre = "Haryanvi / Pop",
            followerCount = "7.9M",
            isFollowed = true
        ),
        Artist(
            id = "art_16",
            name = "Sapna Choudhary",
            imageUrl = "https://i.postimg.cc/kX3BhW5P/download.jpg",
            genre = "Haryanvi / Dance",
            followerCount = "12.3M",
            isFollowed = false
        ),
        Artist(
            id = "art_17",
            name = "Renuka Panwar",
            imageUrl = "https://i.postimg.cc/cCq42N0w/download.webp",
            genre = "Haryanvi / Hits",
            followerCount = "9.5M",
            isFollowed = true
        ),
        Artist(
            id = "art_18",
            name = "Amit Saini Rohtakiya",
            imageUrl = "https://i.postimg.cc/NfDB7X9X/download.jpg",
            genre = "Haryanvi / Swag",
            followerCount = "5.4M",
            isFollowed = false
        ),
        Artist(
            id = "art_19",
            name = "Vicky Kajla",
            imageUrl = "https://i.postimg.cc/8Cr23f53/download.jpg",
            genre = "Haryanvi / Desi",
            followerCount = "2.8M",
            isFollowed = false
        ),
        Artist(
            id = "art_20",
            name = "Khasa Ala Chahar",
            imageUrl = "https://i.postimg.cc/P50cmf4M/download.jpg",
            genre = "Haryanvi / Singer",
            followerCount = "5.1M",
            isFollowed = false
        ),
        Artist(
            id = "art_21",
            name = "Anjali Raghav",
            imageUrl = "https://i.postimg.cc/ydjvr7mH/download.jpg",
            genre = "Haryanvi / Folk",
            followerCount = "3.9M",
            isFollowed = false
        ),
        Artist(
            id = "art_22",
            name = "Raju Panjabi",
            imageUrl = "https://i.postimg.cc/C1cssmg9/download.jpg",
            genre = "Haryanvi / Legend",
            followerCount = "6.2M",
            isFollowed = false
        ),
        Artist(
            id = "art_23",
            name = "KD Desirock",
            imageUrl = "https://i.postimg.cc/tJLPBcmB/download.jpg",
            genre = "Haryanvi / Rap",
            followerCount = "4.7M",
            isFollowed = false
        ),
        Artist(
            id = "art_24",
            name = "Rawme Hooda",
            imageUrl = "https://i.postimg.cc/C5jPHbck/download.jpg",
            genre = "Haryanvi / Pop",
            followerCount = "1.8M",
            isFollowed = false
        ),
        Artist(
            id = "art_25",
            name = "Vijay Varma",
            imageUrl = "https://i.postimg.cc/kgM8fWjJ/download.jpg",
            genre = "Haryanvi / Actor",
            followerCount = "2.4M",
            isFollowed = false
        ),
        Artist(
            id = "art_26",
            name = "Miki Malang",
            imageUrl = "https://i.postimg.cc/XYDZLmG9/download.jpg",
            genre = "Haryanvi / Folk",
            followerCount = "1.1M",
            isFollowed = false
        ),
        Artist(
            id = "art_27",
            name = "Diler Kharkiya",
            imageUrl = "https://i.postimg.cc/MTwzmmDb/download.jpg",
            genre = "Haryanvi / Melodies",
            followerCount = "4.9M",
            isFollowed = false
        ),
        Artist(
            id = "art_28",
            name = "Sumit Goswami",
            imageUrl = "https://i.postimg.cc/NjrYhzCJ/download.jpg",
            genre = "Haryanvi / Gangster",
            followerCount = "5.8M",
            isFollowed = false
        )
    )

    // Strictly user-added playlists only. No default mock playlists.
    val initialPlaylists: List<Playlist> = emptyList()
}
