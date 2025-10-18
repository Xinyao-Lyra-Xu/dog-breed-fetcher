package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        // ① 规范化输入：去空格 + 小写
        String normalized = (breed == null) ? "" : breed.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            // 用“breed 名”的构造器，语义：这个 breed 不存在 / 非法
            throw new BreedNotFoundException("(empty)");
        }

        String url = "https://dog.ceo/api/breed/" + normalized + "/list";
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                // ② 非 2xx：保留原始原因，不套“Breed not found:”前缀
                throw new BreedNotFoundException("HTTP " + response.code() + " when fetching: " + normalized, null);
            }

            String json = response.body().string();
            JSONObject obj = new JSONObject(json);

            String status = obj.optString("status", "");
            if (!"success".equalsIgnoreCase(status)) {
                // ③ API 明确说这个主品种不存在时，用“breed 名”构造器
                throw new BreedNotFoundException(normalized);
            }

            JSONArray arr = obj.optJSONArray("message");
            List<String> result = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    result.add(arr.getString(i));
                }
            }
            return Collections.unmodifiableList(result);

        } catch (IOException | org.json.JSONException e) {
            // ④ 网络/解析错误 → 也按作业要求包装为 BreedNotFoundException（保留 cause）
            throw new BreedNotFoundException("Failed to fetch sub-breeds for: " + normalized, e);
        }
    }
}