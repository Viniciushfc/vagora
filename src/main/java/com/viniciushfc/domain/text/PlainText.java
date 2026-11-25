package com.viniciushfc.domain.text;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlainText {

    private static final Pattern FENCE = Pattern.compile("(?m)^[ \\t]*```.*(\\R|$)");
    private static final Pattern HEADING = Pattern.compile("(?m)^[ \\t]{0,3}#{1,6}[ \\t]+");
    private static final Pattern STAR_BULLET = Pattern.compile("(?m)^([ \\t]*)[*•][ \\t]+");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(?=\\S)(.+?)(?<=\\S)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<![\\w*])\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*(?![\\w*])");
    private static final Pattern CODE = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern ODD_HYPHEN = Pattern.compile("[\u2010\u2011\u2012\u2212]");
    private static final Pattern ODD_SPACE = Pattern.compile("[\u00A0\u2007\u202F]");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");

    private PlainText() {
    }

    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        String out = ODD_HYPHEN.matcher(text).replaceAll("-");
        out = ODD_SPACE.matcher(out).replaceAll(" ");
        out = FENCE.matcher(out).replaceAll("");
        out = HEADING.matcher(out).replaceAll("");
        out = STAR_BULLET.matcher(out).replaceAll("$1- ");
        out = BOLD.matcher(out).replaceAll("$1");
        out = ITALIC.matcher(out).replaceAll("$1");
        out = CODE.matcher(out).replaceAll("$1");
        return out.replace("**", "");
    }

    public static List<String> missingNumbers(String original, String generated) {
        Set<String> found = numbers(generated).collect(Collectors.toSet());
        return numbers(original).filter(number -> !found.contains(number)).distinct().toList();
    }

    private static Stream<String> numbers(String text) {
        return NUMBER.matcher(text == null ? "" : text).results()
                .map(match -> match.group().replaceFirst("^0+(?=\\d)", ""));
    }

    public static List<String> cleanAll(List<String> texts) {
        return texts == null ? null : texts.stream().map(PlainText::clean).toList();
    }
}
