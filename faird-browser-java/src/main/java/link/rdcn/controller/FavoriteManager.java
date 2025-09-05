package link.rdcn.controller;
import java.util.ArrayList;
import java.util.List;

//public class FavoriteManager {
//    private static final List<String> favorites = new ArrayList<>();
//
//    public static void addFavorite(String url) {
//        if (!favorites.contains(url)) {
//            favorites.add(url);
//        }
//    }
//
//    public static List<String> getFavorites() {
//        return new ArrayList<>(favorites); // 返回副本，防止外部修改
//    }
//}

public class FavoriteManager {
    private static final List<String> favorites = new ArrayList<>();

    public static void addFavorite(String url) {
        if (!favorites.contains(url)) {
            favorites.add(url);
        }
    }

    public static void removeFavorite(String url) {
        favorites.remove(url);
    }

    public static Boolean containFavorites(String url) {
        return favorites.contains(url);
    }

    public static List<String> getFavorites() {
        return new ArrayList<>(favorites); // 返回副本，避免外部直接修改
    }

    public static boolean isFavorite(String url) {
        return favorites.contains(url);
    }
}
