-- Singleton row (id fixed at 1) holding the author's own profile content that
-- used to be hardcoded across templates: name, bio, book details, social
-- links. One row rather than a generic key/value table because every field
-- here is a known, fixed piece of the public site's chrome, not open-ended
-- CMS content — a key/value table would just move the hardcoding from Java
-- templates into unstructured row keys.
CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT NOT NULL,
    author_name VARCHAR(160),
    tagline VARCHAR(300),
    tagline_en VARCHAR(300),
    about_bio TEXT,
    profile_image_url VARCHAR(1000),
    book_title VARCHAR(300),
    book_year VARCHAR(40),
    book_venue VARCHAR(300),
    book_cover_url VARCHAR(1000),
    book_quote TEXT,
    book_quote_author VARCHAR(300),
    facebook_url VARCHAR(500),
    instagram_url VARCHAR(500),
    youtube_url VARCHAR(500),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

-- Seed the one row with today's hardcoded values, so the switch to reading
-- from this table is a no-op for visitors until someone edits it in the admin.
INSERT INTO site_settings (
    id, author_name, tagline, tagline_en, about_bio, profile_image_url,
    book_title, book_year, book_venue, book_cover_url, book_quote, book_quote_author,
    facebook_url, instagram_url, youtube_url, updated_at
) VALUES (
    1, 'শাহজাদা আল হাবীব', 'লেখক • কবি • ভ্রমণপিপাসু • সাইক্লিস্ট • ফটোগ্রাফার', 'Writer • Poet • Traveler • Cyclist • Photographer',
    '<p>আমি শাহজাদা আল হাবীব। আমার প্রথম প্রকাশিত কাব্যগ্রন্থ <strong>"অশ্রুচুক্তি"</strong> (২০২৩), অমর একুশে বইমেলায় প্রকাশিত। ফেসবুকসহ বিভিন্ন সামাজিক প্ল্যাটফর্মে সমসাময়িক বিষয় নিয়ে নিয়মিত লিখি। লেখালেখির বাইরে সাইক্লিং, ফটোগ্রাফি ও দেশের আনাচে কানাচে ভ্রমণ করা আমার শখ।</p><p>কিশোরগঞ্জ সরকারি বালক উচ্চ বিদ্যালয় থেকে মাধ্যমিক, গুরুদয়াল সরকারি কলেজ থেকে বিজ্ঞান বিভাগে উচ্চ-মাধ্যমিক এবং চট্টগ্রাম বিশ্ববিদ্যালয় থেকে স্নাতক সম্পন্ন করেছি।</p>',
    '/assets/profile-440.jpg',
    'অশ্রুচুক্তি', '২০২৩', 'অমর একুশে বইমেলা', '/assets/cover-300.jpg',
    'শাহজাদা আল হাবীবের অশ্রুচুক্তি (২০২৩), কাব্যগ্রন্থ প্রেম এবং আত্মোচ্চারণের স্বতঃস্ফূর্ত এবং প্রত্যয়ী প্রকাশ...',
    '— মহীবুল আজিজ, অধ্যাপক, বাংলা বিভাগ, চট্টগ্রাম বিশ্ববিদ্যালয়',
    'https://www.facebook.com/ShahajadaAlHabib', 'https://www.instagram.com/pranto.habib', 'https://www.youtube.com/@shahajadaalhabib',
    CURRENT_TIMESTAMP(6)
);
