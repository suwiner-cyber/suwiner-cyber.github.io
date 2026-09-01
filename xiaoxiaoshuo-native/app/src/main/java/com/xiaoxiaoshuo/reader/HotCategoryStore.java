package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class HotCategoryStore {
    public interface Callback { void onLoaded(List<Item> items, boolean fromCache); }
    public static final String[] CATEGORIES={"男频热门","女频热门","玄幻","奇幻","武侠","仙侠","都市","职场","历史","军事","悬疑","推理","科幻","末世","游戏","体育","轻小说","现实","青春","古言","现言","豪门","种田","无限流"};
    private static final Map<String,String[]> SEEDS=new HashMap<>();
    static{
        SEEDS.put("男频热门",new String[]{"剑来","诡秘之主","大奉打更人","庆余年","雪中悍刀行","凡人修仙传","遮天","斗破苍穹"});
        SEEDS.put("女频热门",new String[]{"知否知否应是绿肥红瘦","星汉灿烂幸甚至哉","簪中录","第一侯","表妹万福","七星彩"});
        SEEDS.put("玄幻",new String[]{"斗破苍穹","遮天","完美世界","牧神记","武动乾坤","大主宰"});
        SEEDS.put("奇幻",new String[]{"诡秘之主","宿命之环","盘龙","恶魔法则","紫川","奥术神座"});
        SEEDS.put("武侠",new String[]{"雪中悍刀行","剑来","昆仑","沧海","英雄志","有匪"});
        SEEDS.put("仙侠",new String[]{"凡人修仙传","一念永恒","仙逆","大道朝天","将夜","择天记"});
        SEEDS.put("都市",new String[]{"大王饶命","修真聊天群","我真没想重生啊","俗人回档","重生之财源滚滚","黄金瞳"});
        SEEDS.put("职场",new String[]{"杜拉拉升职记","输赢","圈子圈套","浮沉","猎场","格局逆袭"});
        SEEDS.put("历史",new String[]{"庆余年","赘婿","唐砖","回到明朝当王爷","新宋","宰执天下"});
        SEEDS.put("军事",new String[]{"弹痕","狼群","最后一颗子弹留给我","特种兵之利刃出鞘","国之利刃","佣兵的战争"});
        SEEDS.put("悬疑",new String[]{"十宗罪","心理罪","法医秦明","白夜行","长夜难明","无证之罪"});
        SEEDS.put("推理",new String[]{"长夜难明","无证之罪","坏小孩","死亡通知单","谋杀官员","心理罪"});
        SEEDS.put("科幻",new String[]{"三体","吞噬星空","全球高武","深空彼岸","小兵传奇","间客"});
        SEEDS.put("末世",new String[]{"全球进化","黑暗血时代","末日乐园","末世大回炉","狩魔手记","灾厄纪元"});
        SEEDS.put("游戏",new String[]{"全职高手","网游之近战法师","惊悚乐园","网游之纵横天下","超神机械师","重生之贼行天下"});
        SEEDS.put("体育",new String[]{"冠军之光","胜者为王","禁区之雄","篮坛教皇","我们是冠军","足球修改器"});
        SEEDS.put("轻小说",new String[]{"异常生物见闻录","希灵帝国","我的女友是恶女","东京绅士物语","二次元之悠闲","我的妹妹是偶像"});
        SEEDS.put("现实",new String[]{"平凡的世界","人世间","大江大河","山海情","装台","乔家的儿女"});
        SEEDS.put("青春",new String[]{"最好的我们","你好旧时光","暗恋橘生淮南","那些回不去的年少时光","致我们终将逝去的青春","匆匆那年"});
        SEEDS.put("古言",new String[]{"知否知否应是绿肥红瘦","星汉灿烂幸甚至哉","簪中录","第一侯","表妹万福","七星彩"});
        SEEDS.put("现言",new String[]{"你是我的荣耀","何以笙箫默","微微一笑很倾城","偷偷藏不住","难哄","骄阳似我"});
        SEEDS.put("豪门",new String[]{"何以笙箫默","你和我的倾城时光","良辰讵可待","盛开","佳期如梦","十年一品温如言"});
        SEEDS.put("种田",new String[]{"知否知否应是绿肥红瘦","农家小福女","丑女种田山里汉宠妻无度","田园闺事","悠闲小农女","山村一亩三分地"});
        SEEDS.put("无限流",new String[]{"无限恐怖","王牌进化","惊悚乐园","轮回乐园","诸天尽头","从姑获鸟开始"});
    }

    public static final class Item{
        public LegacyDexBridge.BookResult book;
        public int chapters;
        Item(LegacyDexBridge.BookResult b,int c){book=b;chapters=c;}
    }
    private HotCategoryStore(){}

    public static void load(Context c,String category,Callback cb){
        List<Item> cached=read(c,category);
        boolean today=isToday(c,category);
        if(!cached.isEmpty())cb.onLoaded(cached,true);
        if(today&&!cached.isEmpty())return;
        new Thread(()->{List<Item> fresh=refresh(c,category);if(!fresh.isEmpty()){write(c,category,fresh);cb.onLoaded(fresh,false);}}).start();
    }

    private static List<Item> refresh(Context c,String category){
        LinkedHashMap<String,Item> map=new LinkedHashMap<>();
        LegacySourceStore.State st=LegacySourceStore.prepare(c,80);
        ArrayList<LegacySourceStore.SourceInfo> src=new ArrayList<>(st.selected);
        if(src.size()>28)src=new ArrayList<>(src.subList(0,28));
        ExecutorService pool=Executors.newFixedThreadPool(8);CountDownLatch latch=new CountDownLatch(src.size());
        for(LegacySourceStore.SourceInfo s:src){pool.submit(()->{try{List<LegacyDexBridge.BookResult> rs=LegacyDexBridge.get(c).search(s,category,1);int inspected=0;for(LegacyDexBridge.BookResult b:rs){if(inspected++>=8)break;if(!looksLikeBook(b,category))continue;BookSourceResolver.Resolved r=BookSourceResolver.inspect(c,b);if(r==null||r.chapterCount()<20)continue;putBetter(map,r.book,r.chapterCount());}}catch(Throwable ignored){}finally{latch.countDown();}});}
        try{latch.await(35,TimeUnit.SECONDS);}catch(Throwable ignored){}pool.shutdownNow();
        String[] seeds=SEEDS.get(category);if(seeds==null)seeds=new String[0];
        for(String title:seeds){if(map.size()>=24)break;try{BookSourceResolver.Resolved r=BookSourceResolver.bestForTitle(c,title,"",null,18);if(r!=null&&r.chapterCount()>=20)putBetter(map,r.book,r.chapterCount());}catch(Throwable ignored){}}
        ArrayList<Item> out=new ArrayList<>(map.values());Collections.sort(out,(a,b)->Integer.compare(b.chapters,a.chapters));if(out.size()>36)return new ArrayList<>(out.subList(0,36));return out;
    }
    private static synchronized void putBetter(Map<String,Item> map,LegacyDexBridge.BookResult b,int count){String k=BookSourceResolver.norm(b.title);if(k.length()==0)return;Item old=map.get(k);if(old==null||count>old.chapters)map.put(k,new Item(b,count));}
    private static boolean looksLikeBook(LegacyDexBridge.BookResult b,String category){String t=b==null?"":BookSourceResolver.norm(b.title);if(t.length()<2)return false;if(t.equals(BookSourceResolver.norm(category))||t.endsWith("类")||t.contains("排行榜")||t.contains("小说分类"))return false;return true;}

    private static void write(Context c,String cat,List<Item> list){try{JSONArray a=new JSONArray();for(Item it:list){LegacyDexBridge.BookResult b=it.book;JSONObject o=new JSONObject();o.put("title",b.title);o.put("author",b.author);o.put("intro",b.intro);o.put("cover",b.coverUrl);o.put("bookUrl",b.bookUrl);o.put("sourceName",b.sourceName);o.put("sourceUrl",b.sourceUrl);o.put("sourceJson",b.sourceJson);o.put("chapters",it.chapters);a.put(o);}c.getSharedPreferences("hot_category",Context.MODE_PRIVATE).edit().putString("items_"+key(cat),a.toString()).putString("day_"+key(cat),today()).apply();}catch(Throwable ignored){}}
    private static List<Item> read(Context c,String cat){ArrayList<Item> out=new ArrayList<>();try{String raw=c.getSharedPreferences("hot_category",Context.MODE_PRIVATE).getString("items_"+key(cat),"[]");JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;LegacyDexBridge.BookResult b=new LegacyDexBridge.BookResult();b.title=o.optString("title");b.author=o.optString("author");b.intro=o.optString("intro");b.coverUrl=o.optString("cover");b.bookUrl=o.optString("bookUrl");b.sourceName=o.optString("sourceName");b.sourceUrl=o.optString("sourceUrl");b.sourceJson=o.optString("sourceJson");int n=o.optInt("chapters",0);if(b.title.length()>0&&n>0)out.add(new Item(b,n));}}catch(Throwable ignored){}return out;}
    private static boolean isToday(Context c,String cat){return today().equals(c.getSharedPreferences("hot_category",Context.MODE_PRIVATE).getString("day_"+key(cat),""));}
    private static String today(){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());}
    private static String key(String s){return Integer.toHexString((s==null?"":s).hashCode());}
}
