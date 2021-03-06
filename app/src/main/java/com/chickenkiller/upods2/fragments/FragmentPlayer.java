package com.chickenkiller.upods2.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.menu.ActionMenuItemView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.chickenkiller.upods2.R;
import com.chickenkiller.upods2.controllers.app.ProfileManager;
import com.chickenkiller.upods2.controllers.player.PlayerPositionUpdater;
import com.chickenkiller.upods2.controllers.player.Playlist;
import com.chickenkiller.upods2.controllers.player.UniversalPlayer;
import com.chickenkiller.upods2.interfaces.IOnPositionUpdatedCallback;
import com.chickenkiller.upods2.interfaces.IOperationFinishCallback;
import com.chickenkiller.upods2.interfaces.IPlayerStateListener;
import com.chickenkiller.upods2.interfaces.IToolbarHolder;
import com.chickenkiller.upods2.models.MediaItem;
import com.chickenkiller.upods2.models.Podcast;
import com.chickenkiller.upods2.models.RadioItem;
import com.chickenkiller.upods2.models.Track;
import com.chickenkiller.upods2.utils.Logger;
import com.chickenkiller.upods2.utils.MediaUtils;
import com.chickenkiller.upods2.utils.ui.LetterBitmap;
import com.chickenkiller.upods2.utils.ui.UIHelper;
import com.chickenkiller.upods2.views.PlayPauseView;
import com.pixplicity.easyprefs.library.Prefs;


/**
 * Created by Alon Zilberman on 7/27/15.
 */
public class FragmentPlayer extends Fragment implements IPlayerStateListener {

    public static String TAG = "fragmentPlayer";
    public static final long DEFAULT_RADIO_DURATIO = 1000000;

    private static final float TOOLBAR_TEXT_SIZE = 20f;
    private static final int COVER_IMAGE_SIZE = UIHelper.dpToPixels(100);
    private static final int CODE_PERMISSIONS_PHONE_STATE = 721;
    private static final String PREF_PHONE_PERM_ASKED = "phone_perm_asked";

    private MediaItem playableMediaItem;
    private UniversalPlayer universalPlayer;
    private Playlist playlist;
    private PlayerPositionUpdater playerPositionUpdater;

    private View rootView;
    private PlayPauseView btnPlay;
    private ImageButton btnRewindLeft;
    private ImageButton btnRewindRight;
    private ImageView imgPlayerCover;
    private RelativeLayout rlTopSectionBckg;
    private TextView tvPlayserSubtitle;
    private TextView tvPlayerTitle;
    private TextView tvTrackInfo;
    private TextView tvTrackCurrentTime;
    private TextView tvTrackDuration;
    private TextView tvTrackNumbers;
    private SeekBar sbPlayerProgress;
    private LinearLayout lnPlayerinfo;
    private ActionMenuItemView itemFavorites;

    private long maxDuration = -1;
    private boolean isChangingProgress = false;
    private boolean isFirstRun = true;

