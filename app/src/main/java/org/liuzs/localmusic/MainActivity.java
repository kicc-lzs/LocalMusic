package org.liuzs.localmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.bogerchan.niervisualizer.NierVisualizerManager;
import me.bogerchan.niervisualizer.renderer.IRenderer;
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer;
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer;
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType4Renderer;
import me.bogerchan.niervisualizer.util.NierAnimator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "myMusic";

    static int AUTO_SESSION_ID = 0;//mediaPlayer的autoseeeionid

    ImageView nextIv,playIv,prevIv;
    TextView singerTv,songTv;
    RecyclerView musicRv;

    List<LocalMusicBean> mDatas;//数据源

    private LocalMusicAdapter adapter;//适配器

    MediaPlayer mediaPlayer;//媒体

    int currnetPlayPosition = -1; //初始化播放状态为-1

    int currnetPausePosition;//初始化暂停状态

    LinearLayoutManager layoutManager;//设置布局管理器

    SurfaceView nvsurfaceview;

    NierVisualizerManager visualizerManager;//音乐可视化组件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transparentStatusBar();//透明状态栏方法
        setContentView(R.layout.activity_main);
        initView();
        mediaPlayer = new MediaPlayer();
        mDatas = new ArrayList<>();
        //创建适配器
        adapter = new LocalMusicAdapter(this,mDatas);
        musicRv.setAdapter(adapter);
        //设置布局管理器
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        musicRv.setLayoutManager(layoutManager);
        //加载本地数据原
        loadLoalMusicData();
        //设置每一项的点击事件
        setEventListener();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.local_music_bottom_iv_prev:
                if (currnetPlayPosition == 0) {
                    //已经是第一首
                    Toast.makeText(this,"已经是第一首音乐啊TNT",Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    currnetPlayPosition = currnetPlayPosition - 1;
                    LocalMusicBean lastBean = mDatas.get(currnetPlayPosition);
                    playMusicInBean(lastBean);
                    break;
                }
            case R.id.local_music_bottom_iv_play:
                if (currnetPlayPosition == -1) {
                    //并没有选中音乐，从第一首歌开始播放
                    Toast.makeText(this,"没有选中音乐啊TNT，帮你选择第一首歌",Toast.LENGTH_SHORT).show();
                    currnetPlayPosition = currnetPlayPosition + 1;
                    LocalMusicBean nextBean = mDatas.get(currnetPlayPosition);
                    playMusicInBean(nextBean);
                    return;
                }else if (mediaPlayer.isPlaying()){
                    //暂停音乐
                    pauseMusic();
                    //暂停渲染
                    visualizerManager.pause();
                }else{
                    //从暂停位置开始播放音乐
                    playMusic();
                    //恢复渲染
                    visualizerManager.resume();
                }
                break;
            case R.id.local_music_bottom_iv_next:
                if (currnetPlayPosition == mDatas.size()-1) {
                    //已经是第一首
                    Toast.makeText(this,"已经是最后一首音乐啊TNT",Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    currnetPlayPosition = currnetPlayPosition + 1;
                    LocalMusicBean nextBean = mDatas.get(currnetPlayPosition);
                    playMusicInBean(nextBean);
                    break;
                }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
        visualizerManager.release();
    }

    /**
     * 透明状态栏适合安卓4.4-5.0
     */
    public void transparentStatusBar() {
        //5.0 全透明实现
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //getWindow.setStatusBarColor(Color.TRANSPARENT)
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //4.4 全透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /***
     * 点击事件通过实现接口实现类
     */
    private void setEventListener() {
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                currnetPlayPosition = position;
                LocalMusicBean musicBean = mDatas.get(position);
                playMusicInBean(musicBean);
            }
        });
    }

    /***
     * 提取冗余的代码：更新可视化视图
     * @param musicBean
     */
    public void playMusicInBean(LocalMusicBean musicBean) {
        //获取歌曲autoSessionID并传递给
        int autoSessionID = mediaPlayer.getAudioSessionId();
        updateMusicVisualization(autoSessionID);
        AUTO_SESSION_ID = autoSessionID;

        //底部同步显示歌曲信息
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();
        //重置播放器
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            playMusic();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /***
     * 播放音乐按钮的方法
     */
    private void playMusic() {
        if (mediaPlayer!=null&&!mediaPlayer.isPlaying()){
            //等于0重新开始播放
            if (currnetPausePosition == 0) {
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }catch (IOException e){
                    e.printStackTrace();
                }
                playIv.setImageResource(R.mipmap.landscape_pause_icon_normal);
            }else {
                //从暂停的位置开始播放
                mediaPlayer.seekTo(currnetPausePosition);
                mediaPlayer.start();
                playIv.setImageResource(R.mipmap.landscape_pause_icon_normal);
            }
        }
    }

    /***
     * 暂停音乐按钮的方法
     */
    private void pauseMusic() {
        if (mediaPlayer!=null&&mediaPlayer.isPlaying()) {
            currnetPausePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.landscape_play_icon_normal);
        }
    }

    /**
     * 停止并结束音乐，用于销毁
     */
    private void stopMusic() {
        if (mediaPlayer!=null){
            currnetPausePosition = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            playIv.setImageResource(R.mipmap.landscape_play_icon_normal);
        }
    }

    /**
     * 加载本地📛mp3数据
     */
    private void loadLoalMusicData() {
        //获取contentResolver
        ContentResolver resolver = getContentResolver();
        //获取本地音乐的uri地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //开始查询地址
        Cursor cursor = resolver.query(uri, null, null, null,null);
        //遍历cursor
        int id = 0;
        while (cursor.moveToNext()){
            String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            id++;
            String sid = String.valueOf(id);
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            Long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time = sdf.format(new Date(duration));
            //将一行的数据封装到对象
            LocalMusicBean bean = new LocalMusicBean(sid, song, singer, album, time, path);
            mDatas.add(bean);
        }
        //数据源变化，提示适配器更新
        adapter.notifyDataSetChanged();
    }

    private void initView(){
        checkPermission();
        //初始化控件
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        prevIv = findViewById(R.id.local_music_bottom_iv_prev);
        singerTv = findViewById(R.id.local_music_bottom_tv_singer);
        songTv = findViewById(R.id.local_music_bottom_tv_song);
        musicRv = findViewById(R.id.local_music_rv);
        prevIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        nextIv.setOnClickListener(this);
        nvsurfaceview = findViewById(R.id.nvsurfaceview);
        //音频可视化控件初始化
        visualizerManager = new NierVisualizerManager();
        //SurfaceView设置透明背景
        nvsurfaceview = findViewById(R.id.nvsurfaceview);
        nvsurfaceview.setZOrderOnTop(true);
        nvsurfaceview.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    /**
     * 更新音乐可视化UI
     *
     * @param autoSessionID mediaPlayer.getAudioSessionId()
     */
    void updateMusicVisualization(int autoSessionID) {
        visualizerManager.release();//当每次选中新的歌曲item时，销毁框架实例，释放资源
        visualizerManager = new NierVisualizerManager();//可视化控件初始化
        visualizerManager.init(autoSessionID);//给visualizer传入autoSessionID
        //开始渲染，ColumnarType4Renderer()为经典柱形图
        visualizerManager.start(nvsurfaceview, new IRenderer[]{ new ColumnarType4Renderer()});
    }

    /**
     * 获取权限
     */
    public void checkPermission() {
        boolean isGranted = true;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //如果没有写sd卡权限
                isGranted = false;
            }
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            Log.i("权限获取", " ： " + isGranted);
            if (!isGranted) {
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission
                                .ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                        },
                        102);
            }
        }
    }
}