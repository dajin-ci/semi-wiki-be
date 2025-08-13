package com.mysite.sbb.document.util;

import java.text.Normalizer;

public class SlugGenerator {
    public static String toSlug(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{Alnum}]+", "-")
                .replaceAll("(^-|-$)", "");
        return s.toLowerCase();
    }
}