    private View.OnClickListener btnPlayStopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (universalPlayer.isPrepaired) {
                universalPlayer.toggle();
                playlist.updateTracks();
            }
        }
    };

    private View.OnClickListener btnForwardClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (universalPlayer.isPrepaired) {
                playlist.goForward();
            }
        }
    };

    private View.OnClickListener btnBackwardClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (universalPlayer.isPrepaired) {
                playlist.goBackward();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        btnPlay = (PlayPauseView) view.findViewById(R.id.btnPlay);
        btnRewindLeft = (ImageButton) view.findViewById(R.id.btnRewindLeft);
        btnRewindRight = (ImageButton) view.findViewById(R.id.btnRewindRight);
        btnPlay.setOnClickListener(btnPlayStopClickListener);
        btnRewindLeft.setOnClickListener(btnBackwardClickListener);
        btnRewindRight.setOnClickListener(btnForwardClickListener);
        rlTopSectionBckg = (RelativeLayout) view.findViewById(R.id.rlTopSectionBckg);
        imgPlayerCover = (ImageView) view.findViewById(R.id.imgPlayerCover);
        tvPlayerTitle = (TextView) view.findViewById(R.id.tvPlayerTitle);
        tvPlayserSubtitle = (TextView) view.findViewById(R.id.tvPlayserSubtitle);
        tvTrackInfo = (TextView) view.findViewById(R.id.tvTrackInfo);
        tvTrackDuration = (TextView) view.findViewById(R.id.tvTrackDuration);
        tvTrackCurrentTime = (TextView) view.findViewById(R.id.tvTrackCurrentTime);
        tvTrackNumbers = (TextView) view.findViewById(R.id.tvTrackNumbers);
        lnPlayerinfo = (LinearLayout) view.findViewById(R.id.lnPlayerInfo);
        sbPlayerProgress = (SeekBar) view.findViewById(R.id.sbPlayerProgress);
        itemFavorites = (ActionMenuItemView) ((IToolbarHolder) getActivity()).getToolbar().findViewById(R.id.action_favorites_player);
        universalPlayer = UniversalPlayer.getInstance();

        ((IToolbarHolder) getActivity()).getToolbar().setTitle(R.string.buffering);
        TextView toolbarTitle = UIHelper.getToolbarTextView(((IToolbarHolder) getActivity()).getToolbar());
        toolbarTitle.setTextSize(TOOLBAR_TEXT_SIZE);
        toolbarTitle.setTypeface(null, Typeface.NORMAL);

        if (playableMediaItem == null) {
            playableMediaItem = UniversalPlayer.getInstance().getPlayingMediaItem();
        }

        askPhoneStatePermissions();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        final ViewTreeObserver observer = rootView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sbPlayerProgress.getLayoutParams();
                RelativeLayout rlPlayerUnderbar = (RelativeLayout) rootView.findViewById(R.id.rlPlayerUnderbar);
                params.bottomMargin = rlPlayerUnderbar.getHeight() - sbPlayerProgress.getHeight() / 2;
                sbPlayerProgress.requestLayout();
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onResume() {
        //If playableMediaItem was changed when fragment was in backround, replace it.
        if (!isFirstRun && universalPlayer.isPrepaired && universalPlayer.getPlayingMediaItem() != null &&
                !universalPlayer.isCurrentMediaItem(playableMediaItem)) {
            playableMediaItem = universalPlayer.getPlayingMediaItem();
        }
        isFirstRun = false;
        initPlayerUI();
        configurePlayer();
        setPlayerCallbacks();
        runPositionUpdater();
        super.onResume();
    }

    @Override
    public void onPause() {
        if (playerPositionUpdater != null) {
            playerPositionUpdater.cancel(false);
        }
        universalPlayer.removeListeners();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setPlayableItem(MediaItem iPlayableMediaItem) {
        this.playableMediaItem = iPlayableMediaItem;
    }


    /**
     * Inits player UI accorfing to current MediaItem
     */
    private void initPlayerUI() {
        if (playableMediaItem instanceof Podcast) {
            tvPlayerTitle.setText(((Podcast) playableMediaItem).getSelectedTrack().getTitle().trim());
        } else {
            tvPlayerTitle.setText(playableMediaItem.getName().trim());
        }
        tvPlayserSubtitle.setText(playableMediaItem.getSubHeader().trim());
        if (playableMediaItem.getCoverImageUrl() == null) {
            final LetterBitmap letterBitmap = new LetterBitmap(getActivity());
            Bitmap letterTile = letterBitmap.getLetterTile(playableMediaItem.getName(), playableMediaItem.getName(), COVER_IMAGE_SIZE, COVER_IMAGE_SIZE);
            imgPlayerCover.setImageBitmap(letterTile);
            int dominantColor = UIHelper.getDominantColor(letterTile);
            rlTopSectionBckg.setBackgroundColor(dominantColor);
        } else {
            Glide.with(getActivity()).load(playableMediaItem.getCoverImageUrl()).crossFade().into(new GlideDrawableImageViewTarget(imgPlayerCover) {
                @Override
                public void onResourceReady(GlideDrawable drawable, GlideAnimation anim) {
                    super.onResourceReady(drawable, anim);
                    Bitmap bitmap = ((GlideBitmapDrawable) drawable).getBitmap();
                    int dominantColor = UIHelper.getDominantColor(bitmap);
                    rlTopSectionBckg.setBackgroundColor(dominantColor);
                }
            });
        }
        if (playableMediaItem instanceof Podcast) {
            Track selectedTrack = ((Podcast) playableMediaItem).getSelectedTrack();
            tvTrackDuration.setText(selectedTrack.getDuration());
            sbPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isChangingProgress = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isChangingProgress = false;
                    int progress = seekBar.getProgress();
                    int position = (int) ((maxDuration * progress) / 100);
                    seekBar.setProgress(progress);
                    universalPlayer.seekTo(position);
                }
            });
        }
        if (playableMediaItem instanceof RadioItem) {
            sbPlayerProgress.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }

        //playableMediaItem.syncWithDB();
        if (playableMediaItem.isSubscribed) {
            itemFavorites.setIcon(getResources().getDrawable(R.drawable.ic_heart_black_24dp));
        } else {
            itemFavorites.setIcon(getResources().getDrawable(R.drawable.ic_heart_white_24dp));
        }
    }


    /**
     * Inits player UI accorfing to current player state, call it after configurePlayer().
     */
    public void initPlayerStateUI() {
        if (!playableMediaItem.getBitrate().isEmpty()) {
            tvTrackInfo.setText(playableMediaItem.getBitrate() + getString(R.string.kbps));
        } else {
            tvTrackInfo.setText("");
        }

        if (universalPlayer.isPrepaired) {
            ((IToolbarHolder) getActivity()).getToolbar().setTitle(R.string.now_paying);
            if (btnPlay.isPlay() && universalPlayer.isPlaying()) {
                btnPlay.toggle();
            } else if (!btnPlay.isPlay() && !universalPlayer.isPlaying()) {
                btnPlay.toggle();
            }

        } else {
            ((IToolbarHolder) getActivity()).getToolbar().setTitle(R.string.buffering);
            if (!btnPlay.isPlay()) {
                btnPlay.toggle();
            }
        }
    }

    private void askPhoneStatePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Prefs.getBoolean(PREF_PHONE_PERM_ASKED, false)) {
            Prefs.putBoolean(PREF_PHONE_PERM_ASKED, true);
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.phone_state_permissions)
                    .content(R.string.phone_state_description)
                    .positiveText(R.string.ok)
                    .onAny(new MaterialDialog.SingleButtonCallback() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            getActivity().requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, CODE_PERMISSIONS_PHONE_STATE);
                        }
                    }).build().show();
        }
    }

    private void initTrackNumbersSection() {
        StringBuilder trackNumberString = new StringBuilder();
        trackNumberString.append(playlist.getCurrentTrackNumber() + 1);
        trackNumberString.append("/");
        trackNumberString.append(playlist.getTracksCount() - 1);
        tvTrackNumbers.setText(trackNumberString.toString());
    }

    private void configurePlayer() {
        if (universalPlayer.isPrepaired && universalPlayer.isCurrentMediaItem(playableMediaItem)) { //Player already running
            Logger.printInfo(TAG, "Configured from playing MediaItem");
        } else {
            Logger.printInfo(TAG, "Starting new MediaItem");
            universalPlayer.resetPlayer();
            universalPlayer.setMediaItem(playableMediaItem);
            universalPlayer.prepare();

        }

        ProfileManager.getInstance().addRecentMediaItem(playableMediaItem);
        if (playlist == null) {
            createPlaylist();
        }
        initPlayerStateUI();
        playlist.updateTracks();
    }

    private void createPlaylist() {
        playlist = new Playlist(getActivity(), rootView, new IOperationFinishCallback() {
            @Override
            public void operationFinished() {
                playableMediaItem = UniversalPlayer.getInstance().getPlayingMediaItem();
                initPlayerUI();
                configurePlayer();
                initTrackNumbersSection();
            }
        });
        lnPlayerinfo.setOnClickListener(playlist.getPlaylistOpenClickListener());
        initTrackNumbersSection();
    }

    private void setPlayerCallbacks() {
        //Sets all callback to player
        universalPlayer.setPlayerStateListener(this);
        universalPlayer.setOnAutonomicTrackChangeCallback(new IOperationFinishCallback() {
            @Override
            public void operationFinished() {
                if (isAdded()) {
                    playableMediaItem = UniversalPlayer.getInstance().getPlayingMediaItem();
                    initPlayerUI();
                    configurePlayer();
                    initTrackNumbersSection();
                }
            }
        });

        IOperationFinishCallback playerFailedCallback = MediaUtils.getPlayerFailCallback(getActivity(), playableMediaItem);

        if (universalPlayer.isInErrorState) {
            playerFailedCallback.operationFinished();
        } else {
            universalPlayer.setOnPlayingFailedCallback(playerFailedCallback);
        }
    }

    private void runPositionUpdater() {
        playerPositionUpdater = (PlayerPositionUpdater) new PlayerPositionUpdater(new IOnPositionUpdatedCallback() {
            @Override
            public void poistionUpdated(int currentPoistion) {
                if (isAdded()) {
                    tvTrackCurrentTime.setText(MediaUtils.formatMsToTimeString(currentPoistion));
                    if (!isChangingProgress) {
                        if (maxDuration < 0) {
                            maxDuration = playableMediaItem instanceof RadioItem ? DEFAULT_RADIO_DURATIO
                                    : MediaUtils.timeStringToLong(tvTrackDuration.getText().toString());
                        }
                        int progress = (int) (currentPoistion * 100 / maxDuration);
                        sbPlayerProgress.setProgress(progress);
                    }
                }
            }

            @Override
            public void poistionUpdaterStoped() {

            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onStateChanged(UniversalPlayer.State state) {
        if (isAdded()) {
            if (btnPlay.isPlay() && state == UniversalPlayer.State.PLAYING) {
                btnPlay.toggle();
                if (((IToolbarHolder) getActivity()).getToolbar() != null) {
                    ((IToolbarHolder) getActivity()).getToolbar().setTitle(R.string.now_paying);
                }
            } else if (!btnPlay.isPlay() && state == UniversalPlayer.State.PAUSED) {
                btnPlay.toggle();
            }
            playlist.updateTracks();
        }
    }
}
