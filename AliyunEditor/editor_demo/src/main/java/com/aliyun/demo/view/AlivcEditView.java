package com.aliyun.demo.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.common.utils.StorageUtils;
import com.aliyun.common.utils.ToastUtil;
import com.aliyun.crop.AliyunCropCreator;
import com.aliyun.crop.supply.AliyunICrop;
import com.aliyun.demo.editor.AbstractPasterUISimpleImpl;
import com.aliyun.demo.editor.PasterUICaptionImpl;
import com.aliyun.demo.editor.PasterUIGifImpl;
import com.aliyun.demo.editor.PasterUITextImpl;
import com.aliyun.demo.editor.R;
import com.aliyun.demo.effects.audiomix.MusicChooser;
import com.aliyun.demo.effects.control.BaseChooser;
import com.aliyun.demo.effects.control.EditorService;
import com.aliyun.demo.effects.control.EffectInfo;
import com.aliyun.demo.effects.control.OnEffectActionLister;
import com.aliyun.demo.effects.control.OnEffectChangeListener;
import com.aliyun.demo.effects.control.OnTabChangeListener;
import com.aliyun.demo.effects.control.TabGroup;
import com.aliyun.demo.effects.control.UIEditorPage;
import com.aliyun.demo.effects.control.ViewStack;
import com.aliyun.demo.msg.Dispatcher;
import com.aliyun.demo.msg.body.FilterTabClick;
import com.aliyun.demo.msg.body.LongClickAnimationFilter;
import com.aliyun.demo.msg.body.LongClickUpAnimationFilter;
import com.aliyun.demo.msg.body.LrcListModel;
import com.aliyun.demo.msg.body.LrcModel;
import com.aliyun.demo.msg.body.SelectColorFilter;
import com.aliyun.demo.publish.PublishActivity;
import com.aliyun.demo.util.Common;
import com.aliyun.demo.util.FixedToastUtils;
import com.aliyun.demo.util.SimpleService;
import com.aliyun.demo.util.ThreadUtil;
import com.aliyun.demo.viewoperate.ViewOperator;
import com.aliyun.demo.widget.AliyunPasterWithImageView;
import com.aliyun.demo.widget.AliyunPasterWithTextView;
import com.aliyun.editor.EditorCallBack;
import com.aliyun.editor.EffectType;
import com.aliyun.nativerender.BitmapGenerator;
import com.aliyun.querrorcode.AliyunEditorErrorCode;
import com.aliyun.querrorcode.AliyunErrorCode;
import com.aliyun.qupai.editor.AliyunIComposeCallBack;
import com.aliyun.qupai.editor.AliyunIEditor;
import com.aliyun.qupai.editor.AliyunPasterController;
import com.aliyun.qupai.editor.AliyunPasterRender;
import com.aliyun.qupai.editor.OnPasterResumeAndSave;
import com.aliyun.qupai.editor.impl.AliyunEditorFactory;
import com.aliyun.qupai.editor.impl.text.TextBitmap;
import com.aliyun.qupai.editor.impl.text.TextBitmapGenerator;
import com.aliyun.svideo.base.MediaInfo;
import com.aliyun.svideo.base.UIConfigManager;
import com.aliyun.svideo.base.utils.DensityUtil;
import com.aliyun.svideo.sdk.external.struct.PasterDescriptor;
import com.aliyun.svideo.sdk.external.struct.common.AliyunVideoParam;
import com.aliyun.svideo.sdk.external.struct.common.VideoDisplayMode;
import com.aliyun.svideo.sdk.external.struct.effect.EffectBean;
import com.aliyun.svideo.sdk.external.struct.effect.EffectPaster;
import com.aliyun.svideo.sdk.external.struct.effect.EffectText;
import com.aliyun.svideo.sdk.external.thumbnail.AliyunIThumbnailFetcher;
import com.aliyun.svideo.sdk.external.thumbnail.AliyunThumbnailFetcherFactory;
import com.aliyun.video.common.utils.ThreadUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

/**
 * @author zsy_18 data:2018/8/24
 */
