package cloud.ohiyou;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author ohiyou
 * @since ${DATE} ${TIME}
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // 记录开始时间
        long startTime = System.nanoTime();

        // 创建HttpClient实例
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // 获取环境变量
        String cookieValue = System.getenv("COOKIE");
         cookieValue = "bbs_sid=nu7tm5k3o2n6p0572i0ifjfum0; Hm_lvt_4ab5ca5f7f036f4a4747f1836fffe6f2=1702434178,1703638979,1704182270; bbs_token=B_2B_2BEx5cgZMw0yOZLu9VkMWbf222vbux51Sf1vfkNXtrK_2BpnfiP77eqmO_2B8AX2P6pOjNyDfc3kKnCuQfU8M6yslx2cX4nKC_2BM; 75522e99ef4ef3be3069767a423f422d=7cf4025beefac8a398128e45d1462d9b";
        String serverChanKey = System.getenv("SERVER_CHAN");

        if (cookieValue == null) {
            publishWechat(httpClient, serverChanKey, new ResultVO(-1, "COOKIE 环境变量未设置"), null);
            throw new RuntimeException("COOKIE 环境变量未设置");
        }

        // 发送签到请求
        HttpPost httpPost = new HttpPost("https://www.hifini.com/sg_sign.htm");
        httpPost.setHeader("Cookie", cookieValue);
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");

        HttpResponse response = httpClient.execute(httpPost);

        // 执行结束时间
        long endTime = System.nanoTime();

        // 执行时间
        Long duration = (endTime - startTime) / 1000000;

        // 读取响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String readLine;
        StringBuffer result = new StringBuffer();
        while ((readLine = reader.readLine()) != null) {
            result.append(readLine);
        }

        System.out.println(result);
        ResultVO resultVO = toResultVO(result);

        // 发送微信推送
        if (serverChanKey != null) {
            publishWechat(httpClient, serverChanKey, resultVO, duration);
        }

        // 关闭client
        httpClient.close();
    }

    private static ResultVO toResultVO(StringBuffer result) {
        return JSONObject.parseObject(result.toString(), ResultVO.class);
    }

    public static void publishWechat(CloseableHttpClient httpClient, String serverChanKey, ResultVO resultVO, Long duration) {
        String title;
        if (resultVO.getCode() == -1) {
            title = "HiFiNi签到失败";
        } else {
            title = "HiFiNi签到成功";
        }

        if (duration != null) {
            title += "，耗时" + duration + "ms";
        }
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet("https://sctapi.ftqq.com/" + serverChanKey + ".send?title=" + URLEncoder.encode(title, "UTF-8") + "&desp=" + URLEncoder.encode(resultVO.getMessage()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}