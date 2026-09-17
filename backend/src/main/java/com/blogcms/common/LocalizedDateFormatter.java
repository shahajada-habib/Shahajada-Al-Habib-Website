package com.blogcms.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for how a date or a plain count is written on the
 * public site. Templates used to mix two sources of truth for this — Java's
 * {@code DateTimeFormatter}/{@code #temporals} (Latin digits, its own month
 * spelling) next to this service's own hand-rolled month names (also Latin
 * digits) — which is why the same month could read two different ways on the
 * same page and every digit stayed in English even in Bengali. Bean name
 * "dateDisplay" so templates can call it as {@code ${@dateDisplay....}}.
 */
@Component("dateDisplay")
public class LocalizedDateFormatter {

    private static final String[] BENGALI_MONTHS = {
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    };
    private static final String[] ENGLISH_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final char[] BENGALI_DIGITS = {
            '০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'
    };

    public String format(LocalDate date) {
        return date == null ? "" : build(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public String format(LocalDateTime date) {
        return date == null ? "" : build(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    /** Any plain count — a view count, a page number — written in the reader's digits. */
    public String number(long value) {
        return isEnglish() ? Long.toString(value) : toBengaliDigits(Long.toString(value));
    }

    private String build(int day, int month, int year) {
        boolean english = isEnglish();
        String[] months = english ? ENGLISH_MONTHS : BENGALI_MONTHS;
        String dayStr = english ? String.valueOf(day) : toBengaliDigits(String.valueOf(day));
        String yearStr = english ? String.valueOf(year) : toBengaliDigits(String.valueOf(year));
        return dayStr + " " + months[month - 1] + ", " + yearStr;
    }

    private boolean isEnglish() {
        return "en".equals(LocaleContextHolder.getLocale().getLanguage());
    }

    private String toBengaliDigits(String ascii) {
        StringBuilder result = new StringBuilder(ascii.length());
        for (int i = 0; i < ascii.length(); i++) {
            char c = ascii.charAt(i);
            result.append(c >= '0' && c <= '9' ? BENGALI_DIGITS[c - '0'] : c);
        }
        return result.toString();
    }
}
