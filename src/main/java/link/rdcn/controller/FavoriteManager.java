//package link.rdcn.controller;
//import java.util.ArrayList;
//import java.util.List;
//
//public class FavoriteManager {
//    private static final List<String> favorites = new ArrayList<>();
//
//    public static void addFavorite(String url) {
//        if (!favorites.contains(url)) {
//            favorites.add(url);
//        }
//    }
//
//    public static void removeFavorite(String url) {
//        favorites.remove(url);
//    }
//
//    public static Boolean containFavorites(String url) {
//        return favorites.contains(url);
//    }
//
//    public static List<String> getFavorites() {
//        return new ArrayList<>(favorites); // 返回副本，避免外部直接修改
//    }
//
//    public static boolean isFavorite(String url) {
//        return favorites.contains(url);
//    }
//}
package link.rdcn.controller;

import java.util.*;

public class FavoriteManager {
    // 使用 Map 存储：name -> url
    private static final Map<String, String> favorites = new HashMap<>();

    public static void addFavorite(String name, String url) {
        favorites.put(name, url);
    }

    public static void removeFavorite(String url) {
        // 根据 url 找到对应的 name 再删除
        favorites.entrySet().removeIf(entry -> entry.getValue().equals(url));
    }

    public static boolean containFavorites(String url) {
        return favorites.containsValue(url);
    }

    public static boolean isFavorite(String url) {
        return favorites.containsValue(url);
    }

    public static String getNameByUrl(String url) {
        return favorites.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(url))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static Map<String, String> getFavorites() {
        return new HashMap<>(favorites); // 返回副本
    }
}
