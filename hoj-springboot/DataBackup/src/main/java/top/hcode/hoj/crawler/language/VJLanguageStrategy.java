package top.hcode.hoj.crawler.language;

import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.problem.Language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import top.hcode.hoj.utils.JsoupUtils;
import cn.hutool.json.JSONObject;
import org.jsoup.Connection;

public class VJLanguageStrategy extends LanguageStrategy {

    public static Map<String, String> headers = MapUtil
            .builder(new HashMap<String, String>())
            .put("Accept", "*/*")
            .put("Connection", "keep-alive")
            .put("X-Requested-With", "XMLHttpRequest")
            .put("Content-Type",
                    "application/x-www-form-urlencoded;application/json;charset=UTF-8")
            .put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0")
            .map();

    private static final Map<String, Map<String, String>> ojLanguagesMap = new HashMap<>();

    static {
        try {
            // String url = "https://vjudge.net/util/cfg";

            // Connection connection = JsoupUtils.getConnectionFromUrl(url, null, headers,
            // false);
            // JSONObject jsonObject = JsoupUtils.getJsonFromConnection(connection);
            // String json = jsonObject.toString();

            String json = "{\r\n" + //
                    "\"remoteOJs\":{" + //
                    "    \"FZU\": {\r\n" + //
                    "        \"name\": \"FZU\",\r\n" + //
                    "        \"homepage\": \"http://acm.fzu.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"GNU C++\",\r\n" + //
                    "            \"1\": \"GNU C\",\r\n" + //
                    "            \"2\": \"Pascal\",\r\n" + //
                    "            \"3\": \"Java\",\r\n" + //
                    "            \"4\": \"Visual C++\",\r\n" + //
                    "            \"5\": \"Visual C\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"FZU_favicon.gif\"\r\n" + //
                    "    },\r\n" + //
                    "    \"SGU\": {\r\n" + //
                    "        \"name\": \"SGU\",\r\n" + //
                    "        \"homepage\": \"https://codeforces.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"codeforces.com\",\r\n" + //
                    "                \"JSESSIONID\",\r\n" + //
                    "                \"0123456789ABCDEF0123456789ABCDEF\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"SGU_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CSES\": {\r\n" + //
                    "        \"name\": \"CSES\",\r\n" + //
                    "        \"homepage\": \"https://cses.fi\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"Assembly\": \"Assembly\",\r\n" + //
                    "            \"C++11\": \"C++11\",\r\n" + //
                    "            \"C++17\": \"C++17\",\r\n" + //
                    "            \"C++20\": \"C++20\",\r\n" + //
                    "            \"Haskell\": \"Haskell\",\r\n" + //
                    "            \"Java\": \"Java\",\r\n" + //
                    "            \"Node.js\": \"Node.js\",\r\n" + //
                    "            \"Pascal\": \"Pascal\",\r\n" + //
                    "            \"CPython2\": \"CPython2\",\r\n" + //
                    "            \"PyPy2\": \"PyPy2\",\r\n" + //
                    "            \"CPython3\": \"CPython3\",\r\n" + //
                    "            \"PyPy3\": \"PyPy3\",\r\n" + //
                    "            \"Ruby\": \"Ruby\",\r\n" + //
                    "            \"Rust\": \"Rust\",\r\n" + //
                    "            \"Scala\": \"Scala\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"cses.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HRBUST\": {\r\n" + //
                    "        \"name\": \"HRBUST\",\r\n" + //
                    "        \"homepage\": \"http://acm.hrbust.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"GCC\",\r\n" + //
                    "            \"2\": \"G++\",\r\n" + //
                    "            \"3\": \"JAVA\",\r\n" + //
                    "            \"4\": \"PHP\",\r\n" + //
                    "            \"6\": \"Python3\",\r\n" + //
                    "            \"7\": \"Haskell\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"hrbust.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"DMOJ\": {\r\n" + //
                    "        \"name\": \"DMOJ\",\r\n" + //
                    "        \"homepage\": \"https://dmoj.ca\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"dmoj.ca\",\r\n" + //
                    "                \"sessionid\",\r\n" + //
                    "                \"0123456789abcdef0123456789abcdef\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"dmoj_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"SCU\": {\r\n" + //
                    "        \"name\": \"SCU\",\r\n" + //
                    "        \"homepage\": \"https://acm.scu.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C++\": \"C++\",\r\n" + //
                    "            \"C\": \"C\",\r\n" + //
                    "            \"Java\": \"Java\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"SCU_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"51Nod\": {\r\n" + //
                    "        \"name\": \"51Nod\",\r\n" + //
                    "        \"homepage\": \"https://www.51nod.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"C\",\r\n" + //
                    "            \"2\": \"C 11\",\r\n" + //
                    "            \"11\": \"C++\",\r\n" + //
                    "            \"12\": \"C++ 11\",\r\n" + //
                    "            \"21\": \"C#\",\r\n" + //
                    "            \"31\": \"Java\",\r\n" + //
                    "            \"41\": \"Python2\",\r\n" + //
                    "            \"42\": \"Python3\",\r\n" + //
                    "            \"45\": \"PyPy2\",\r\n" + //
                    "            \"46\": \"PyPy3\",\r\n" + //
                    "            \"51\": \"Ruby\",\r\n" + //
                    "            \"61\": \"Php\",\r\n" + //
                    "            \"71\": \"Haskell\",\r\n" + //
                    "            \"81\": \"Scala\",\r\n" + //
                    "            \"91\": \"Javascript\",\r\n" + //
                    "            \"101\": \"Go\",\r\n" + //
                    "            \"111\": \"Visual C++\",\r\n" + //
                    "            \"121\": \"Objective C\",\r\n" + //
                    "            \"131\": \"Pascal\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"www.51nod.com\",\r\n" + //
                    "                \".AspNetCore.Cookies\",\r\n" + //
                    "                \"C7B20CA......\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"51nod.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CSAcademy\": {\r\n" + //
                    "        \"name\": \"CSAcademy\",\r\n" + //
                    "        \"homepage\": \"https://csacademy.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"2\": \"Java\",\r\n" + //
                    "            \"3\": \"Python 2\",\r\n" + //
                    "            \"4\": \"Python 3\",\r\n" + //
                    "            \"5\": \"C#\",\r\n" + //
                    "            \"6\": \"Haskell\",\r\n" + //
                    "            \"7\": \"BASH\",\r\n" + //
                    "            \"8\": \"Fortran\",\r\n" + //
                    "            \"9\": \"Lua\",\r\n" + //
                    "            \"10\": \"Ruby\",\r\n" + //
                    "            \"11\": \"Perl\",\r\n" + //
                    "            \"12\": \"PHP\",\r\n" + //
                    "            \"13\": \"C\",\r\n" + //
                    "            \"14\": \"Objective-C\",\r\n" + //
                    "            \"15\": \"Smalltalk\",\r\n" + //
                    "            \"16\": \"OCaml\",\r\n" + //
                    "            \"17\": \"Javascript\",\r\n" + //
                    "            \"18\": \"COBOL\",\r\n" + //
                    "            \"19\": \"Ada\",\r\n" + //
                    "            \"20\": \"Pascal\",\r\n" + //
                    "            \"21\": \"Common LISP\",\r\n" + //
                    "            \"22\": \"Erlang\",\r\n" + //
                    "            \"23\": \"Tcl\",\r\n" + //
                    "            \"24\": \"Octave\",\r\n" + //
                    "            \"25\": \"Go\",\r\n" + //
                    "            \"26\": \"Swift\",\r\n" + //
                    "            \"27\": \"Scala\",\r\n" + //
                    "            \"28\": \"Pypy 2\",\r\n" + //
                    "            \"29\": \"Pypy 3\",\r\n" + //
                    "            \"30\": \"Kotlin\",\r\n" + //
                    "            \"31\": \"Rust\",\r\n" + //
                    "            \"32\": \"Julia\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"csacademy.com\",\r\n" + //
                    "                \"crossSessionId\",\r\n" + //
                    "                \"\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"csacademy.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Yukicoder\": {\r\n" + //
                    "        \"name\": \"Yukicoder\",\r\n" + //
                    "        \"homepage\": \"https://yukicoder.me\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"yukicoder_icon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HUST\": {\r\n" + //
                    "        \"name\": \"HUST\",\r\n" + //
                    "        \"homepage\": \"http://www.hustoj.org\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C\",\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"2\": \"Pascal\",\r\n" + //
                    "            \"3\": \"Java\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"HUST_icon.jpg\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Baekjoon\": {\r\n" + //
                    "        \"name\": \"Baekjoon\",\r\n" + //
                    "        \"homepage\": \"https://www.acmicpc.net/lang?lang=1\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C99\",\r\n" + //
                    "            \"12\": \"Go\",\r\n" + //
                    "            \"17\": \"node.js\",\r\n" + //
                    "            \"28\": \"Python 3\",\r\n" + //
                    "            \"29\": \"D\",\r\n" + //
                    "            \"58\": \"Text\",\r\n" + //
                    "            \"68\": \"Ruby\",\r\n" + //
                    "            \"69\": \"Kotlin (JVM)\",\r\n" + //
                    "            \"73\": \"PyPy3\",\r\n" + //
                    "            \"74\": \"Swift\",\r\n" + //
                    "            \"84\": \"C++17\",\r\n" + //
                    "            \"85\": \"C++17 (Clang)\",\r\n" + //
                    "            \"86\": \"C#\",\r\n" + //
                    "            \"93\": \"Java 11\",\r\n" + //
                    "            \"94\": \"Rust 2018\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"acmicpc.net\",\r\n" + //
                    "                \"bojautologin\",\r\n" + //
                    "                \"Check 'Keep me signed in' when login.\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"baekjoon_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"LibreOJ\": {\r\n" + //
                    "        \"name\": \"LibreOJ\",\r\n" + //
                    "        \"homepage\": \"https://api.loj.ac\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"haskell\": \"Haskell 2010\",\r\n" + //
                    "            \"cpp14-g++\": \"G++ (ISO C++14) -Ofast 64bit\",\r\n" + //
                    "            \"gcc\": \"GCC (ISO C17) -Ofast 64bit\",\r\n" + //
                    "            \"cpp11-g++\": \"G++ (ISO C++11) -Ofast 64bit\",\r\n" + //
                    "            \"pascal\": \"Kotlin -O2\",\r\n" + //
                    "            \"kotlin\": \"Kotlin 1.5\",\r\n" + //
                    "            \"go\": \"Go\",\r\n" + //
                    "            \"cpp17-g++\": \"G++ (ISO C++17) -Ofast 64bit\",\r\n" + //
                    "            \"cpp17-clang\": \"Clang++ (ISO C++17) -Ofast 64bit\",\r\n" + //
                    "            \"python3\": \"Python 3.9\",\r\n" + //
                    "            \"python2\": \"Python 2.7\",\r\n" + //
                    "            \"rust\": \"Rust 2018\",\r\n" + //
                    "            \"csharp\": \"C# 8\",\r\n" + //
                    "            \"java\": \"Java\",\r\n" + //
                    "            \"clang\": \"Clang (ISO C17) -Ofast 64bit\",\r\n" + //
                    "            \"fsharp\": \"F#\",\r\n" + //
                    "            \"cpp11-clang\": \"Clang++ (ISO C++11) -Ofast 64bit\",\r\n" + //
                    "            \"cpp14-clang\": \"Clang++ (ISO C++14) -Ofast 64bit\",\r\n" + //
                    "            \"cpp20-g++\": \"G++ (ISO C++20) -Ofast 64bit\",\r\n" + //
                    "            \"cpp20-clang\": \"Clang++ (ISO C++20) -Ofast 64bit\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"1\",\r\n" + //
                    "                \"loj.ac\",\r\n" + //
                    "                \"appState\",\r\n" + //
                    "                \"{'localLocale......\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"libreoj.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"OpenJ_Bailian\": {\r\n" + //
                    "        \"name\": \"OpenJ_Bailian\",\r\n" + //
                    "        \"homepage\": \"http://bailian.openjudge.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"G++\": \"G++(9.3(with c++17))\",\r\n" + //
                    "            \"GCC\": \"GCC(9.3)\",\r\n" + //
                    "            \"Java\": \"Java(OpenJDK14)\",\r\n" + //
                    "            \"Pascal\": \"Pascal(FreePascal)\",\r\n" + //
                    "            \"Python3\": \"Python3(3.8)\",\r\n" + //
                    "            \"C#\": \"C#(mono6.8)\",\r\n" + //
                    "            \"PyPy3\": \"PyPy3(7.3.1)\",\r\n" + //
                    "            \"Perl\": \"Perl(5.30.0)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"poj.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"黑暗爆炸\": {\r\n" + //
                    "        \"name\": \"黑暗爆炸\",\r\n" + //
                    "        \"homepage\": \"https://darkbzoj.cc\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C++\": \"C++\",\r\n" + //
                    "            \"Python2.7\": \"Python2.7\",\r\n" + //
                    "            \"Java7\": \"Java7\",\r\n" + //
                    "            \"C++11\": \"C++11\",\r\n" + //
                    "            \"Python3\": \"Python3\",\r\n" + //
                    "            \"Java8\": \"Java8\",\r\n" + //
                    "            \"C\": \"C\",\r\n" + //
                    "            \"Pascal\": \"Pascal\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"darkbzoj.cc\",\r\n" + //
                    "                \"PHPSESSID\",\r\n" + //
                    "                \"\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"UOJ.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"UESTC_old\": {\r\n" + //
                    "        \"name\": \"UESTC_old\",\r\n" + //
                    "        \"homepage\": \"http://acm.uestc.edu.cn\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"UESTC_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Gym\": {\r\n" + //
                    "        \"name\": \"Gym\",\r\n" + //
                    "        \"homepage\": \"https://codeforces.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"codeforces.com\",\r\n" + //
                    "                \"JSESSIONID\",\r\n" + //
                    "                \"0123456789ABCDEF0123456789ABCDEF\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"CodeForces_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HYSBZ\": {\r\n" + //
                    "        \"name\": \"HYSBZ\",\r\n" + //
                    "        \"homepage\": \"http://www.lydsy.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C\",\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"2\": \"Pascal\",\r\n" + //
                    "            \"3\": \"Java\",\r\n" + //
                    "            \"6\": \"Python\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"HYSBZ_icon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Aizu\": {\r\n" + //
                    "        \"name\": \"Aizu\",\r\n" + //
                    "        \"homepage\": \"https://judgeapi.u-aizu.ac.jp\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C\": \"C\",\r\n" + //
                    "            \"C++\": \"C++\",\r\n" + //
                    "            \"JAVA\": \"JAVA\",\r\n" + //
                    "            \"C++11\": \"C++11\",\r\n" + //
                    "            \"C++14\": \"C++14\",\r\n" + //
                    "            \"C++17\": \"C++17\",\r\n" + //
                    "            \"C#\": \"C#\",\r\n" + //
                    "            \"D\": \"D\",\r\n" + //
                    "            \"Ruby\": \"Ruby\",\r\n" + //
                    "            \"Python\": \"Python\",\r\n" + //
                    "            \"Python3\": \"Python3\",\r\n" + //
                    "            \"PyPy3\": \"PyPy3\",\r\n" + //
                    "            \"PHP\": \"PHP\",\r\n" + //
                    "            \"JavaScript\": \"JavaScript\",\r\n" + //
                    "            \"Scala\": \"Scala\",\r\n" + //
                    "            \"Haskell\": \"Haskell\",\r\n" + //
                    "            \"OCaml\": \"OCaml\",\r\n" + //
                    "            \"Rust\": \"Rust\",\r\n" + //
                    "            \"Go\": \"Go\",\r\n" + //
                    "            \"Kotlin\": \"Kotlin\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"Aizu_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"ACdream\": {\r\n" + //
                    "        \"name\": \"ACdream\",\r\n" + //
                    "        \"homepage\": \"http://acdream.info\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"C\",\r\n" + //
                    "            \"2\": \"C++\",\r\n" + //
                    "            \"3\": \"Java\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"ACdream_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CodeForces\": {\r\n" + //
                    "        \"name\": \"CodeForces\",\r\n" + //
                    "        \"homepage\": \"https://codeforces.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"codeforces.com\",\r\n" + //
                    "                \"JSESSIONID\",\r\n" + //
                    "                \"0123456789ABCDEF0123456789ABCDEF\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"CodeForces_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"UVALive\": {\r\n" + //
                    "        \"name\": \"UVALive\",\r\n" + //
                    "        \"homepage\": \"https://icpcarchive.ecs.baylor.edu\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"ANSI C 5.3.0\",\r\n" + //
                    "            \"2\": \"JAVA 1.8.0\",\r\n" + //
                    "            \"3\": \"C++ 5.3.0\",\r\n" + //
                    "            \"4\": \"PASCAL 3.0.0\",\r\n" + //
                    "            \"5\": \"C++11 5.3.0\",\r\n" + //
                    "            \"6\": \"PYTH3 3.5.1\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"UVA_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Yosupo\": {\r\n" + //
                    "        \"name\": \"Yosupo\",\r\n" + //
                    "        \"homepage\": \"https://v2.api.judge.yosupo.jp\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"cpp-func\": \"C++23(Function) (GCC 14.2 + AC Library(1.5.1))\",\r\n" + //
                    "            \"cpp\": \"C++23 (GCC 14.2 + AC Library(1.5.1))\",\r\n" + //
                    "            \"cpp20\": \"C++20 (GCC 14.2 + AC Library(1.5.1))\",\r\n" + //
                    "            \"cpp17\": \"C++17 (GCC 14.2 + AC Library(1.5.1))\",\r\n" + //
                    "            \"rust\": \"Rust (rustc(1.71.1 edition 2021))\",\r\n" + //
                    "            \"d\": \"LDC2 (ldc2 1.29.0)\",\r\n" + //
                    "            \"java\": \"Java (openjdk 17)\",\r\n" + //
                    "            \"python3\": \"Python3 (python3.10 + numpy + scipy)\",\r\n" + //
                    "            \"pypy3\": \"PyPy3 (pypy3.9-7.3.9)\",\r\n" + //
                    "            \"haskell\": \"GHC (ghc 9.0.2)\",\r\n" + //
                    "            \"csharp\": \"C# (dotnet 7.0)\",\r\n" + //
                    "            \"go\": \"Go (go 1.18.2)\",\r\n" + //
                    "            \"lisp\": \"Common Lisp (sbcl 2.1.5)\",\r\n" + //
                    "            \"crystal\": \"Crystal (crystal 1.9.1)\",\r\n" + //
                    "            \"ruby\": \"Ruby (ruby 2.7.1)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"yosupo_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Kattis\": {\r\n" + //
                    "        \"name\": \"Kattis\",\r\n" + //
                    "        \"homepage\": \"https://open.kattis.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"kattis.com\",\r\n" + //
                    "                \"EduSiteCookie\",\r\n" + //
                    "                \"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"Kattis_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"POJ\": {\r\n" + //
                    "        \"name\": \"POJ\",\r\n" + //
                    "        \"homepage\": \"http://poj.org\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C++98 (MinGW GCC 4.4.0)\",\r\n" + //
                    "            \"1\": \"C99 (MinGW GCC 4.4.0)\",\r\n" + //
                    "            \"2\": \"Java (JDK 6)\",\r\n" + //
                    "            \"3\": \"Pascal (FreePascal 2.2.0)\",\r\n" + //
                    "            \"4\": \"C++98 (MS VC++ 2008 Express)\",\r\n" + //
                    "            \"5\": \"C99 (MS VC++ 2008 Express)\",\r\n" + //
                    "            \"6\": \"Fortran (MinGW GCC 4.4.0)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"poj.org\",\r\n" + //
                    "                \"JSESSIONID\",\r\n" + //
                    "                \"0123456789ABCDEF0123456789ABCDEF\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"poj.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"BZOJ\": {\r\n" + //
                    "        \"name\": \"BZOJ\",\r\n" + //
                    "        \"homepage\": \"https://new.bzoj.org:88\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"c\": \"C\",\r\n" + //
                    "            \"cc.std98\": \"C++ 98\",\r\n" + //
                    "            \"cc.std11\": \"C++ 11\",\r\n" + //
                    "            \"cc.std17\": \"C++ 17\",\r\n" + //
                    "            \"cc.std11O2\": \"C++ 11 O2\",\r\n" + //
                    "            \"cc.std17O2\": \"C++ 17 O2\",\r\n" + //
                    "            \"pas\": \"Pascal\",\r\n" + //
                    "            \"java\": \"Java\",\r\n" + //
                    "            \"py.py2\": \"Python 2\",\r\n" + //
                    "            \"py.py3\": \"Python 3\",\r\n" + //
                    "            \"php\": \"PHP\",\r\n" + //
                    "            \"rs\": \"Rust\",\r\n" + //
                    "            \"hs\": \"Haskell\",\r\n" + //
                    "            \"js\": \"Javascript (JSC)\",\r\n" + //
                    "            \"go\": \"Golang\",\r\n" + //
                    "            \"rb\": \"Ruby\",\r\n" + //
                    "            \"cs\": \"Csharp\",\r\n" + //
                    "            \"jl\": \"Julia\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"bzoj.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Kilonova\": {\r\n" + //
                    "        \"name\": \"Kilonova\",\r\n" + //
                    "        \"homepage\": \"https://kilonova.ro\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"kilonova.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"URAL\": {\r\n" + //
                    "        \"name\": \"URAL\",\r\n" + //
                    "        \"homepage\": \"https://acm.timus.ru\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"URAL_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"LightOJ\": {\r\n" + //
                    "        \"name\": \"LightOJ\",\r\n" + //
                    "        \"homepage\": \"https://lightoj.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"bash\": \"Bash (GNU bash 4.4.20)\",\r\n" + //
                    "            \"c\": \"C (gcc 7.5.0)\",\r\n" + //
                    "            \"clojure\": \"Clojure (Clojure 1.9.0)\",\r\n" + //
                    "            \"cpp\": \"C++ 11 (g++ 7.5.0)\",\r\n" + //
                    "            \"cpp14\": \"C++ 14 (g++ 7.5.0)\",\r\n" + //
                    "            \"cpp17\": \"C++ 17 (g++ 7.5.0)\",\r\n" + //
                    "            \"csharp\": \"C# (Mono C# 4.6.2.0)\",\r\n" + //
                    "            \"dart\": \"Dart (Dart SDK 2.10.4)\",\r\n" + //
                    "            \"elixir\": \"Elixir (Erlang/OTP 20)\",\r\n" + //
                    "            \"erlang\": \"Erlang (Erlang/OTP 20)\",\r\n" + //
                    "            \"go\": \"Go (Go 1.10.4)\",\r\n" + //
                    "            \"groovy\": \"Groovy (Groovy 2.4.16)\",\r\n" + //
                    "            \"haskell\": \"Haskell (Haskell 8.0.2)\",\r\n" + //
                    "            \"java\": \"Java 11 (Openjdk 11.0.9.1)\",\r\n" + //
                    "            \"javascript\": \"JavaScript (NodeJs 8.10.0)\",\r\n" + //
                    "            \"kotlin\": \"Kotlin (Kotlinc-jvm 1.4.21)\",\r\n" + //
                    "            \"lisp\": \"Lisp (GNU Clisp 2.49.60)\",\r\n" + //
                    "            \"lua\": \"Lua (Lua 5.3.3)\",\r\n" + //
                    "            \"objectivec\": \"Objective C (g++ 7.5.0)\",\r\n" + //
                    "            \"pascal\": \"Pascal (Free Pascal 3.0.4)\",\r\n" + //
                    "            \"perl\": \"Perl (Perl 5.26.1)\",\r\n" + //
                    "            \"php\": \"PHP (Php 7.2.24)\",\r\n" + //
                    "            \"python3\": \"Python (Python 3.6.9)\",\r\n" + //
                    "            \"r\": \"R (Rscript 3.4.4)\",\r\n" + //
                    "            \"ruby\": \"Ruby (Ruby 2.5.1)\",\r\n" + //
                    "            \"rust\": \"Rust (Rust 1.43.0)\",\r\n" + //
                    "            \"scala\": \"Scala (Scala 2.11.12)\",\r\n" + //
                    "            \"sql\": \"Sql (SQL Lite 3.22.0)\",\r\n" + //
                    "            \"swift\": \"Swift (Swift 5.3.2)\",\r\n" + //
                    "            \"visualbasic\": \"Visual Basic (VB.net 0.0.0.5943)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"lightoj.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"ZOJ\": {\r\n" + //
                    "        \"name\": \"ZOJ\",\r\n" + //
                    "        \"homepage\": \"https://pintia.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"GCC\": \"C99 (gcc 11.4.0)\",\r\n" + //
                    "            \"GXX\": \"C++17 (g++ 11.4.0)\",\r\n" + //
                    "            \"JAVAC\": \"Java (java 11.0.19)\",\r\n" + //
                    "            \"PYTHON2\": \"Python (python 2.7.17)\",\r\n" + //
                    "            \"PYTHON3\": \"Python (python 3.10.13)\",\r\n" + //
                    "            \"CANGJIE\": \"仓颉 (cjc 0.53.4)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"pintia.cn\",\r\n" + //
                    "                \"PTASession\",\r\n" + //
                    "                \"66666666-6666-6666-6666-666666666666\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"ZOJ_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"EIJudge\": {\r\n" + //
                    "        \"name\": \"EIJudge\",\r\n" + //
                    "        \"homepage\": \"http://acm.mipt.ru\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"Free Pascal\": \"Free Pascal 1.8.2\",\r\n" + //
                    "            \"GNU C\": \"GNU C 3.3.3\",\r\n" + //
                    "            \"GNU C++\": \"GNU C++ 3.3.3\",\r\n" + //
                    "            \"Haskell\": \"Haskell GC 6.8.2\",\r\n" + //
                    "            \"Java\": \"java 1.5.0\",\r\n" + //
                    "            \"Kylix\": \"Kylix 14.5\",\r\n" + //
                    "            \"Lua\": \"Lua 5.1.3\",\r\n" + //
                    "            \"OCaml\": \"Objective Caml 3.10.2\",\r\n" + //
                    "            \"Perl\": \"Perl 5.8.5\",\r\n" + //
                    "            \"Python\": \"Python 2.1.3\",\r\n" + //
                    "            \"Ruby\": \"Ruby 1.8.6\",\r\n" + //
                    "            \"Scheme\": \"mzScheme 301 Swindle\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"eijudge.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"VNOJ\": {\r\n" + //
                    "        \"name\": \"VNOJ\",\r\n" + //
                    "        \"homepage\": \"https://oj.vnoi.info\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"oj.vnoi.info\",\r\n" + //
                    "                \"sessionid\",\r\n" + //
                    "                \"0123456789abcdef0123456789abcdef\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"vnoj_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"UESTC\": {\r\n" + //
                    "        \"name\": \"UESTC\",\r\n" + //
                    "        \"homepage\": \"https://cdoj.site:1443\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C\",\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"2\": \"Java\",\r\n" + //
                    "            \"3\": \"Python3\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"UESTC_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Szkopuł\": {\r\n" + //
                    "        \"name\": \"Szkopuł\",\r\n" + //
                    "        \"homepage\": \"https://szkopul.edu.pl\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C\": \"C (gcc:4.8.2 std=gnu99)\",\r\n" + //
                    "            \"C++\": \"C++ (g++:8.3 std=c++17)\",\r\n" + //
                    "            \"Pascal\": \"Pascal (fpc:2.6.2)\",\r\n" + //
                    "            \"Python\": \"Python (python:3.7 + numpy)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"szkopul.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"UVA\": {\r\n" + //
                    "        \"name\": \"UVA\",\r\n" + //
                    "        \"homepage\": \"https://onlinejudge.org\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"ANSI C 5.3.0\",\r\n" + //
                    "            \"2\": \"JAVA 1.8.0\",\r\n" + //
                    "            \"3\": \"C++ 5.3.0\",\r\n" + //
                    "            \"4\": \"PASCAL 3.0.0\",\r\n" + //
                    "            \"5\": \"C++11 5.3.0\",\r\n" + //
                    "            \"6\": \"PYTH3 3.5.1\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \".onlinejudge.org\",\r\n" + //
                    "                \"b985b4592acb7c5112cce9e4729765d0\",\r\n" + //
                    "                \"df448c6dcd......\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"UVA_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"TopCoder\": {\r\n" + //
                    "        \"name\": \"TopCoder\",\r\n" + //
                    "        \"homepage\": \"https://community.topcoder.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"Java\",\r\n" + //
                    "            \"3\": \"C++\",\r\n" + //
                    "            \"4\": \"C#\",\r\n" + //
                    "            \"5\": \"VB\",\r\n" + //
                    "            \"6\": \"Python\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"topcoder.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Z_trening\": {\r\n" + //
                    "        \"name\": \"Z_trening\",\r\n" + //
                    "        \"homepage\": \"http://www.codah.club\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"Pascal fpc 3.0.0\",\r\n" + //
                    "            \"2\": \"C gcc 6.3.1\",\r\n" + //
                    "            \"3\": \"C99 gcc 6.3.1\",\r\n" + //
                    "            \"4\": \"C++98 gcc 6.3.1\",\r\n" + //
                    "            \"5\": \"C++11 gcc 6.3.1\",\r\n" + //
                    "            \"6\": \"C++14 gcc 6.3.1\",\r\n" + //
                    "            \"7\": \"Java gcc-gcj 6.3.1\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"z_trening.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"EOlymp\": {\r\n" + //
                    "        \"name\": \"EOlymp\",\r\n" + //
                    "        \"homepage\": \"https://basecamp.eolymp.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"asm:mars4.5\": \"Assembly (Mars 4.5)\",\r\n" + //
                    "            \"bf:1.3\": \"BF 1.3\",\r\n" + //
                    "            \"c:17-gnu10\": \"C 17 (gnu 10.2)\",\r\n" + //
                    "            \"cpp:17-gnu10\": \"C++ 17 (gnu 10.2)\",\r\n" + //
                    "            \"cpp:17-gnu10-extra\": \"C++ 17 (gnu 10.2 with gmp)\",\r\n" + //
                    "            \"cpp:20-gnu10\": \"C++ 20 (gnu 10.2)\",\r\n" + //
                    "            \"cpp:20-gnu10-extra\": \"C++ 20 (gnu 10.2 with gmp)\",\r\n" + //
                    "            \"csharp:5-dotnet\": \"C# (Microsoft .NET 5)\",\r\n" + //
                    "            \"csharp:5-mono\": \"C# (Mono 5.20)\",\r\n" + //
                    "            \"d:1-dmd\": \"D (dmd 2.096)\",\r\n" + //
                    "            \"d:1-gdc\": \"D (gdc 10.2)\",\r\n" + //
                    "            \"dart:2.13\": \"Dart 2.13\",\r\n" + //
                    "            \"go:1.18\": \"Go 1.18\",\r\n" + //
                    "            \"go:1.20\": \"Go 1.20\",\r\n" + //
                    "            \"haskell:8.8-ghc\": \"Haskell (ghc 8.8)\",\r\n" + //
                    "            \"java:1.17\": \"Java (openjdk 1.17)\",\r\n" + //
                    "            \"java:1.21\": \"Java (openjdk 1.21)\",\r\n" + //
                    "            \"js:18\": \"JavaScript (node 18)\",\r\n" + //
                    "            \"kotlin:1.7\": \"Kotlin 1.7\",\r\n" + //
                    "            \"kotlin:1.9\": \"Kotlin 1.9\",\r\n" + //
                    "            \"lua:5.1\": \"Lua 5.1\",\r\n" + //
                    "            \"mysql:8\": \"MySQL 8.0\",\r\n" + //
                    "            \"pascal:3.2\": \"Pascal (fpc 3.2)\",\r\n" + //
                    "            \"perl:5.32\": \"Perl 5.32\",\r\n" + //
                    "            \"php:7.4\": \"PHP 7.4\",\r\n" + //
                    "            \"plain:1\": \"Plain Text\",\r\n" + //
                    "            \"python:3.10-pypy\": \"Python 3.10 (PyPy)\",\r\n" + //
                    "            \"python:3.10-pypy-extra\": \"Python 3.10 (PyPy with extra libs)\",\r\n" + //
                    "            \"python:3.11-ai\": \"Python 3.11 (AI)\",\r\n" + //
                    "            \"python:3.11-python\": \"Python 3.11\",\r\n" + //
                    "            \"python:3.11-python-extra\": \"Python 3.11 (with extra libs)\",\r\n" + //
                    "            \"ruby:2.4\": \"Ruby 2.4\",\r\n" + //
                    "            \"rust:1.46\": \"Rust 1.46\",\r\n" + //
                    "            \"swift:5.6\": \"Swift 5.6\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"basecamp.eolymp.com\",\r\n" + //
                    "                \"access_token\",\r\n" + //
                    "                \"abcdefghijklmnopqrstuvwxyz\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"e-olymp.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HackerRank\": {\r\n" + //
                    "        \"name\": \"HackerRank\",\r\n" + //
                    "        \"homepage\": \"https://www.hackerrank.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"c\": \"c\",\r\n" + //
                    "            \"cpp\": \"cpp\",\r\n" + //
                    "            \"java\": \"java\",\r\n" + //
                    "            \"csharp\": \"csharp\",\r\n" + //
                    "            \"php\": \"php\",\r\n" + //
                    "            \"ruby\": \"ruby\",\r\n" + //
                    "            \"python\": \"python\",\r\n" + //
                    "            \"perl\": \"perl\",\r\n" + //
                    "            \"haskell\": \"haskell\",\r\n" + //
                    "            \"clojure\": \"clojure\",\r\n" + //
                    "            \"scala\": \"scala\",\r\n" + //
                    "            \"lua\": \"lua\",\r\n" + //
                    "            \"go\": \"go\",\r\n" + //
                    "            \"javascript\": \"javascript\",\r\n" + //
                    "            \"erlang\": \"erlang\",\r\n" + //
                    "            \"d\": \"d\",\r\n" + //
                    "            \"ocaml\": \"ocaml\",\r\n" + //
                    "            \"pascal\": \"pascal\",\r\n" + //
                    "            \"python3\": \"python3\",\r\n" + //
                    "            \"groovy\": \"groovy\",\r\n" + //
                    "            \"objectivec\": \"objectivec\",\r\n" + //
                    "            \"fsharp\": \"fsharp\",\r\n" + //
                    "            \"visualbasic\": \"visualbasic\",\r\n" + //
                    "            \"lolcode\": \"lolcode\",\r\n" + //
                    "            \"smalltalk\": \"smalltalk\",\r\n" + //
                    "            \"tcl\": \"tcl\",\r\n" + //
                    "            \"whitespace\": \"whitespace\",\r\n" + //
                    "            \"sbcl\": \"sbcl\",\r\n" + //
                    "            \"java8\": \"java8\",\r\n" + //
                    "            \"octave\": \"octave\",\r\n" + //
                    "            \"racket\": \"racket\",\r\n" + //
                    "            \"rust\": \"rust\",\r\n" + //
                    "            \"bash\": \"bash\",\r\n" + //
                    "            \"r\": \"r\",\r\n" + //
                    "            \"swift\": \"swift\",\r\n" + //
                    "            \"fortran\": \"fortran\",\r\n" + //
                    "            \"cpp14\": \"cpp14\",\r\n" + //
                    "            \"coffeescript\": \"coffeescript\",\r\n" + //
                    "            \"ada\": \"ada\",\r\n" + //
                    "            \"pypy\": \"pypy\",\r\n" + //
                    "            \"pypy3\": \"pypy3\",\r\n" + //
                    "            \"julia\": \"julia\",\r\n" + //
                    "            \"elixir\": \"elixir\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"www.hackerrank.com\",\r\n" + //
                    "                \"remember_hacker_token\",\r\n" + //
                    "                \"Check 'Remember me' when login.\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"hackerrank.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CSG\": {\r\n" + //
                    "        \"name\": \"CSG\",\r\n" + //
                    "        \"homepage\": \"https://cpc.csgrandeur.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C\",\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"3\": \"Java\",\r\n" + //
                    "            \"6\": \"Python3\",\r\n" + //
                    "            \"17\": \"Go\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"csg.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HDU\": {\r\n" + //
                    "        \"name\": \"HDU\",\r\n" + //
                    "        \"homepage\": \"https://acm.hdu.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"G++\",\r\n" + //
                    "            \"1\": \"GCC\",\r\n" + //
                    "            \"2\": \"C++\",\r\n" + //
                    "            \"3\": \"C\",\r\n" + //
                    "            \"4\": \"Pascal\",\r\n" + //
                    "            \"5\": \"Java\",\r\n" + //
                    "            \"6\": \"C#\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"acm.hdu.edu.cn\",\r\n" + //
                    "                \"PHPSESSID\",\r\n" + //
                    "                \"abcdefg\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"HDU_icon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"AtCoder\": {\r\n" + //
                    "        \"name\": \"AtCoder\",\r\n" + //
                    "        \"homepage\": \"https://atcoder.jp\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"atcoder.jp\",\r\n" + //
                    "                \"REVEL_SESSION\",\r\n" + //
                    "                \"b98a1023f4e87...\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"atcoder.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"USACO\": {\r\n" + //
                    "        \"name\": \"USACO\",\r\n" + //
                    "        \"homepage\": \"https://usaco.org\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"C\",\r\n" + //
                    "            \"3\": \"Python 2.7.17\",\r\n" + //
                    "            \"4\": \"Python 3.6.9\",\r\n" + //
                    "            \"6\": \"C++11\",\r\n" + //
                    "            \"7\": \"C++17\",\r\n" + //
                    "            \"9\": \"Java\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"usaco.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"SPOJ\": {\r\n" + //
                    "        \"name\": \"SPOJ\",\r\n" + //
                    "        \"homepage\": \"https://www.spoj.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \".spoj.com\",\r\n" + //
                    "                \"autologin_hash\",\r\n" + //
                    "                \"5bf43f8dc1cb384c3900528a852e33e0\"\r\n" + //
                    "            ],\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \".spoj.com\",\r\n" + //
                    "                \"autologin_login\",\r\n" + //
                    "                \"tourist\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"SPOJ_favicon.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"TLX\": {\r\n" + //
                    "        \"name\": \"TLX\",\r\n" + //
                    "        \"homepage\": \"https://api.tlx.toki.id\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C\": \"C\",\r\n" + //
                    "            \"Cpp11\": \"C++11\",\r\n" + //
                    "            \"Cpp17\": \"C++17\",\r\n" + //
                    "            \"Cpp20\": \"C++20\",\r\n" + //
                    "            \"Go\": \"Go\",\r\n" + //
                    "            \"Java\": \"Java 11\",\r\n" + //
                    "            \"Pascal\": \"Pascal\",\r\n" + //
                    "            \"Python3\": \"Python 3\",\r\n" + //
                    "            \"PyPy3\": \"PyPy 3\",\r\n" + //
                    "            \"Rust2021\": \"Rust 2021\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"tlx.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"OpenJ_NOI\": {\r\n" + //
                    "        \"name\": \"OpenJ_NOI\",\r\n" + //
                    "        \"homepage\": \"http://noi.openjudge.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"G++\": \"G++(9.3(with c++17))\",\r\n" + //
                    "            \"GCC\": \"GCC(9.3)\",\r\n" + //
                    "            \"Java\": \"Java(OpenJDK14)\",\r\n" + //
                    "            \"Pascal\": \"Pascal(FreePascal)\",\r\n" + //
                    "            \"Python3\": \"Python3(3.8)\",\r\n" + //
                    "            \"Go\": \"Go(1.14.3)\",\r\n" + //
                    "            \"Rust\": \"Rust(1.15)\",\r\n" + //
                    "            \"PHP\": \"PHP(7.4.3)\",\r\n" + //
                    "            \"JavaScript\": \"JavaScript(node10.19.0)\",\r\n" + //
                    "            \"Ruby\": \"Ruby(2.7)\",\r\n" + //
                    "            \"Haskell\": \"Haskell(ghc8.6.5)\",\r\n" + //
                    "            \"R\": \"R(3.6.3)\",\r\n" + //
                    "            \"Lua\": \"Lua(2.1.0)\",\r\n" + //
                    "            \"bash\": \"bash(5.0.17)\",\r\n" + //
                    "            \"awk\": \"awk(5.0.1)\",\r\n" + //
                    "            \"VB\": \"VB(vbnc)\",\r\n" + //
                    "            \"PyPy3\": \"PyPy3(7.3.1)\",\r\n" + //
                    "            \"Perl\": \"Perl(5.30.0)\",\r\n" + //
                    "            \"TypeScript\": \"TypeScript(node10.19.0)\",\r\n" + //
                    "            \"Kotlin\": \"Kotlin(1.5.31)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"poj.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CSU\": {\r\n" + //
                    "        \"name\": \"CSU\",\r\n" + //
                    "        \"homepage\": \"http://acm.csu.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"0\": \"C\",\r\n" + //
                    "            \"1\": \"C++\",\r\n" + //
                    "            \"3\": \"Java\",\r\n" + //
                    "            \"6\": \"Python\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"CSU_favicon.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"CodeChef\": {\r\n" + //
                    "        \"name\": \"CodeChef\",\r\n" + //
                    "        \"homepage\": \"https://www.codechef.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"codechef.com\",\r\n" + //
                    "                \"SESS93b6022d778ee317bf48f7dbffe03173\",\r\n" + //
                    "                \"0123456789ABCDEF0123456789ABCDEF\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"codechef.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"OJUZ\": {\r\n" + //
                    "        \"name\": \"OJUZ\",\r\n" + //
                    "        \"homepage\": \"https://oj.uz/?locale=en\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"ojuz.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"OpenJ_POJ\": {\r\n" + //
                    "        \"name\": \"OpenJ_POJ\",\r\n" + //
                    "        \"homepage\": \"http://poj.openjudge.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"G++\": \"G++(9.3(with c++17))\",\r\n" + //
                    "            \"GCC\": \"GCC(9.3)\",\r\n" + //
                    "            \"Java\": \"Java(OpenJDK14)\",\r\n" + //
                    "            \"Pascal\": \"Pascal(FreePascal)\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"poj.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HihoCoder\": {\r\n" + //
                    "        \"name\": \"HihoCoder\",\r\n" + //
                    "        \"homepage\": \"https://hihocoder.com\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"GCC\": \"GCC\",\r\n" + //
                    "            \"G++\": \"G++\",\r\n" + //
                    "            \"C#\": \"C#\",\r\n" + //
                    "            \"Java\": \"Java\",\r\n" + //
                    "            \"Python2\": \"Python2\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"hiho.jpg\"\r\n" + //
                    "    },\r\n" + //
                    "    \"QOJ\": {\r\n" + //
                    "        \"name\": \"QOJ\",\r\n" + //
                    "        \"homepage\": \"https://qoj.ac\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"qoj.ac\",\r\n" + //
                    "                \"UOJSESSID\",\r\n" + //
                    "                \"\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"UOJ.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"洛谷\": {\r\n" + //
                    "        \"name\": \"洛谷\",\r\n" + //
                    "        \"homepage\": \"https://www.luogu.com.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"Pascal\",\r\n" + //
                    "            \"2\": \"C\",\r\n" + //
                    "            \"3\": \"C++98\",\r\n" + //
                    "            \"4\": \"C++11\",\r\n" + //
                    "            \"7\": \"Python 3\",\r\n" + //
                    "            \"8\": \"Java 8\",\r\n" + //
                    "            \"9\": \"Node.js LTS\",\r\n" + //
                    "            \"11\": \"C++14\",\r\n" + //
                    "            \"12\": \"C++17\",\r\n" + //
                    "            \"13\": \"Ruby\",\r\n" + //
                    "            \"14\": \"Go\",\r\n" + //
                    "            \"15\": \"Rust\",\r\n" + //
                    "            \"16\": \"PHP\",\r\n" + //
                    "            \"17\": \"C# Mono\",\r\n" + //
                    "            \"19\": \"Haskell\",\r\n" + //
                    "            \"21\": \"Kotlin/JVM\",\r\n" + //
                    "            \"22\": \"Scala\",\r\n" + //
                    "            \"23\": \"Perl\",\r\n" + //
                    "            \"25\": \"PyPy 3\",\r\n" + //
                    "            \"27\": \"C++20\",\r\n" + //
                    "            \"28\": \"C++14 (GCC 9)\",\r\n" + //
                    "            \"30\": \"OCaml\",\r\n" + //
                    "            \"31\": \"Julia\",\r\n" + //
                    "            \"32\": \"Lua\",\r\n" + //
                    "            \"33\": \"Java 21\"\r\n" + //
                    "        },\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"luogu.com.cn\",\r\n" + //
                    "                \"__client_id\",\r\n" + //
                    "                \"0123456789012345678901234567890123456789\"\r\n" + //
                    "            ],\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"luogu.com.cn\",\r\n" + //
                    "                \"_uid\",\r\n" + //
                    "                \"66666\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"luogu.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"HIT\": {\r\n" + //
                    "        \"name\": \"HIT\",\r\n" + //
                    "        \"homepage\": \"http://acm.hit.edu.cn\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C++\": \"C++\",\r\n" + //
                    "            \"C89\": \"C89\",\r\n" + //
                    "            \"Java\": \"Java\",\r\n" + //
                    "            \"Pascal\": \"Pascal\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"HIT.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Toph\": {\r\n" + //
                    "        \"name\": \"Toph\",\r\n" + //
                    "        \"homepage\": \"https://toph.co\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"toph.co\",\r\n" + //
                    "                \"t\",\r\n" + //
                    "                \"MTY5NjA...\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"toph.png\"\r\n" + //
                    "    },\r\n" + //
                    "    \"Minieye\": {\r\n" + //
                    "        \"name\": \"Minieye\",\r\n" + //
                    "        \"homepage\": \"https://oj.minieye.tech\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"C\": \"GCC 5.4\",\r\n" + //
                    "            \"C++\": \"G++ 5.4\",\r\n" + //
                    "            \"Java\": \"OpenJDK 1.8\",\r\n" + //
                    "            \"Python3\": \"Python 3.5\",\r\n" + //
                    "            \"Golang\": \"Go 1.11\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": false,\r\n" + //
                    "        \"faviconUrl\": \"minieye.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"NBUT\": {\r\n" + //
                    "        \"name\": \"NBUT\",\r\n" + //
                    "        \"homepage\": \"https://ac.2333.moe\",\r\n" + //
                    "        \"languages\": {\r\n" + //
                    "            \"1\": \"GCC\",\r\n" + //
                    "            \"2\": \"G++\",\r\n" + //
                    "            \"4\": \"FPC\"\r\n" + //
                    "        },\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"NBUT_icon.jpg\"\r\n" + //
                    "    },\r\n" + //
                    "    \"计蒜客\": {\r\n" + //
                    "        \"name\": \"计蒜客\",\r\n" + //
                    "        \"homepage\": \"https://www.jisuanke.com\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \".jisuanke.com\",\r\n" + //
                    "                \"JSKUSS\",\r\n" + //
                    "                \"eyJ000000000000000000\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"jisuanke.ico\"\r\n" + //
                    "    },\r\n" + //
                    "    \"UniversalOJ\": {\r\n" + //
                    "        \"name\": \"UniversalOJ\",\r\n" + //
                    "        \"homepage\": \"https://uoj.ac\",\r\n" + //
                    "        \"languages\": {},\r\n" + //
                    "        \"vapt\": [\r\n" + //
                    "            [\r\n" + //
                    "                \"0\",\r\n" + //
                    "                \"uoj.ac\",\r\n" + //
                    "                \"UOJSESSID\",\r\n" + //
                    "                \"\"\r\n" + //
                    "            ]\r\n" + //
                    "        ],\r\n" + //
                    "        \"isAlive\": true,\r\n" + //
                    "        \"faviconUrl\": \"UOJ.ico\"\r\n" + //
                    "    }\r\n" + //
                    "}" + //
                    "}";

            // 创建ObjectMapper对象
            ObjectMapper mapper = new ObjectMapper();
            // 解析JSON为JsonNode
            JsonNode rootNode = mapper.readTree(json);

            JsonNode remoteOJsNode = rootNode.path("remoteOJs");

            // 遍历 remoteOJsNode 中的所有 OJ 键
            Iterator<String> ojKeys = remoteOJsNode.fieldNames();

            while (ojKeys.hasNext()) {
                String ojKey = ojKeys.next();
                JsonNode ojNode = remoteOJsNode.path(ojKey);

                Map<String, String> languagesMap = new HashMap<>();

                // 提取 languages 字段的键值对
                if (ojNode.has("languages")) {
                    JsonNode languagesNode = ojNode.get("languages");
                    Iterator<String> languageKeys = languagesNode.fieldNames();

                    while (languageKeys.hasNext()) {
                        String langKey = languageKeys.next();
                        languagesMap.put(langKey, languagesNode.get(langKey).asText());
                    }

                    // 将该 OJ 的语言映射存入 ojLanguagesMap
                    ojLanguagesMap.put(ojKey, languagesMap);
                }
            }
        } catch (IOException e) {

        }
    }

