package sample.sdk.dabkick.sampleappdkvp.PlayVideos;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dabkick.dkvideoplayer.livesession.videoplayer.DkVideoView;
import com.dabkick.dkvideoplayer.publicsettings.DabKickRegistration;
import com.dabkick.dkvideoplayer.publicsettings.NotifyStageVideoReceived;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import sample.sdk.dabkick.sampleappdkvp.R;
import sample.sdk.dabkick.sampleappdkvp.Utils.Util;
import sample.sdk.dabkick.sampleappdkvp.VideoDetails.VideoItemDetail;
import timber.log.Timber;

//import at.huber.youtubeExtractor.VideoMeta;
//import at.huber.youtubeExtractor.YtFile;

public class PlayerActivity extends AppCompatActivity {

    public static VideoItemDetail detail;
    TextView title;
    TextView desc;
    ListView recommended;
    DkVideoView mVideoPlayer;
    public static boolean isRegistered = false, onConfigurationChanged = false;
    DabKickRegistration dabkickRegistration = DabKickRegistration.newInstance();
    ArrayList<VideoItemDetail> videos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

//        if (!isRegistered)
        dabkickRegistration.register(this);
        isRegistered = true;
        init();
        getLifecycle().addObserver(mVideoPlayer);
        getLifecycle().addObserver(dabkickRegistration);
    }


    void init() {

        title = findViewById(R.id.title);
        desc = findViewById(R.id.desc);
        recommended = findViewById(R.id.recomended);
        desc.setMovementMethod(new ScrollingMovementMethod());
        mVideoPlayer = findViewById(R.id.video_view);
        videos = new ArrayList<>();

        play();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(mVideoPlayer != null)
//            mVideoPlayer.onPauseCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mVideoPlayer != null)
//            mVideoPlayer.onResumeCheck();
        if (!isRegistered)
            dabkickRegistration.register(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoPlayer != null && !onConfigurationChanged)
            mVideoPlayer.release();

        if (onConfigurationChanged)
            onConfigurationChanged = false;

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NotifyStageVideoReceived event) {

        Timber.d("onMessageEvent: " + event.url);

        if (event.url != null && mVideoPlayer != null) {
            if (recommended.getAdapter() == null) {

                for (List<VideoItemDetail> videosList : Util.getInstance().videosForPlaylists.values()) {
                    videos.addAll(videosList);
                }

                RecomendedListAdapter adapter = new RecomendedListAdapter(PlayerActivity.this, videos);
                recommended.setAdapter(adapter);
            }

            mVideoPlayer.release();
            mVideoPlayer.setMediaItem(event.url);
            mVideoPlayer.prepare(false);
            mVideoPlayer.showPopUp = false;

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onConfigurationChanged = true;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            title.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoPlayer.getLayoutParams();
            params.width = params.MATCH_PARENT;
            params.height = params.MATCH_PARENT;
            mVideoPlayer.setLayoutParams(params);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            title.setVisibility(View.VISIBLE);
            desc.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoPlayer.getLayoutParams();
            params.width = params.MATCH_PARENT;
            params.height = (int) Util.getInstance().convertDpToPixel(this, 200);
            mVideoPlayer.setLayoutParams(params);
        }
    }

    public void play() {

        if (detail != null) {
            title.setText(detail.getTitle());
            desc.setText(detail.getDesc());
            final String detailUrl = detail.getUrl();

            videos.clear();

            for (List<VideoItemDetail> videosList : Util.getInstance().videosForPlaylists.values()) {
                videos.addAll(videosList);
            }

            videos.remove(detail);

            if (recommended.getAdapter() == null) {
                RecomendedListAdapter adapter = new RecomendedListAdapter(PlayerActivity.this, videos);
                recommended.setAdapter(adapter);
            } else {

                RecomendedListAdapter adapter = (RecomendedListAdapter) recommended.getAdapter();
                /*adapter.videos.clear();
                adapter.videos = videos;*/
                adapter.notifyDataSetChanged();
            }

            String videoID = detail.getId();

            LoadYoutubeVideos.getInstance().setOnFinishedDownload(new LoadYoutubeVideos.OnFinishedDownloadListener() {
                @Override
                public void onFinishedDownload(String fullStreamURL, boolean success) {
                    if (success) {

                        mVideoPlayer.setMediaItem(fullStreamURL);
                        mVideoPlayer.prepare(true);

                    } else {

                        Toast.makeText(PlayerActivity.this, "Unable to play video", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            LoadYoutubeVideos.getInstance().loadYoutubeURL(PlayerActivity.this, videoID, null);
        }
    }
}