public class AlivcEditView extends RelativeLayout
        implements OnEffectChangeListener, OnTabChangeListener {
    private static final String TAG = AlivcEditView.class.getName();
    /**
     * 编辑核心接口类
     */
    private AliyunIEditor mAliyunIEditor;
    /**
     * 动图管理接口类
     */
//    private AliyunPasterManager mPasterManager;
    private AliyunPasterRender mPasterRender;

    /**
     * 裁剪接口核心类，对于Gop比较大的视频做时间特效时需要先检查是否满足实时，如果不满足实时，需要提前做转码，逻辑如下
     */
    private AliyunICrop mTranscoder;

    /**
     * 右侧编辑菜单栏
     */
    private View mEditContainer;
    /**
     * 编辑需要渲染显示的SurfaceView
     */
    private SurfaceView mSurfaceView;
    /**
     * 主要用于记录各个功能view上次的状态，用于下次进入的时候进行恢复
     */
    private EditorService mEditorService;
    /**
     * 控件
     */
    private RelativeLayout mActionBar;
    private FrameLayout resCopy;
    public FrameLayout mPasterContainer;
    private FrameLayout mGlSurfaceContainer;
    private ImageView mIvLeft;
    private ImageView mIvRight;

    private TextView mTitle;

    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * 水印图片
     */
    private Bitmap mWatermarkBitmap;
    /**
     * 状态，使用倒放时间特效
     */
    private boolean mUseInvert = false;
    /**
     * 状态，正在添加滤镜特效那个中
     */
    private boolean mUseAnimationFilter = false;
    /**
     * 状态，判断是否可以继续添加时间特效，true不可以继续添加特效
     */
    private boolean mCanAddAnimation = true;
    /**
     * 状态，是否正在转码中
     */
    private boolean mIsTranscoding = false;
    /**
     * 状态，界面是否被销毁
     */
    private boolean mIsDestroyed = false;
    /**
     * 状态，与生命周期onStop有关
     */
    private boolean mIsStop = false;
    private boolean mWaitForReady = false;

    private AbstractPasterUISimpleImpl mCurrentEditEffect;
    /**
     * 音量
     */
    private int mVolume = 50;
    /**
     * 控制UI变动
     */
    private ViewOperator mViewOperate;
    private Point mPasterContainerPoint;
    private EffectBean lastMusicBean;
    //播放时间、显示时间、缩略图位置同步接口
    private PlayerListener mPlayerListener;
    private EffectInfo mLastMVEffect;
    private ObjectAnimator animatorX;
    private Toast showToast;

    /*
     *  判断是编辑模块进入还是通过社区模块的编辑功能进入
     *  svideo: 短视频
     *  community: 社区
     */
    private String entrance;

    private List<EffectPaster> mPasterEffecCachetList = new ArrayList<>();
    private List<AliyunPasterController> mPasterControllerList = new ArrayList<>();

    /**
     * 线程池
     */
    private ExecutorService executorService;
    private boolean mStartMusicList;
    private TextBitmapGenerator mBitmapGenerator;
    private TextBitmap mTextBitmap;
    private EffectText mEffectText;;

    public AlivcEditView(Context context) {
        this(context, null);
    }

    public AlivcEditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlivcEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        Dispatcher.getInstance().register(this);

        Point point = new Point();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(point);
        mScreenWidth = point.x;
        LayoutInflater.from(getContext()).inflate(R.layout.aliyun_svideo_activity_editor, this, true);
        initView();
        initListView();
        initThreadHandler();
        copyAssets();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        resCopy = (FrameLayout) findViewById(R.id.copy_res_tip);

        mActionBar = (RelativeLayout) findViewById(R.id.action_bar);
        mActionBar.setBackgroundDrawable(null);
        mActionBar.bringToFront();

        mIvLeft = (ImageView) findViewById(R.id.iv_left);
        mIvRight = (ImageView) findViewById(R.id.iv_right);
        mIvLeft.setImageResource(R.mipmap.aliyun_svideo_back);
        //uiConfig中的属性
        UIConfigManager.setImageResourceConfig(mIvRight, R.attr.finishImage, R.mipmap.aliyun_svideo_complete_red);
        mIvLeft.setVisibility(View.VISIBLE);
        mIvRight.setVisibility(View.VISIBLE);
        mIvLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((Activity) getContext()).finish();
            }
        });

        mTitle = findViewById(R.id.tab_effect_audio_mix);

        mPasterContainer = (FrameLayout) findViewById(R.id.pasterView);
        mGlSurfaceContainer = (FrameLayout) findViewById(R.id.glsurface_view);
        mSurfaceView = (SurfaceView) findViewById(R.id.play_view);
        mEditContainer = findViewById(R.id.edit_right_tab);

        final GestureDetector mGesture = new GestureDetector(getContext(), new MyOnGestureListener());
        View.OnTouchListener pasterTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGesture.onTouchEvent(event);
            }
        };

        mPasterContainer.setOnTouchListener(pasterTouchListener);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBottomEditorView();
            }
        });

        View showLrc = findViewById(R.id.tab_effect_font);
        showLrc.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                if (v.isSelected()) {
                    showLrc(v, false);
                } else {
                    showLrc(v, true);
                }
            }
        });

    }

    private AliyunPasterController controller;
    private PasterUITextImpl textui;

    /**
     * 获取歌词并显示
     * @param view
     * @param showLrc
     */
    private void showLrc(final View view, boolean showLrc) {
        if (showLrc) {
                if (mPasterControllerList == null) {
                    mPasterControllerList = new ArrayList<>();
                } else {
                    mPasterControllerList.clear();
                }
                if (mPasterEffecCachetList == null) {
                    mPasterEffecCachetList = new ArrayList<>();
                } else {
                    mPasterEffecCachetList.clear();
                }
                SimpleService.getLrcFromRaw(getContext(), new SimpleService.OnServiceListener<LrcListModel>() {
                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        view.setEnabled(true);
                    }

                    @Override
                    public void onSuccess(LrcListModel lrcListModel) {
                        if (lrcListModel != null) {
                            List<LrcModel> lrcModels = lrcListModel.getLrcModels();
                            if (lrcModels != null) {
                                LrcModel model = null;
                                LrcModel nextModel = null;
                                int i = 0;
                                long endTime;
                                int startTime = 0;
                                int cutDuration = (int) (mAliyunIEditor.getDuration() / 1000);
                                for (i = 0; i < lrcModels.size(); i++) {
                                    model = lrcModels.get(i);
                                    nextModel = i + 1 < lrcModels.size() ? lrcModels.get(i + 1) : null;
                                    //从lrc文件里读取到的时间戳是这一行歌词的起始时间，model中的leftTime是毫秒，下面赋值和计算时需要和微秒作区分
                                    if (model != null
                                            && model.leftTime > startTime) {
                                        endTime = (long) (nextModel != null ? nextModel.leftTime : model.leftTime + 1000);
                                        if (endTime > startTime + cutDuration) {
                                            break;
                                        }
                                        mEffectText = new EffectText("");
                                        mEffectText.textColor = getResources().getColor(android.R.color.white);
                                        mEffectText.mTextSize = 20;
                                        mEffectText.text = model.lrc;
                                        mEffectText.width = 200;
                                        mEffectText.height = 100;
                                        mEffectText.mTextPaddingX = 100;
                                        mEffectText.mTextPaddingY = 100;
                                        mEffectText.x = mEffectText.mTextPaddingX;
                                        mEffectText.y = mEffectText.mTextPaddingY;
                                        mEffectText.start = (long) (model.leftTime - startTime) * 1000;
                                        mEffectText.duration = (long) (endTime - model.leftTime) * 1000;
                                        int code = mPasterRender.addSubtitle(new BitmapGenerator() {
                                            @Override
                                            public Bitmap generateBitmap(int bmpWidth, int bmpHeight) {
                                                if(mBitmapGenerator == null) {
                                                    mTextBitmap = new TextBitmap();
                                                    mBitmapGenerator = new TextBitmapGenerator();
                                                }
                                                mTextBitmap.mText = mEffectText.text;
                                                mTextBitmap.mFontPath = mEffectText.getPath();
                                                mTextBitmap.mBmpWidth = bmpWidth;
                                                mTextBitmap.mBmpHeight = bmpHeight;
                                                mTextBitmap.mTextWidth = bmpWidth;
                                                mTextBitmap.mTextHeight = bmpHeight;
                                                mTextBitmap.mTextPaddingX = mEffectText.mTextPaddingX;
                                                mTextBitmap.mTextPaddingY = mEffectText.mTextPaddingY;
                                                mTextBitmap.mTextSize = mEffectText.mTextSize;
                                                mTextBitmap.mTextColor = mEffectText.textColor;
                                                mTextBitmap.mTextStrokeColor = mEffectText.textStrokeColor;
                                                mTextBitmap.mTextAlignment = Layout.Alignment.ALIGN_CENTER;
                                                mBitmapGenerator.updateTextBitmap(mTextBitmap);
                                                Log.e("AlivcEditView", "addsubtitle:" + mTextBitmap.mText + ", "
                                                        + mTextBitmap.mBmpWidth + ", " + mTextBitmap.mBmpHeight + ", "
                                                        + mTextBitmap.mTextWidth + ", " + mTextBitmap.mTextHeight
                                                        + ", " + mTextBitmap.mTextPaddingX + ", " + mTextBitmap.mTextPaddingY
                                                        + ", " + mTextBitmap.mTextSize);
                                                return mBitmapGenerator.generateBitmap(bmpWidth, bmpHeight);

                                            }
                                        }, mEffectText);
                                        mPasterRender.showPaster(mEffectText);
                                        mPasterEffecCachetList.add(mEffectText);
                                        Log.e("AlivcEditView", "addsubtitle:" + i + ", code:" + code + ", " + model.lrc + ", " + mEffectText.start + ", " + mEffectText.duration);
                                    }
                                }
                            }
                            view.setSelected(true);
                        }
                        view.setEnabled(true);
                    }
                });
        } else {
            if (mPasterEffecCachetList != null) {
                for (EffectPaster paster : mPasterEffecCachetList) {
                    if (paster != null) {
                        mPasterRender.removePaster(paster);
                    }
                }
                mPasterEffecCachetList.clear();
                view.setSelected(false);
            }
            view.setEnabled(true);
        }
    }

    private void initGlSurfaceView() {
        if (mVideoParam == null) {
            return;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mGlSurfaceContainer.getLayoutParams();
        FrameLayout.LayoutParams surfaceLayout = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        int rotation = mAliyunIEditor.getRotation();
        int outputWidth = mVideoParam.getOutputWidth();
        int outputHeight = mVideoParam.getOutputHeight();
        if ((rotation == 90 || rotation == 270)) {
            int temp = outputWidth;
            outputWidth = outputHeight;
            outputHeight = temp;
        }

        float percent;
        if (outputWidth >= outputHeight) {
            percent = (float) outputWidth / outputHeight;
        } else {
            percent = (float) outputHeight / outputWidth;
        }
        /*
          指定surfaceView的宽高比是有必要的，这样可以避免某些非标分辨率下造成显示比例不对的问题
         */
        surfaceLayout.width = mScreenWidth;
        surfaceLayout.height = Math.round((float) outputHeight * mScreenWidth / outputWidth);
        mPasterContainerPoint = new Point(surfaceLayout.width, surfaceLayout.height);
        ViewGroup.MarginLayoutParams marginParams = null;
        if (layoutParams instanceof MarginLayoutParams) {
            marginParams = (ViewGroup.MarginLayoutParams) surfaceLayout;
        } else {
            marginParams = new MarginLayoutParams(surfaceLayout);
        }
        if (percent < 1.5 || (rotation == 90 || rotation == 270)) {
            marginParams.setMargins(0,
                    getContext().getResources().getDimensionPixelSize(R.dimen.alivc_svideo_title_height), 0, 0);
        } else {
            if (outputWidth > outputHeight) {
                marginParams.setMargins(0,
                        getContext().getResources().getDimensionPixelSize(R.dimen.alivc_svideo_title_height) * 2, 0, 0);
            }
        }
        mGlSurfaceContainer.setLayoutParams(layoutParams);
        mPasterContainer.setLayoutParams(marginParams);
        mSurfaceView.setLayoutParams(marginParams);
    }

    private void initListView() {
        mEditorService = new EditorService();
    }

    private void initEditor() {
        mAliyunIEditor = AliyunEditorFactory.creatAliyunEditor(mUri, mEditorCallback);
        initGlSurfaceView();
        {//该代码块中的操作必须在AliyunIEditor.init之前调用，否则会出现动图、动效滤镜的UI恢复回调不执行，开发者将无法恢复动图、动效滤镜UI
//            mPasterManager = mAliyunIEditor.createPasterManager();
            mPasterRender = mAliyunIEditor.getPasterRender();
            FrameLayout.LayoutParams surfaceLayout = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
            /*
              指定显示区域大小后必须调用mPasterManager.setDisplaySize，否则将无法添加和恢复一些需要提前获知区域大小的资源，如字幕，动图等
              如果开发者的布局使用了wrapContent或者matchParent之类的布局，务必获取到view的真实宽高之后在调用
             */
            try {
                mPasterRender.setDisplaySize(surfaceLayout.width, surfaceLayout.height);
//                mPasterManager.setDisplaySize(surfaceLayout.width, surfaceLayout.height);
            } catch (Exception e) {
                showToast = FixedToastUtils.show(getContext(), e.getMessage());
                ((Activity) getContext()).finish();
            }
            mPasterRender.setOnPasterResumeAndSave(mOnPasterRestoreListener);
//            mPasterManager.setOnPasterRestoreListener(mOnPasterRestoreListener);
//            mAliyunIEditor.setAnimationRestoredListener(AlivcEditView.this);
        }

        mTranscoder = AliyunCropCreator.createCropInstance(getContext());
        VideoDisplayMode mode = mVideoParam.getScaleMode();
        int ret = mAliyunIEditor.init(mSurfaceView, getContext().getApplicationContext());
        mAliyunIEditor.setDisplayMode(mode);
        mAliyunIEditor.setVolume(mVolume);
        mAliyunIEditor.setFillBackgroundColor(Color.BLACK);
        if (ret != AliyunErrorCode.OK) {
            showToast = FixedToastUtils.show(getContext(),
                    getResources().getString(R.string.aliyun_svideo_editor_init_failed));
            return;
        }
        mEditorService.addTabEffect(UIEditorPage.MV, mAliyunIEditor.getMVLastApplyId());
        mEditorService.addTabEffect(UIEditorPage.FILTER_EFFECT, mAliyunIEditor.getFilterLastApplyId());
        mEditorService.addTabEffect(UIEditorPage.AUDIO_MIX, mAliyunIEditor.getMusicLastApplyId());
        mEditorService.setPaint(mAliyunIEditor.getPaintLastApply());

        mIvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                //合成方式分为两种，当前页面合成（前台页面）和其他页面合成（后台合成，这里后台并不是真正的app退到后台）
                //前台合成如下：如果要直接合成（当前页面合成），请打开注释，参考注释代码这种方式

                //后台合成如下：如果要像Demo默认的这样，在其他页面合成，请参考下面这种方式
                mAliyunIEditor.saveEffectToLocal();
                final AliyunIThumbnailFetcher fetcher = AliyunThumbnailFetcherFactory.createThumbnailFetcher();
                fetcher.fromConfigJson(mUri.getPath());
                fetcher.setParameters(mAliyunIEditor.getVideoWidth(), mAliyunIEditor.getVideoHeight(),
                        AliyunIThumbnailFetcher.CropMode.Mediate, VideoDisplayMode.FILL, 1);
                fetcher.requestThumbnailImage(new long[]{0}, new AliyunIThumbnailFetcher.OnThumbnailCompletion() {

                    @Override
                    public void onThumbnailReady(Bitmap bitmap, long l) {
                        String path = getContext().getExternalFilesDir(null) + "thumbnail.jpeg";
                        try {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(path));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(getContext(), PublishActivity.class);
                        intent.putExtra(PublishActivity.KEY_PARAM_THUMBNAIL, path);
                        intent.putExtra(PublishActivity.KEY_PARAM_CONFIG, mUri.getPath());
                        intent.putExtra(PublishActivity.KEY_PARAM_ENTRANCE, entrance);
                        intent.putExtra(PublishActivity.KEY_PARAM_VIDEO_RATIO, ((float) mPasterContainerPoint.x) / mPasterContainerPoint.y);//传入视频比列
                        getContext().startActivity(intent);

                        fetcher.release();
                    }

                    @Override
                    public void onError(int errorCode) {
                        fetcher.release();
                    }
                });
            }
        });

        mPlayerListener = new PlayerListener() {

            @Override
            public long getCurrDuration() {
                return mAliyunIEditor.getCurrentStreamPosition();
            }

            @Override
            public long getDuration() {
                long streamDuration = mAliyunIEditor.getStreamDuration();
                Log.d(TAG, "getDuration: " + streamDuration);
                return streamDuration;
            }

            @Override
            public void updateDuration(long duration) {
//                mTvCurrTime.setText(convertDuration2Text(duration));
            }
        };

        //非编辑态隐藏
        File mWatermarkFile = new File(StorageUtils.getCacheDirectory(getContext()) + "/editor/tail/logo.png");
        if (mWatermarkFile.exists()) {
            if (mWatermarkBitmap == null || mWatermarkBitmap.isRecycled()) {
                mWatermarkBitmap = BitmapFactory.decodeFile(
                        StorageUtils.getCacheDirectory(getContext()) + "/editor/tail/logo.png");
            }
            mSurfaceView.post(new Runnable() {
                @Override
                public void run() {
                    int outputWidth = mVideoParam.getOutputWidth();
                    int outputHeight = mVideoParam.getOutputHeight();
                    int mWatermarkBitmapWidth = DensityUtil.dip2px(getContext(), 30);
                    int mWatermarkBitmapHeight = DensityUtil.dip2px(getContext(), 30);
                    if (mWatermarkBitmap == null && !mWatermarkBitmap.isRecycled()) {
                        mWatermarkBitmapWidth = mWatermarkBitmap.getWidth();
                        mWatermarkBitmapHeight = mWatermarkBitmap.getHeight();
                    }
                    float posY = 0;
                    float percent = (float) outputHeight / outputWidth;
                    if (percent > 1.5) {
                        posY = 0f
                                + (float) (mWatermarkBitmapHeight / 2 + getContext().getResources().getDimensionPixelSize(
                                R.dimen.alivc_svideo_title_height)) / 1.5f / mSurfaceView.getHeight();
                    } else {
                        posY = 0f + (float) mWatermarkBitmapHeight / 1.5f / mSurfaceView.getHeight() / 2;
                    }
                    /**
                     * 水印例子 水印的大小为 ：水印图片的宽高和显示区域的宽高比，注意保持图片的比例，不然显示不完全
                     * 水印的位置为 ：以水印图片中心点为基准，显示区域宽高的比例为偏移量，0,0为左上角，1,1为右下角
                     *
                     */
                    mAliyunIEditor.applyWaterMark(
                            StorageUtils.getCacheDirectory(getContext()) + "/editor/tail/logo.png",
                            (float) mWatermarkBitmapWidth * 0.5f * 0.8f / mSurfaceView.getWidth(),
                            (float) mWatermarkBitmapHeight * 0.5f * 0.8f / mSurfaceView.getHeight(),
                            (float) mWatermarkBitmapWidth / 1.5f / mSurfaceView.getWidth() / 2,
                            posY);

                    //旋转水印
                    //ActionRotate actionRotate = new ActionRotate();
                    //actionRotate.setStartTime(0);
                    //actionRotate.setTargetId(id);
                    //actionRotate.setDuration(10 * 1000 * 1000);
                    //actionRotate.setRepeat(true);
                    //actionRotate.setDurationPerCircle(3 * 1000 * 1000);
                    //mAliyunIEditor.addFrameAnimation(actionRotate);

                    /* //图片水印
                    EffectPicture effectPicture = new EffectPicture("/sdcard/1.png");
                    effectPicture.x = 0.5f;
                    effectPicture.y = 0.5f;
                    effectPicture.width = 0.5f;
                    effectPicture.height = 0.5f;
                    effectPicture.start = 0;
                    effectPicture.end = 10 * 1000 * 1000;
                    mAliyunIEditor.addImage(effectPicture);

                    ActionRotate actionRotateImg = new ActionRotate();
                    actionRotateImg.setStartTime(0);
                    actionRotateImg.setTargetId(effectPicture.getViewId());
                    actionRotateImg.setDuration(10 * 1000 * 1000);
                    actionRotateImg.setRepeat(true);
                    actionRotateImg.setDurationPerCircle(3 * 1000 * 1000);
                    mAliyunIEditor.addFrameAnimation(actionRotateImg);*/


//                    if (hasTailAnimation) {
//                        //片尾水印
//                        mAliyunIEditor.addTailWaterMark(
//                            StorageUtils.getCacheDirectory(getContext()) + "/editor/tail/logo.png",
//                            (float)mWatermarkBitmapWidth / mSurfaceView.getWidth(),
//                            (float)mWatermarkBitmapHeight / mSurfaceView.getHeight(), 0.5f, 0.5f, 2000 * 1000);
//                    }

                }
            });
        }

        mAliyunIEditor.play();


    }

    /**
     * 配置新的缩略条
     */

    private final OnPasterResumeAndSave mOnPasterRestoreListener = new OnPasterResumeAndSave() {

        @Override
        public void onPasterResume(List<PasterDescriptor> list) {

        }

        @Override
        public List<PasterDescriptor> onPasterSave(List<EffectPaster> list) {
            List<PasterDescriptor> pasterDescriptors = new ArrayList<>();
//            PasterDescriptor descriptor;
//            for (int i = 0; i < list.size() ; i ++) {
//                EffectPaster paster = list.get(i);
//                descriptor = new PasterDescriptor();
//                descriptor.duration =
//                pasterDescriptors.add(new PasterDescriptor());
//            }
            return pasterDescriptors;
        }

//        @Override
//        public void onPasterRestored(final List<AliyunPasterController> controllers) {
//
//            Log.d(TAG, "onPasterRestored: " + controllers.size());

//            mPasterContainer.post(new Runnable() {
//                @Override
//                public void run() {
//
//                    if (mPasterContainer != null) {
//                        mPasterContainer.removeAllViews();
//                    }
//                    final List<AbstractPasterUISimpleImpl> aps = new ArrayList<>();
//                    for (AliyunPasterController c : controllers) {
//                        if (!c.isPasterExists()) {
//                            continue;
//                        }
//                        if (c.getPasterStartTime() >= mAliyunIEditor.getStreamDuration()) {
//                            //恢复时覆盖超出缩略图,丢弃
//                            continue;
//                        }
//                        c.setOnlyApplyUI(true);
//                        if (c.getPasterType() == EffectPaster.PASTER_TYPE_GIF) {
//                            mCurrentEditEffect = addPaster(c);
//                        } else if (c.getPasterType() == EffectPaster.PASTER_TYPE_TEXT) {
//                            mCurrentEditEffect = addSubtitle(c, true);
//                        } else if (c.getPasterType() == EffectPaster.PASTER_TYPE_CAPTION) {
//                            mCurrentEditEffect = addCaption(c);
//                        }
//
//                        mCurrentEditEffect.showTimeEdit();
//                        mCurrentEditEffect.getPasterView().setVisibility(View.INVISIBLE);
//                        aps.add(mCurrentEditEffect);
//                        mCurrentEditEffect.moveToCenter();
//                        mCurrentEditEffect.hideOverlayView();
//
//                    }
//
//                    for (AbstractPasterUISimpleImpl pui : aps) {
//                        pui.editTimeCompleted();
//                        pui.getController().setOnlyApplyUI(false);
//                    }
//                }
//            });
//        }

    };

    @Override
    public void onEffectChange(final EffectInfo effectInfo) {
        Log.e("editor", "====== onEffectChange :" + effectInfo);
        //返回素材属性

        EffectBean effect = new EffectBean();
        effect.setId(effectInfo.id);
        effect.setPath(effectInfo.getPath());
        UIEditorPage type = effectInfo.type;

        Log.d(TAG, "effect path " + effectInfo.getPath());
        switch (type) {
            case AUDIO_MIX:
                if (!effectInfo.isAudioMixBar) {
                    //重制mv和混音的音效
                    mAliyunIEditor.resetEffect(EffectType.EFFECT_TYPE_MIX);
                    mAliyunIEditor.resetEffect(EffectType.EFFECT_TYPE_MV_AUDIO);
                    if (lastMusicBean != null) {
                        mAliyunIEditor.removeMusic(lastMusicBean);
                    }
                    lastMusicBean = new EffectBean();
                    lastMusicBean.setId(effectInfo.id);
                    lastMusicBean.setPath(effectInfo.getPath());

                    if (lastMusicBean.getPath() != null) {
                        lastMusicBean.setStartTime(effectInfo.startTime * 1000);//单位是us所以要x1000
                        lastMusicBean.setDuration(effectInfo.endTime == 0 ? Integer.MAX_VALUE
                                : (effectInfo.endTime - effectInfo.startTime) * 1000);//单位是us所以要x1000
                        lastMusicBean.setStreamStartTime(effectInfo.streamStartTime * 1000);
                        lastMusicBean.setStreamDuration(
                                (effectInfo.streamEndTime - effectInfo.streamStartTime) * 1000);//单位是us所以要x1000
                        effectInfo.mixId = mAliyunIEditor.applyMusic(lastMusicBean);
                    } else {
                        //恢复mv声音
                        if (mLastMVEffect != null) {
                            applyMVEffect(mLastMVEffect);
                        }
                    }
                    mAliyunIEditor.applyMusicMixWeight(effectInfo.mixId, effectInfo.musicWeight);
                } else {
                    effectInfo.mixId = mAliyunIEditor.getMusicLastApplyId();
                    mAliyunIEditor.applyMusicMixWeight(effectInfo.mixId, effectInfo.musicWeight);
                }
//                mAliyunIEditor.applyMusicMixWeight(effectInfo.mixId, effectInfo.musicWeight);

                // 确定重新开始播放
                playingResume();
                break;
            default:
                break;
        }
    }

    /**
     * 应用MV特效
     *
     * @param effectInfo
     */
    private void applyMVEffect(EffectInfo effectInfo) {
        EffectBean effect = new EffectBean();
        effect.setId(effectInfo.id);
        effect.setPath(effectInfo.getPath());
        UIEditorPage type = effectInfo.type;
        if (mCurrentEditEffect != null && !mCurrentEditEffect.isPasterRemoved()) {
            mCurrentEditEffect.editTimeCompleted();
        }

        String path = null;
        if (effectInfo.list != null) {
            path = Common.getMVPath(effectInfo.list, mVideoParam.getOutputWidth(),
                    mVideoParam.getOutputHeight());
        }
        effect.setPath(path);
        if (path != null && new File(path).exists()) {
            mAliyunIEditor.resetEffect(EffectType.EFFECT_TYPE_MIX);
            Log.d(TAG, "editor resetEffect end");
            mAliyunIEditor.applyMV(effect);
        } else {
            mAliyunIEditor.resetEffect(EffectType.EFFECT_TYPE_MV);
            if (lastMusicBean != null) {
                mAliyunIEditor.applyMusic(lastMusicBean);
            }
        }
        //重新播放，倒播重播流时间轴需要设置到最后
        if (mUseInvert) {
            mAliyunIEditor.seek(mAliyunIEditor.getStreamDuration());
        } else {
            mAliyunIEditor.seek(0);
        }
        mAliyunIEditor.resume();
    }

    /**
     * 初始化线程池和Handler
     */
    private void initThreadHandler() {
        executorService = ThreadUtil.newDynamicSingleThreadedExecutor(new AlivcEditThread());
    }

    public static class AlivcEditThread implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("AlivcEdit Thread");
            return thread;
        }
    }

    @Override
    public void onTabChange() {
//        UIEditorPage page = UIEditorPage.get(mTabGroup.getCheckedIndex());
//        Log.d(TAG, "onTabChange: " + page.index());
    }

    protected void playingPause() {
        if (mAliyunIEditor.isPlaying()) {
            mAliyunIEditor.pause();
        }
    }

    private void playingResume() {
        if (!mAliyunIEditor.isPlaying()) {
            mAliyunIEditor.play();
            mAliyunIEditor.resume();
        }
    }

    private void clickConfirm() {

        // 确认后变化，各个模块自行实现
//        int checkIndex = mTabGroup.getCheckedIndex();
//        UIEditorPage page = UIEditorPage.get(checkIndex);
//        if (mCurrentEditEffect != null && !mCurrentEditEffect.isPasterRemoved()) {
//            mCurrentEditEffect.editTimeCompleted();
//        }
//        if (page == UIEditorPage.AUDIO_CHANGE) {
//            mStartMusicList = true;
//        }
//        mViewOperate.hideBottomView();
//        LogUtil.e("clickConfirm:" + page.index() + (page == UIEditorPage.AUDIO_CHANGE));
    }

    /**
     * 编辑态视图点击返回
     */
    private void clickCancel() {

        // 取消后变化，各个模块自行实现
//        int checkIndex = mTabGroup.getCheckedIndex();
        int checkIndex = 0;
        UIEditorPage page = UIEditorPage.get(checkIndex);
        if (mCurrentEditEffect != null && !mCurrentEditEffect.isEditCompleted()) {
            mCurrentEditEffect.removePaster();
        }
        switch (page) {
            case AUDIO_MIX:
                playingResume();
                break;
            case CAPTION:
                //这里做paster的撤销恢复处理
                if (mCurrentEditEffect != null && !mCurrentEditEffect.isEditCompleted()) {
                    mCurrentEditEffect.removePaster();
                }

                //先remove所有指定类型的paster
                for (int i = 0; i < mPasterContainer.getChildCount(); i++) {
                    View childAt = mPasterContainer.getChildAt(i);
                    Object tag = childAt.getTag();
                    if (tag == null || !(tag instanceof AbstractPasterUISimpleImpl)) {
                        continue;
                    }
                    AbstractPasterUISimpleImpl uiSimple = (AbstractPasterUISimpleImpl) tag;

                    if (isPasterTypeHold(uiSimple.getEditorPage(), page)) {
                        // 1.Controller remove
                        // 2.pasterContainer remove
                        // 3.ThumbLBar remove
                        uiSimple.removePaster();
                        //涉及到集合遍历删除元素的问题（角标前移）
                        i--;
                    }

                }

                //恢复缓存的指定类型paster
//                for (EffectBase effectBase : mPasterEffecCachetList) {
//                    AliyunPasterController pasterController;
//
//                    //获取对应的controller、（判断文件存在，避免用户删除了对应的资源后恢复时crash）
//                    if (effectBase instanceof EffectCaption && new File(effectBase.getPath()).exists()) {
//                        EffectCaption effect = (EffectCaption) effectBase;
//                        pasterController = mPasterManager.addPasterWithStartTime(effect.getPath(), effect.start, effect.end - effect.start);
//                    } else if (effectBase instanceof EffectText) {
//                        EffectText effect = (EffectText) effectBase;
//                        pasterController = mPasterManager.addSubtitleWithStartTime(effect.text, effect.font, effect.start, effect.end - effect.start);
//                    } else if (effectBase instanceof EffectPaster && new File(effectBase.getPath()).exists()) {
//                        EffectPaster effect = (EffectPaster) effectBase;
//                        pasterController = mPasterManager.addPasterWithStartTime(effect.getPath(), effect.start, effect.end - effect.start);
//                    } else {
//                        continue;
//                    }
//                    pasterController.setEffect(effectBase);
//                    //锁定参数（避免被设置effectBase参数被冲掉）
//                    pasterController.setRevert(true);
//                    if (pasterController.getPasterType() == EffectPaster.PASTER_TYPE_GIF) {
//                        mCurrentEditEffect = addPaster(pasterController);
//                    } else if (pasterController.getPasterType() == EffectPaster.PASTER_TYPE_TEXT) {
//                        mCurrentEditEffect = addSubtitle(pasterController, true);
//                    } else if (pasterController.getPasterType() == EffectPaster.PASTER_TYPE_CAPTION) {
//                        mCurrentEditEffect = addCaption(pasterController);
//                    }
//                    mCurrentEditEffect.showTimeEdit();
//                    mCurrentEditEffect.editTimeStart();
//                    mCurrentEditEffect.editTimeCompleted();
//                    pasterController.setRevert(false);
//                }

                break;
            default:
                break;
        }

        mViewOperate.hideBottomView();
    }

    /**
     * 点击空白出弹窗消失
     */
    private void hideBottomEditorView() {

//        int checkIndex = mTabGroup.getCheckedIndex();
//        LogUtil.e("hideBottomEditorView:" + checkIndex);
//        if (checkIndex == -1) {
//            return;
//        }
//        UIEditorPage page = UIEditorPage.get(checkIndex);

//        mViewOperate.hideBottomEditorView(page);

    }

    /**
     * 页面缩小时 对应的paster也要缩小
     *
     * @param scaleSize 缩小比率
     */
    public void setPasterDisplayScale(float scaleSize) {
//        mPasterManager.setDisplaySize((int) (mPasterContainerPoint.x * scaleSize),
//                (int) (mPasterContainerPoint.y * scaleSize));
        mPasterRender.setDisplaySize((int) (mPasterContainerPoint.x * scaleSize),
                (int) (mPasterContainerPoint.y * scaleSize));
    }

    /**
     * 通过前面的界面传递的入口类型的参数, 为了区分短视频和社区两个模块的不同进入该界面的路径
     *
     * @param moduleEntrance svideo: 短视频,  community: 社区
     */
    public void setModuleEntrance(String moduleEntrance) {
        this.entrance = moduleEntrance;
    }

    private class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        float mPosX;
        float mPosY;
        boolean shouldDrag = true;

        boolean shouldDrag() {
            return shouldDrag;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent");
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "onSingleTapConfirmed: " + shouldDrag);

            if (!shouldDrag) {
                boolean outside = true;
                BaseChooser bottomView = null;
                if (mViewOperate != null) {
                    bottomView = mViewOperate.getBottomView();
                }
                if (bottomView != null) {

                    int count = mPasterContainer.getChildCount();
                    for (int i = count - 1; i >= 0; i--) {
                        View pv = mPasterContainer.getChildAt(i);
                        AbstractPasterUISimpleImpl uic = (AbstractPasterUISimpleImpl) pv.getTag();

                        if (uic != null && bottomView.isHostPaster(uic)) {
                            if (uic.isVisibleInTime(mAliyunIEditor.getCurrentStreamPosition())
                                    && uic.contentContains(e.getX(), e.getY())) {
                                outside = false;
                                if (mCurrentEditEffect != null && mCurrentEditEffect != uic && !mCurrentEditEffect
                                        .isEditCompleted()) {
                                    mCurrentEditEffect.editTimeCompleted();
                                }
                                mCurrentEditEffect = uic;
                                if (uic.isEditCompleted()) {
                                    playingPause();
                                    uic.editTimeStart();
                                }
                                break;
                            } else {
                                if (mCurrentEditEffect != uic && uic.isVisibleInTime(
                                        mAliyunIEditor.getCurrentStreamPosition())) {
                                    uic.editTimeCompleted();
                                    playingResume();
                                }
                            }
                        }
                    }
                }
                if (outside) {
                    if (mCurrentEditEffect != null && !mCurrentEditEffect.isEditCompleted()) {
                        mCurrentEditEffect.editTimeCompleted();
                        //要保证涂鸦永远在动图的上方，则需要每次添加动图时都把已经渲染的涂鸦remove掉，添加完动图后，再重新把涂鸦加上去
                        //mCanvasController = mAliyunIEditor.obtainCanvasController(getContext()
                        // .getApplicationContext(),
                        //    mGlSurfaceContainer.getWidth(), mGlSurfaceContainer.getHeight());
                    }
                    hideBottomEditorView();
                }
            } else {
                playingPause();
                mCurrentEditEffect.showTextEdit(mUseInvert);
            }
            //            if (mAliyunPasterController != null) {
            //                //旋转动图，文字，字幕
            //                ActionRotate actionRotate = new ActionRotate();
            //                actionRotate.setStartTime(0);
            //                actionRotate.setTargetId(mAliyunPasterController.getEffect().getViewId());
            //                actionRotate.setDuration(10 * 1000 * 1000);
            //                actionRotate.setRepeat(true);
            //                actionRotate.setDurationPerCircle(3 * 1000 * 1000);
            //                mAliyunIEditor.addFrameAnimation(actionRotate);
            //                if(mAliyunPasterController.getEffect() instanceof EffectCaption){
            //                    actionRotate = new ActionRotate();
            //                    actionRotate.setStartTime(0);
            //                    actionRotate.setDuration(10 * 1000 * 1000);
            //                    actionRotate.setRepeat(true);
            //                    actionRotate.setDurationPerCircle(3 * 1000 * 1000);
            //                    actionRotate.setTargetId(((EffectCaption) mAliyunPasterController.getEffect())
            // .gifViewId);
            //                    mAliyunIEditor.addFrameAnimation(actionRotate);
            //                }
            //            }
            return shouldDrag;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress");
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (shouldDrag()) {
                if (mPosX == 0 || mPosY == 0) {
                    mPosX = e1.getX();
                    mPosY = e1.getY();
                }
                float x = e2.getX();
                float y = e2.getY();

                mCurrentEditEffect.moveContent(x - mPosX, y - mPosY);

                mPosX = x;
                mPosY = y;

            } else {

            }

            return shouldDrag;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress");
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return shouldDrag;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");
            if (mCurrentEditEffect != null && mCurrentEditEffect.isPasterRemoved()) {
                mCurrentEditEffect = null;
            }

            if (mCurrentEditEffect != null) {
                Log.d(TAG, "mCurrentEditEffect != null");
                shouldDrag = !mCurrentEditEffect.isEditCompleted()
                        && mCurrentEditEffect.contentContains(e.getX(), e.getY())
                        && mCurrentEditEffect.isVisibleInTime(mAliyunIEditor.getCurrentStreamPosition()

                );
            } else {
                shouldDrag = false;

            }

            mPosX = 0;
            mPosY = 0;
            return true;

        }
    }

    StringBuilder mDurationText = new StringBuilder(5);

    private String convertDuration2Text(long duration) {
        mDurationText.delete(0, mDurationText.length());
        float relSec = (float) duration / (1000 * 1000);// us -> s
        int min = (int) ((relSec % 3600) / 60);
        int sec = 0;
        sec = (int) (relSec % 60);
        if (min >= 10) {
            mDurationText.append(min);
        } else {
            mDurationText.append("0").append(min);
        }
        mDurationText.append(":");
        if (sec >= 10) {
            mDurationText.append(sec);
        } else {
            mDurationText.append("0").append(sec);
        }
        return mDurationText.toString();
    }

    private void copyAssets() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Common.copyAll(getContext(), resCopy);
            }
        });
    }

    public AliyunIEditor getEditor() {
        return this.mAliyunIEditor;
    }

    private OnEffectActionLister mOnEffectActionLister = new OnEffectActionLister() {
        @Override
        public void onCancel() {
            clickCancel();
        }

        @Override
        public void onComplete() {
            clickConfirm();
        }
    };

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventColorFilterSelected(SelectColorFilter selectColorFilter) {
        EffectInfo effectInfo = selectColorFilter.getEffectInfo();
        EffectBean effect = new EffectBean();
        effect.setId(effectInfo.id);
        effect.setPath(effectInfo.getPath());
        mAliyunIEditor.applyFilter(effect);
    }

    /**
     * 长按时需要恢复播放
     *
     * @param filter
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventAnimationFilterLongClick(LongClickAnimationFilter filter) {
        if (!mUseAnimationFilter) {
            mUseAnimationFilter = true;
        }
        if (mCanAddAnimation) {
            playingResume();
        } else {
            playingPause();
        }

    }

    /**
     * 长按抬起手指需要暂停播放
     *
     * @param filter
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventAnimationFilterClickUp(LongClickUpAnimationFilter filter) {
        if (mUseAnimationFilter) {
            mUseAnimationFilter = false;
        }
        if (mAliyunIEditor.isPlaying()) {
            playingPause();

        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventFilterTabClick(FilterTabClick ft) {
        //切换到特效的tab需要暂停播放，切换到滤镜的tab需要恢复播放
        if (mAliyunIEditor != null) {
            switch (ft.getPosition()) {
                case FilterTabClick.POSITION_ANIMATION_FILTER:
                    if (mAliyunIEditor.isPlaying()) {
                        playingPause();
                    }
                    break;
                case FilterTabClick.POSITION_COLOR_FILTER:
                    if (!mAliyunIEditor.isPlaying()) {
                        playingResume();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 添加特效后，点击播放，然后再点击撤销，需要切换播放按钮状态
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventAnimationFilterDelete(Integer msg) {
//        switchPlayStateUI(mAliyunIEditor.isPaused());
    }

    private EditorCallBack mEditorCallback = new EditorCallBack() {
        @Override
        public void onEnd(int state) {

            post(new Runnable() {
                @Override
                public void run() {

                    if (!mUseAnimationFilter) {
                        //当正在添加滤镜的时候，不允许重新播放
                        mAliyunIEditor.replay();
                    } else {
                        mCanAddAnimation = false;
                    }

                }
            });
        }

        @Override
        public void onError(final int errorCode) {
            Log.e(TAG, "play error " + errorCode);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    switch (errorCode) {
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_WRONG_STATE:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_PROCESS_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_NO_FREE_DISK_SPACE:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_CREATE_DECODE_GOP_TASK_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_AUDIO_STREAM_DECODER_INIT_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_VIDEO_STREAM_DECODER_INIT_FAILED:

                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_QUEUE_FULL_WARNING:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_SPS_PPS_NULL:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_CREATE_H264_PARAM_SET_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_CREATE_HEVC_PARAM_SET_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_QUEUE_EMPTY_WARNING:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_CREATE_DECODER_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_ERROR_STATE:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_ERROR_INPUT:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_ERROR_NO_BUFFER_AVAILABLE:

                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_ERROR_DECODE_SPS:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_QUEUE_EMPTY_WARNING:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_QUEUE_FULL_WARNING:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_CREATE_DECODER_FAILED:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_ERROR_STATE:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_ERROR_INPUT:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_AUDIO_DECODER_ERROR_NO_BUFFER_AVAILABLE:
                            showToast = FixedToastUtils.show(getContext(), "错误码是" + errorCode);
                            ((Activity) getContext()).finish();
                            break;
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_CACHE_DATA_SIZE_OVERFLOW:
                            showToast = FixedToastUtils.show(getContext(), "错误码是" + errorCode);
                            mAliyunIEditor.play();
                            break;
                        case AliyunErrorCode.ERROR_MEDIA_NOT_SUPPORTED_AUDIO:
                            showToast = FixedToastUtils.show(getContext(),
                                    getResources().getString(R.string.not_supported_audio));
                            ((Activity) getContext()).finish();
                            break;
                        case AliyunErrorCode.ERROR_MEDIA_NOT_SUPPORTED_VIDEO:
                            showToast = FixedToastUtils.show(getContext(),
                                    getResources().getString(R.string.not_supported_video));
                            ((Activity) getContext()).finish();
                            break;
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_MEDIA_POOL_STREAM_NOT_EXISTS:
                        case AliyunEditorErrorCode.ALIVC_FRAMEWORK_VIDEO_DECODER_ERROR_INTERRUPT:
                        case AliyunErrorCode.ERROR_MEDIA_NOT_SUPPORTED_PIXEL_FORMAT:
                            showToast = FixedToastUtils.show(getContext(),
                                    getResources().getString(R.string.not_supported_pixel_format));
                            ((Activity) getContext()).finish();
                            break;
                        default:
                            showToast = FixedToastUtils.show(getContext(),
                                    getResources().getString(R.string.play_video_error));
                            ((Activity) getContext()).finish();
                            break;
                    }
                }
            });

        }

        @Override
        public int onCustomRender(int srcTextureID, int width, int height) {
            return srcTextureID;
        }

        @Override
        public int onTextureRender(int srcTextureID, int width, int height) {
            return 0;
        }

        @Override
        public void onPlayProgress(final long currentPlayTime, final long currentStreamPlayTime) {
            post(new Runnable() {
                @Override
                public void run() {
                    long currentPlayTime = mAliyunIEditor.getCurrentPlayPosition();
                    if (mUseAnimationFilter && mAliyunIEditor.getDuration() - currentPlayTime < USE_ANIMATION_REMAIN_TIME) {
                        mCanAddAnimation = false;
                    } else {
                        mCanAddAnimation = true;
                    }
                }
            });

        }

        private int c = 0;

        @Override
        public void onDataReady() {
            post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onDataReady received");
                    if (mWaitForReady && c > 0) {
                        Log.d(TAG, "onDataReady resume");
                        mWaitForReady = false;
                        mAliyunIEditor.resume();
                    }
                    c++;
                }
            });

        }
    };
    public static final int USE_ANIMATION_REMAIN_TIME = 300 * 1000;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_VOLUME_DOWN:
                mVolume -= 5;
                if (mVolume < 0) {
                    mVolume = 0;
                }
                Log.d("xxffdd", "volume down, current volume = " + mVolume);
                mAliyunIEditor.setVolume(mVolume);
                return true;
            case KEYCODE_VOLUME_UP:
                mVolume += 5;
                if (mVolume > 100) {
                    mVolume = 100;
                }
                Log.d("xxffdd", "volume up, current volume = " + mVolume);
                mAliyunIEditor.setVolume(mVolume);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private boolean isNeedResume = true;

    public void onStart() {
        mIsStop = false;
        mIvRight.setEnabled(true);
//        if (mViewStack != null) {
//            mViewStack.setVisibleStatus(true);
//        }
    }

    public void onResume() {
        if (isNeedResume) {
            playingResume();
        }
        //当删除使用的MV的时候，会发生崩溃，所以在次判断一下mv是否被删除
        if (mLastMVEffect != null) {
            String path = Common.getMVPath(mLastMVEffect.list, mVideoParam.getOutputWidth(),
                    mVideoParam.getOutputHeight());

            if (!TextUtils.isEmpty(path) && !new File(path).exists()) {
                applyMVEffect(new EffectInfo());
            }
        }
    }

    public void onPause() {
        isNeedResume = mAliyunIEditor.isPlaying();
        playingPause();
        mAliyunIEditor.saveEffectToLocal();
    }

    public void onStop() {
        mIsStop = true;
//        if (mViewStack != null) {
//            mViewStack.setVisibleStatus(false);
//        }
        if (showToast != null) {
            showToast.cancel();
            showToast = null;
        }
    }

    public void onDestroy() {
        mIsDestroyed = true;

        Dispatcher.getInstance().unRegister(this);

        if (mAliyunIEditor != null) {
            mAliyunIEditor.onDestroy();
        }
        if (mTranscoder != null) {
            if (mIsTranscoding) {
                mTranscoder.cancel();
            } else {
                mTranscoder.dispose();
            }
        }

        if (mViewOperate != null) {
            mViewOperate.setAnimatorListener(null);
            mViewOperate = null;
        }

        if (animatorX != null) {
            animatorX.cancel();
            animatorX.addUpdateListener(null);
            animatorX.addListener(null);
            animatorX = null;
        }

        if (mWatermarkBitmap != null && !mWatermarkBitmap.isRecycled()) {
            mWatermarkBitmap.recycle();
            mWatermarkBitmap = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
        //退出编辑界面，将编辑生成的文件（编辑添加的文字图片会保存为文件存在project相应目录）及project config配置删除，如果后续还有合成该视频的需求则不应该删除
        //        String path = mUri.getPath();
        //        File f = new File(path);
        //        if(!f.exists()){
        //            return ;
        //        }
        //        FileUtils.deleteDirectory(f.getParentFile());
        //删除录制生成的临时文件
        //deleteTempFiles();由于返回依然可以接着录，因此现在不能删除
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        mViewStack.onActivityResult(requestCode, resultCode, data);
    }

    private PasterUIGifImpl addPaster(AliyunPasterController controller) {
        Log.d(TAG, "add GIF");
        AliyunPasterWithImageView pasterView = (AliyunPasterWithImageView) View.inflate(getContext(),
                R.layout.aliyun_svideo_qupai_paster_gif, null);

        mPasterContainer.addView(pasterView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return new PasterUIGifImpl(pasterView, controller, null);
    }

    /**
     * 添加字幕
     *
     * @param controller
     * @return
     */
    private PasterUICaptionImpl addCaption(AliyunPasterController controller) {
        AliyunPasterWithImageView captionView = (AliyunPasterWithImageView) View.inflate(getContext(),
                R.layout.aliyun_svideo_qupai_paster_caption, null);
        mPasterContainer.addView(captionView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Log.d(TAG, "add 字幕");
        return new PasterUICaptionImpl(captionView, controller, null, mAliyunIEditor);
    }

    /**
     * 添加文字
     *
     * @param controller
     * @param restore
     * @return
     */
    private PasterUITextImpl addSubtitle(AliyunPasterController controller, boolean restore) {
        Log.d(TAG, "add 文字");
        AliyunPasterWithTextView captionView = (AliyunPasterWithTextView) View.inflate(getContext(),
                R.layout.aliyun_svideo_add_text_paster, null);
        mPasterContainer.addView(captionView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return new PasterUITextImpl(captionView, controller, null, mAliyunIEditor, restore);
    }

    /**
     * 贴纸是否相同的超强力判断
     *
     * @param pageOne {@link UIEditorPage}
     * @param page2   {@link UIEditorPage}
     * @return boolean
     */
    private boolean isPasterTypeHold(UIEditorPage pageOne, UIEditorPage page2) {
        //当pageOne为动图时，page2也是动图返回true
        //当pageOne是字幕或者字体，page2也是字幕或者字体时返回true
        return pageOne == UIEditorPage.OVERLAY && page2 == UIEditorPage.OVERLAY
                || pageOne != UIEditorPage.OVERLAY && page2 != UIEditorPage.OVERLAY;
    }

    public boolean onBackPressed() {
        if (mViewOperate != null) {
            boolean isShow = mViewOperate.isBottomViewShow();
            // 直接隐藏
            if (isShow) {
                if (mViewOperate != null) {
                    mViewOperate.getBottomView().onBackPressed();
                }
            }
            return isShow;
        } else {
            return false;
        }
    }

    private Uri mUri;
    private boolean hasTailAnimation = false;

    public void setParam(AliyunVideoParam mVideoParam, Uri mUri, boolean hasTailAnimation) {
        this.hasTailAnimation = hasTailAnimation;
        this.mUri = mUri;
        this.mVideoParam = mVideoParam;
        initEditor();
    }

    private AliyunVideoParam mVideoParam;

    public void setTempFilePaths(ArrayList<String> mTempFilePaths) {
        this.mTempFilePaths = mTempFilePaths;
    }

    private ArrayList<String> mTempFilePaths = null;

    /**
     * 播放时间、显示时间同步接口
     */
    public interface PlayerListener {

        //获取当前的播放时间（-->缩略图条位置同步）
        long getCurrDuration();

        //获取视频总时间
        long getDuration();

        //更新时间（-->显示时间同步）
        void updateDuration(long duration);
    }
}