    @Override
    public List<Language> buildLanguageList() {
        List<Language> languageList = new ArrayList<>();

        // 打印或处理所有 OJ 的语言映射
        for (Map.Entry<String, Map<String, String>> entry : ojLanguagesMap.entrySet()) {
            String ojName = entry.getKey();
            Map<String, String> languages = entry.getValue();

            for (Map.Entry<String, String> language : languages.entrySet()) {

                languageList.add(new Language()
                        .setName(language.getValue())
                        .setDescription(language.getValue())
                        .setKey(language.getKey())
                        .setOj(getOJName() + "_" + ojName)
                        .setContentType(getLangContentType(language.getValue())));
            }

        }

        return languageList;
    }

    @Override
    public String getLanguageNameById(String id, String oj) {
        return ojLanguagesMap.get(oj).get(id);
    }

    @Override
    public List<Language> buildLanguageListByIds(List<Language> allLanguageList, List<String> langIdList) {
        return allLanguageList.stream().filter(language -> langIdList.contains(language.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Language> buildAddLanguageList(List<Language> allLanguageList, List<Pair_<String, String>> langList,
            String oj) {
        List<Language> addLanguageList = new ArrayList<>();

        for (Pair_<String, String> lang : langList) {
            String name = lang.getValue();
            String key = lang.getKey();

            // 使用 stream 来检查是否已经存在该语言
            boolean isOk = allLanguageList.stream().noneMatch(l -> l.getName().equals(name));

            if (isOk) {
                addLanguageList.add(new Language()
                        .setName(name)
                        .setDescription(name)
                        .setKey(key)
                        .setOj(oj)
                        .setContentType(getLangContentType(name)));
            }
        }

        return addLanguageList;
    }

    @Override
    public Collection<String> getLangList() {
        return null;
    }

    @Override
    public String getOJName() {
        return "VJ";
    }

    public static Map<String, String> getLanguage(String oj) {
        return ojLanguagesMap.get(oj);
    }

}