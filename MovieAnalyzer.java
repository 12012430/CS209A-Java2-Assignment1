import static java.util.stream.Collectors.summingInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MovieAnalyzer {
    private class MovieInfo {
        String Series_Title;
        int Released_Year;
        String Certificate;
        int Runtime;
        List<String> Genre;
        float IMDB_Rating;
        String Overview;
        int Meta_score;
        String Director;
        String Star1;
        String Star2;
        String Star3;
        String Star4;
        int Noofvotes;
        long Gross; //-1 means no data

        public int getReleased_Year() {
            return Released_Year;
        }

        public List<String> getGenre() {
            return Genre;
        }

        public List<String> getStars() {
            List<String> stars = new ArrayList<>();
            stars.add(Star1);
            stars.add(Star2);
            stars.add(Star3);
            stars.add(Star4);
            return stars;
        }

        public int getRuntime() {
            return Runtime;
        }

        public String getSeries_Title() {
            return Series_Title.replace("|", ",").replace("\"", "");
        }

        public long getGross() {
            return Gross;
        }

        public double getIMDB_Rating() {
            return IMDB_Rating;
        }

        public String getOverview() {
            return Overview.replace("|", "");
        }

        public int getOverviewLength() {
            return Overview.length();
        }
    }

    Stream<MovieInfo> movieList;
    List<MovieInfo> movieTemp;

    public MovieAnalyzer(String dataset_path) {
        File file = new File(dataset_path);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            //BufferedReader br = new BufferedReader(new FileReader(file), StandardCharsets.UTF_8);
            movieList = br.lines().skip(1).map(line -> {
                Pattern pattern1 = Pattern.compile("https[\\w\\W]+.jpg");
                String preprocess1 = line;
                Matcher m1 = pattern1.matcher(preprocess1);
                Pattern pattern2 = Pattern.compile(",,");
                String preprocess2 = m1.replaceAll("fuck");
                Matcher m2 = pattern2.matcher(preprocess2);
                Pattern pattern3 = Pattern.compile(",$");
                String preprocess3 = m2.replaceAll(",-1,");
                Matcher m3 = pattern3.matcher(preprocess3);
                String preprocess4 = m3.replaceAll(",-1");
                Pattern pattern4 = Pattern.compile(",(?!(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                Matcher m4 = pattern4.matcher(preprocess4);
                String preprocess5 = m4.replaceAll("|");

                String[] data = preprocess5.split(",");
                MovieInfo movieInfo = new MovieInfo();
                movieInfo.Series_Title = data[1];
                movieInfo.Released_Year = Integer.parseInt(data[2]);
                movieInfo.Certificate = data[3];
                movieInfo.Runtime = Integer.parseInt(data[4].replace(" min", ""));
                movieInfo.Genre = new ArrayList<>();
                movieInfo.Genre = List.of(data[5].replace("\"", "").replace(" ", "").split("\\|"));
                movieInfo.IMDB_Rating = Float.parseFloat(data[6]);
                movieInfo.Overview = data[7].replace("\"\"", "||").replace("\"", "");
                movieInfo.Meta_score = Integer.parseInt(data[8]);
                movieInfo.Director = data[9];
                movieInfo.Star1 = data[10];
                movieInfo.Star2 = data[11];
                movieInfo.Star3 = data[12];
                movieInfo.Star4 = data[13];
                movieInfo.Noofvotes = Integer.parseInt(data[14]);
                movieInfo.Gross = Long.parseLong(data[15].replace("|", "").replace("\"", ""));
                return movieInfo;
            });
            movieTemp = movieList.collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        Map<Integer, Integer> movieCountByYear = movieListTemp.sorted(Comparator.comparing(MovieInfo::getReleased_Year, Comparator.reverseOrder())).collect(Collectors.groupingBy(MovieInfo::getReleased_Year, LinkedHashMap::new, summingInt(x -> 1)));
        return movieCountByYear;
    }

    public Map<String, Integer> getMovieCountByGenre() {
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        Map<String, Integer> movieCountByGenre = new LinkedHashMap<>();
        movieListTemp.flatMap(x -> x.getGenre().stream()).forEach(x -> movieCountByGenre.put(x, movieCountByGenre.getOrDefault(x, 0) + 1));
        Map<String, Integer> ans = new LinkedHashMap<>();
        movieCountByGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> ans.put(entry.getKey(), entry.getValue()));
        return ans;
    }

    public Map<List<String>, Integer> getCoStarCount() {
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        Map<List<String>, Integer> coStarCount = new LinkedHashMap<>();
        movieListTemp.forEach(x -> {
            List<String> stars = x.getStars();
            for (int i = 0; i < stars.size(); i++) {
                Boolean tag = false;
                for (int j = i + 1; j < stars.size(); j++) {
                    if (stars.get(i).equals(stars.get(j))) {
                        if (tag == true) {
                            continue;
                        } else {
                            tag = true;
                        }
                    }
                    List<String> pair = new ArrayList<>();
                    pair.add(stars.get(i));
                    pair.add(stars.get(j));
                    pair.sort(String::compareTo);
                    coStarCount.put(pair, coStarCount.getOrDefault(pair, 0) + 1);
                }
            }
        });
        return coStarCount;
    }

    public List<String> getTopMovies(int top_k, String by) {
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        List<String> topMovies = new ArrayList<>();
        if (by.equals("runtime")) {
            movieListTemp.sorted(Comparator.comparing(MovieInfo::getRuntime, Comparator.reverseOrder()).thenComparing(MovieInfo::getSeries_Title)).limit(top_k).forEach(x -> topMovies.add(x.getSeries_Title()));
        } else if (by.equals("overview")) {
            movieListTemp.sorted(Comparator.comparing(MovieInfo::getOverviewLength, Comparator.reverseOrder()).thenComparing(MovieInfo::getSeries_Title)).limit(top_k).forEach(x -> topMovies.add(x.getSeries_Title()));
        }
        return topMovies;
    }

    public List<String> getTopStars(int top_k, String by) {
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        List<String> topStars = new ArrayList<>();
        if (by.equals("rating")) {
            Map<String, Double> getTopStars = new LinkedHashMap();
            Map<String, Integer> countTopStars = new LinkedHashMap();
            movieListTemp.forEach(x -> {
                List<String> stars = x.getStars();
                Double imdb = x.getIMDB_Rating();
                for (int i = 0; i < stars.size(); i++) {
                    getTopStars.put(stars.get(i), getTopStars.getOrDefault(stars.get(i), (double) 0) + imdb);
                    countTopStars.put(stars.get(i), countTopStars.getOrDefault(stars.get(i), 0) + 1);
                }
            });
            for (String key : getTopStars.keySet()) {
                getTopStars.put(key, getTopStars.get(key) / (double) countTopStars.get(key));
            }
            getTopStars.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(top_k)
                    .forEach(entry -> topStars.add(entry.getKey()));
        } else if (by.equals("gross")) {
            Map<String, Long> getTopStars = new LinkedHashMap();
            Map<String, Integer> countTopStars = new LinkedHashMap();
            movieListTemp.forEach(x -> {
                List<String> stars = x.getStars();
                long gross = x.getGross();
                for (int i = 0; i < stars.size(); i++) {
                    if (gross != -1) {
                        getTopStars.put(stars.get(i), getTopStars.getOrDefault(stars.get(i), (long) 0) + gross);
                        countTopStars.put(stars.get(i), countTopStars.getOrDefault(stars.get(i), 0) + 1);
                    }
                }
            });
            for (String key : getTopStars.keySet()) {
                getTopStars.put(key, getTopStars.get(key) / (long) countTopStars.get(key));
            }
            getTopStars.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(top_k)
                    .forEach(entry -> topStars.add(entry.getKey()));
        }
        return topStars;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        List<String> searchMovies = new ArrayList<>();
        Stream<MovieInfo> movieListTemp = movieTemp.stream();
        movieListTemp.filter(x -> x.getGenre().contains(genre) && x.getIMDB_Rating()
                >= min_rating && x.getRuntime() <= max_runtime).sorted(Comparator
                .comparing(x -> x.getSeries_Title())).forEach(x -> searchMovies
                .add(x.getSeries_Title()));
        return searchMovies;
    }

}