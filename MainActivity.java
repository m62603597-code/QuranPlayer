package com.quranplayer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    final int GREEN = Color.rgb(23,107,58);
    final int CREAM = Color.rgb(255,253,247);
    final int GOLD = Color.rgb(184,138,42);
    final int TEXT = Color.rgb(29,36,31);

    LinearLayout root, content;
    TextView title;
    ExecutorService executor = Executors.newCachedThreadPool();
    Handler main = new Handler();
    MediaPlayer player;
    ArrayList<Reciter> reciters = new ArrayList<>();
    Reciter selectedReciter;
    float fontSize = 25f;
    int selectedSurah = 1;
    String[] surahNames = {
        "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
        "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه",
        "الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم",
        "لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر",
        "فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق",
        "الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة",
        "الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج",
        "نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس",
        "التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد",
        "الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات",
        "القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر",
        "المسد","الإخلاص","الفلق","الناس"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildShell();
        showHome();
        loadReciters();
    }

    void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(CREAM);
        title = tv("القرآن الكريم",22,true); title.setTextColor(GREEN);
        title.setGravity(Gravity.CENTER);
        title.setPadding(12,20,12,14);
        root.addView(title,new LinearLayout.LayoutParams(-1,70));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(6,5,6,5);
        nav.setBackgroundColor(Color.WHITE);
        nav.addView(navButton("السور",v->showHome()),new LinearLayout.LayoutParams(0,60,1));
        nav.addView(navButton("القارئ",v->showReciters()),new LinearLayout.LayoutParams(0,60,1));
        nav.addView(navButton("بحث",v->showSearch()),new LinearLayout.LayoutParams(0,60,1));
        root.addView(nav);
        setContentView(root);
    }

    Button navButton(String s, View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setTextSize(15); b.setTextColor(TEXT); b.setOnClickListener(l); return b;
    }
    TextView tv(String s,float size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(TEXT); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    void clear(){ content.removeAllViews(); }

    void showHome() {
        clear();
        TextView h=tv("سور القرآن الكريم",21,true); h.setPadding(18,12,18,8); content.addView(h);
        LinearLayout row=new LinearLayout(this); row.setPadding(12,4,12,8);
        Button r=navButton("اختيار القارئ",v->showReciters()); row.addView(r,new LinearLayout.LayoutParams(0,52,1));
        Button q=navButton(selectedReciter==null?"القارئ: اختر":"القارئ: "+selectedReciter.name,v->showReciters()); row.addView(q,new LinearLayout.LayoutParams(0,52,1));
        content.addView(row);
        RecyclerView list=new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(new SurahAdapter());
        content.addView(list,new LinearLayout.LayoutParams(-1,0,1));
    }

    void openSurah(int n) {
        selectedSurah=n; clear();
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); head.setPadding(8,5,8,5);
        Button back=navButton("رجوع",v->showHome()); head.addView(back,new LinearLayout.LayoutParams(80,50));
        TextView st=tv(surahNames[n-1],22,true); st.setGravity(Gravity.CENTER); head.addView(st,new LinearLayout.LayoutParams(0,50,1));
        Button play=navButton("▶ استماع",v->playSelectedSurah()); head.addView(play,new LinearLayout.LayoutParams(110,50));
        content.addView(head);
        LinearLayout controls=new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setPadding(8,0,8,6);
        Button minus=navButton("A−",v->{fontSize=Math.max(18,fontSize-2); loadVerses();});
        Button plus=navButton("A+",v->{fontSize=Math.min(42,fontSize+2); loadVerses();});
        TextView rec=tv(selectedReciter==null?"اختر قارئًا من تبويب القارئ":selectedReciter.name,14,false); rec.setGravity(Gravity.CENTER);
        controls.addView(minus,new LinearLayout.LayoutParams(65,48)); controls.addView(rec,new LinearLayout.LayoutParams(0,48,1)); controls.addView(plus,new LinearLayout.LayoutParams(65,48));
        content.addView(controls);
        ProgressBar p=new ProgressBar(this); content.addView(p,new LinearLayout.LayoutParams(-1,4));
        loadVerses();
    }

    void loadVerses() {
        if(content.getChildCount()>2){ View v=content.getChildAt(content.getChildCount()-1); if(v instanceof ProgressBar){} }
        final int chapter=selectedSurah;
        executor.execute(()->{
            try{
                String url="https://api.quran.com/api/v4/verses/by_chapter/"+chapter+"?language=ar&words=false&fields=text_uthmani,text_uthmani_tajweed&per_page=300";
                JSONObject o=new JSONObject(get(url));
                JSONArray a=o.getJSONArray("verses");
                ArrayList<String> verses=new ArrayList<>();
                for(int i=0;i<a.length();i++){
                    JSONObject v=a.getJSONObject(i);
                    String text=v.optString("text_uthmani_tajweed","");
                    if(text.isEmpty()) text=v.optString("text_uthmani","");
                    verses.add(text+"  ۝"+(i+1));
                }
                main.post(()->renderVerses(verses));
            }catch(Exception e){ main.post(()->showError("تعذر تحميل نص السورة. تأكد من اتصال الإنترنت.")); }
        });
    }

    void renderVerses(ArrayList<String> verses){
        // Preserve the header/controls, replace old verse list.
        while(content.getChildCount()>3) content.removeViewAt(3);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,12,18,30);
        for(String s:verses){
            TextView t=tv(s,fontSize,false); t.setTextDirection(View.TEXT_DIRECTION_RTL); t.setGravity(Gravity.RIGHT);
            t.setLineSpacing(10,1.15f); t.setPadding(4,12,4,12); box.addView(t);
        }
        scroll.addView(box); content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }

    void playSelectedSurah(){
        if(selectedReciter==null){ Toast.makeText(this,"اختر القارئ أولًا من تبويب «القارئ».",Toast.LENGTH_LONG).show(); return; }
        try{
            if(player!=null){player.stop();player.release();}
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
            String url=selectedReciter.server+String.format(Locale.US,"%03d.mp3",selectedSurah);
            player.setDataSource(url); player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp->Toast.makeText(this,"انتهت السورة",Toast.LENGTH_SHORT).show());
            player.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,"تعذر تشغيل التلاوة لهذا القارئ/السورة.",Toast.LENGTH_LONG).show(); return true;});
            player.prepareAsync();
            Toast.makeText(this,"جاري تحميل التلاوة…",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"تعذر تشغيل الصوت.",Toast.LENGTH_SHORT).show();}
    }

    void showReciters(){
        clear();
        LinearLayout top=new LinearLayout(this); top.setPadding(12,8,12,8);
        EditText search=new EditText(this); search.setHint("ابحث عن اسم القارئ…"); search.setSingleLine(true);
        top.addView(search,new LinearLayout.LayoutParams(0,58,1)); content.addView(top);
        RecyclerView list=new RecyclerView(this); list.setLayoutManager(new LinearLayoutManager(this));
        ReciterAdapter adapter=new ReciterAdapter(reciters); list.setAdapter(adapter);
        content.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){adapter.filter(s.toString());} public void afterTextChanged(android.text.Editable e){}
        });
        if(reciters.isEmpty()){
            ProgressBar p=new ProgressBar(this); content.addView(p,new LinearLayout.LayoutParams(-1,70));
        }
    }

    void showSearch(){
        clear();
        EditText search=new EditText(this); search.setHint("اكتب اسم السورة…"); search.setSingleLine(true); search.setPadding(16,10,16,10); content.addView(search,new LinearLayout.LayoutParams(-1,60));
        LinearLayout results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); content.addView(results,new LinearLayout.LayoutParams(-1,0,1));
        search.setOnEditorActionListener((v,a,e)->{String x=search.getText().toString().trim(); for(int i=0;i<surahNames.length;i++) if(surahNames[i].contains(x)){openSurah(i+1);break;} return true;});
        for(int i=0;i<surahNames.length;i++){
            final int n=i+1; Button b=navButton(n+" — "+surahNames[i],v->openSurah(n)); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); results.addView(b,new LinearLayout.LayoutParams(-1,52));
        }
    }

    void loadReciters(){
        executor.execute(()->{
            try{
                JSONObject o=new JSONObject(get("https://mp3quran.net/api/v3/reciters?language=ar"));
                JSONArray a=o.getJSONArray("reciters");
                for(int i=0;i<a.length();i++){
                    JSONObject r=a.getJSONObject(i); JSONArray m=r.optJSONArray("moshaf");
                    if(m==null||m.length()==0) continue;
                    JSONObject first=m.getJSONObject(0);
                    String server=first.optString("server","");
                    if(server.isEmpty()) continue;
                    if(!server.endsWith("/")) server+="/";
                    reciters.add(new Reciter(r.optString("id",""),r.optString("name","قارئ"),server));
                }
                Collections.sort(reciters,(x,y)->x.name.compareTo(y.name));
                main.post(()->{ if(content!=null) showHome(); });
            }catch(Exception e){ main.post(()->Toast.makeText(this,"تعذر تحميل قائمة القراء. يمكنك المحاولة لاحقًا.",Toast.LENGTH_LONG).show()); }
        });
    }

    String get(String u) throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestProperty("Accept","application/json");
        InputStream in=c.getInputStream(); BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
        StringBuilder s=new StringBuilder(); String line; while((line=br.readLine())!=null)s.append(line);
        br.close(); c.disconnect(); return s.toString();
    }

    void showError(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    class Reciter { String id,name,server; Reciter(String i,String n,String s){id=i;name=n;server=s;} }

    class SurahAdapter extends RecyclerView.Adapter<SurahAdapter.H>{
        public H onCreateViewHolder(ViewGroup p,int v){Button b=navButton("",null); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); return new H(b);}
        public void onBindViewHolder(H h,int pos){int n=pos+1; h.b.setText(n+"  —  "+surahNames[pos]); h.b.setOnClickListener(v->openSurah(n));}
        public int getItemCount(){return surahNames.length;}
        class H extends RecyclerView.ViewHolder{Button b; H(View v){super(v);b=(Button)v;}}
    }

    class ReciterAdapter extends RecyclerView.Adapter<ReciterAdapter.H>{
        ArrayList<Reciter> source, data;
        ReciterAdapter(ArrayList<Reciter> a){source=a;data=new ArrayList<>(a);}
        void filter(String x){data.clear(); for(Reciter r:source) if(r.name.contains(x)) data.add(r); notifyDataSetChanged();}
        public H onCreateViewHolder(ViewGroup p,int v){Button b=navButton("",null); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); return new H(b);}
        public void onBindViewHolder(H h,int pos){Reciter r=data.get(pos); h.b.setText("🎙  "+r.name); h.b.setOnClickListener(v->{selectedReciter=r; Toast.makeText(MainActivity.this,"تم اختيار "+r.name,Toast.LENGTH_SHORT).show(); showHome();});}
        public int getItemCount(){return data.size();}
        class H extends RecyclerView.ViewHolder{Button b; H(View v){super(v);b=(Button)v;}}
    }

    @Override protected void onDestroy(){super.onDestroy(); if(player!=null){player.release();player=null;} executor.shutdownNow();}
}
