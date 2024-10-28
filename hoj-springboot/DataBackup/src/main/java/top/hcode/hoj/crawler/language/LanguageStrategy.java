package top.hcode.hoj.crawler.language;

import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.problem.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/1/27 21:14
 * @Description:
 */
public abstract class LanguageStrategy {

    public abstract String getLanguageNameById(String id, String oj);

    public abstract List<Language> buildLanguageListByIds(List<Language> allLanguageList, List<String> langIdList);

    public abstract List<Language> buildAddLanguageList(List<Language> allLanguageList,
            List<Pair_<String, String>> langList, String oj);

    public abstract Collection<String> getLangList();

    public abstract String getOJName();

    public List<Language> buildLanguageList() {
        List<Language> languageList = new ArrayList<>();
        for (String lang : getLangList()) {
            languageList.add(new Language()
                    .setName(lang)
                    .setDescription(lang)
                    .setOj(getOJName())
                    .setContentType(getLangContentType(lang)));
        }
        return languageList;
    }

    private final static List<String> CLang = Arrays.asList("c", "gcc", "clang");
    private final static List<String> CPPLang = Arrays.asList("c++", "g++", "clang++");
    private final static List<String> PythonLang = Arrays.asList("python", "pypy");
    private final static List<String> JSLang = Arrays.asList("node", "javascript");

    protected String getLangContentType(String name) {
        String lowerName = name.toLowerCase();

        for (String lang : CPPLang) {
            if (lowerName.contains(lang)) {
                return "text/x-c++src";
            }
        }

        if (lowerName.contains("c#")) {
            return "text/x-csharp";
        }

        for (String lang : CLang) {
            if (lowerName.contains(lang)) {
                return "text/x-csrc";
            }
        }

        for (String lang : PythonLang) {
            if (lowerName.contains(lang)) {
                return "text/x-python";
            }
        }
        for (String lang : JSLang) {
            if (lowerName.contains(lang)) {
                return "text/javascript";
            }
        }
        if (lowerName.contains("scala")) {
            return "text/x-scala";
        }

        if (lowerName.contains("java")) {
            return "text/x-java";
        }

        if (lowerName.contains("pascal")) {
            return "text/x-pascal";
        }

        if (lowerName.contains("go")) {
            return "text/x-go";
        }

        if (lowerName.contains("ruby")) {
            return "text/x-ruby";
        }

        if (lowerName.contains("rust")) {
            return "text/x-rustsrc";
        }

        if (lowerName.contains("php")) {
            return "text/x-php";
        }

        if (lowerName.contains("perl")) {
            return "text/x-perl";
        }

        if (lowerName.contains("fortran")) {
            return "text/x-fortran";
        }

        if (lowerName.contains("haskell")) {
            return "text/x-haskell";
        }

        if (lowerName.contains("ocaml")) {
            return "text/x-ocaml";
        }

        if (lowerName.contains("assembly")) {
            return "text/x-asm";
        }
        if (lowerName.contains("erlang")) {
            return "text/x-erlang";
        }
        if (lowerName.contains("swift")) {
            return "text/x-swift";
        }
        if (lowerName.contains("kotlin")) {
            return "text/x-kotlin";
        }
        if (lowerName.contains("julia")) {
            return "text/x-julia";
        }
        if (lowerName.contains("smalltalk")) {
            return "text/x-smalltalk";
        }
        if (lowerName.contains("ada")) {
            return "text/x-ada";
        }
        if (lowerName.contains("bash")) {
            return "application/x-sh";
        }
        if (lowerName.contains("lua")) {
            return "text/x-lua";
        }
        if (lowerName.contains("elixir")) {
            return "text/x-elixir";
        }
        if (lowerName.contains("sql")) {
            return "text/x-sql";
        }
        if (lowerName.contains("dart")) {
            return "text/x-dart";
        }
        if (lowerName.contains("groovy")) {
            return "text/x-groovy";
        }
        if (lowerName.contains("vb")) {
            return "text/x-vb";
        }
        if (lowerName.contains("bf")) {
            return "text/x-brainfuck";
        }
        if (lowerName.contains("r")) {
            return "text/x-rsrc";
        }
        if (lowerName.contains("awk")) {
            return "text/x-awk";
        }
        if (lowerName.contains("plain text")) {
            return "text/plain";
        }
        if (lowerName.contains("openjdk")) {
            return "text/x-java";
        }

        return null;

    }
}