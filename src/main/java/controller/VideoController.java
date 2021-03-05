package controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import entiy.Video;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class VideoController {

    /*
    * 抖音获取ids
    * */
    @RequestMapping("/videoZMDY")
    @ResponseBody
    public Video videoZM(@RequestParam String url, HttpServletRequest request,HttpServletResponse response){
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(5000);
        String videourl = conn.getHeaderField("Location");//获取到抖音链接
        String ids = videourl.substring(38,57);   //获取到ids
        Video video = new Video();
        if(ids!="" || ids!=null){
            video.setIds(ids);
            video.setStatus("200");
        }else{
            video.setIds("");
            video.setStatus("500");
        }
        return video;
    }

    /*
     * 获取抖音对外视频地址
     * */
    @RequestMapping("getVideo")
    @ResponseBody
    private Object getVideo(String url,String ids){
        //https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=6910519894912109827
        if(url == null || url.trim().isEmpty())
            return null;
        //创建一个HttpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        //创建一个HttpGet对象，这里需要指定一个请求的url
        HttpGet get = new HttpGet(url+ids);
        //执行http请求
        CloseableHttpResponse response = null;
        String result ="";
        try {
            response = httpClient.execute(get);
            //接收返回结果的对象
            HttpEntity entity = response.getEntity();
            //获取响应内容数据
            result = EntityUtils.toString(entity);
            //关闭流
            response.close();
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /*
    * 获取抖音302跳转真实地址
    * */
    @RequestMapping("DYDownloadVideo")
    @ResponseBody
    private Video DYDownloadVideo(String url, HttpServletResponse response, HttpSession session) throws Exception {
        HashMap<String, String> headers = MapUtil.newHashMap();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Mobile Safari/537.36");
        String redirectUrl = HttpUtil.createGet(url).addHeaders(headers).execute().header("Location");//跳转后网页


        //时间格式化格式
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        //获取当前时间并作为时间戳
        String timeStamp = simpleDateFormat.format(new Date());
        //获取全路径
        String filePath = session.getServletContext().getRealPath("/upload");
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        FileOutputStream out = null;
        Video video = null;
        try
        {
            File file0=new File(filePath);
            if(!file0.isDirectory()&&!file0.exists()){
                file0.mkdirs();
                File file=new File(filePath+"\\"+timeStamp+".mp4");

            }
            out = new FileOutputStream(file0+"\\"+timeStamp+".mp4");
            // 建立链接
            URL httpUrl=new URL(redirectUrl);
            conn=(HttpURLConnection) httpUrl.openConnection();
            //以Post方式提交表单，默认get方式
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // post方式不能使用缓存
            conn.setUseCaches(false);
            //连接指定的资源
            conn.connect();
            //获取网络输入流
            inputStream=conn.getInputStream();
            bis = new BufferedInputStream(inputStream);
            byte b [] = new byte[1024];
            int len = 0;
            while((len=bis.read(b))!=-1){
                out.write(b, 0, len);
            }
            System.out.println("下载完成...");
            video = new Video(timeStamp+".mp4","200");
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                if(out!=null){
                    out.close();
                }
                if(bis!=null){
                    bis.close();
                }
                if(inputStream!=null){
                    inputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        return video;
    }

    @RequestMapping("videoZMKS")
    @ResponseBody
    public Video ksParseUrl(String url) {
        HashMap<String, String> headers = MapUtil.newHashMap();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Mobile Safari/537.36");
        String redirectUrl = HttpUtil.createGet(url).addHeaders(headers).execute().header("Location");//跳转后网页
        String body = HttpUtil.createGet(redirectUrl).addHeaders(headers).execute().body();//获取网页body
        Document doc = Jsoup.parse(body);//将body转为document元素
        Elements videoElement = doc.select("script[type=text/javascript]");
        String videoInfo = videoElement.get(3).data().replaceAll("window.pageData= ","");
        JSONObject json =JSONObject.parseObject(videoInfo);
        System.out.println(json);
        String title = json.getJSONObject("video").getString("caption");
        String videoUrl=json.getJSONObject("video").getString("srcNoMark");
        videoUrl=videoUrl.substring(0,videoUrl.indexOf("?"));//获取到无水印视频链接
        System.out.println(videoUrl);
        Video video = new Video(videoUrl,"200");
        return video;
    }
}
